package com.monitor.parser.onec.srvinfo;

/*
 * Copyright 2025 Aleksei Andreev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.monitor.parser.onec.reglog.*;
import com.monitor.agent.server.BufferedRandomAccessFileStream;
import com.monitor.agent.server.FileState;
import com.monitor.agent.server.PredefinedFields;
import com.monitor.agent.server.filter.Filter;
import com.monitor.parser.LogParser;
import com.monitor.parser.LogRecord;
import com.monitor.parser.ParseException;
import com.monitor.parser.ParserParameters;
import com.monitor.parser.reader.ParserNullStorage;
import com.monitor.parser.reader.ParserRecordsStorage;
import com.monitor.util.FileUtil;
import com.monitor.util.StringUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;


public class OneCSrvInfoParser implements LogParser {
    
    public static boolean DEBUG_SYMBOLS = false;
    public static boolean DEBUG_RECORDS = false;
    
    private static final int STREAM_BUFFER_SIZE = 1024 * 1024 * 2; // 2Mb 

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final long GC_PER_RECORDS = 10000L;
    
    private static final byte EOF = -1;
    private static final byte TAB = 9;
    private static final byte NEW_LINE = 10;
    private static final byte CARRIAGE_RETURN = 13;
    private static final byte QUOTATION_MARK = 34;
    private static final byte COMMA = 44;
    private final static byte LEFT_CURLY_BRACKET = 123;
    private final static byte RIGHT_CURLY_BRACKET = 125;
    
    public static final String VOLUME_PROP_NAME = "volume";                   // том данных (имя лог-файла)
    public static final String REFERENCE_PROP_NAME = "ref";                   // ссылка на запись, позиция в файле
    public static final String DATE_PROP_NAME = "date";                       // дата события
    public static final String TRANSACTION_STATE_PROP_NAME = "tstate";        // состояние транзакции
    public static final String TRANSACTION_DATA_PROP_NAME = "tdata";          // данные транзакции - момент и смещение в логе
    public static final String USER_PROP_NAME = "user";                       // пользователь
    public static final String COMPUTER_PROP_NAME = "computer";               // компьютер
    public static final String APPLICATION_PROP_NAME = "application";         // приложение
    public static final String CONNECTION_PROP_NAME = "connection";           // соединение
    public static final String EVENT_PROP_NAME = "event";                     // событие
    public static final String LOG_LEVEL_PROP_NAME = "level";                 // важность
    public static final String COMMENT_PROP_NAME = "comment";                 // комментарий
    public static final String METADATA_PROP_NAME = "metadata";               // метаданные
    public static final String DATA_VALUE_PROP_NAME = "data";                 // данные
    public static final String DATA_PRESENTATION_PROP_NAME = "presentation";  // представление данных
    public static final String SERVER_PROP_NAME = "server";                   // сервер
    public static final String MAIN_PORT_PROP_NAME = "mainport";              // основной порт
    public static final String ADDITIONAL_PORT_PROP_NAME = "addport";         // вспомогательный порт
    public static final String SESSION_PROP_NAME = "session";                 // сеанс
    public static final String ADDITIONAL_DATA_PROP_NAME = "raw";             // дополнительные данные (массив)
    public static final String DATA_DIVIDER_PROP_NAME = "divider";            // разделитель данных (?)

    private static final byte MODE_UNKNOWN = 0;                               // первый вызов парсера
    private static final byte MODE_RECORD_TERMINATE = 1;                      // чтение окончания записи
    private static final byte MODE_RECORD_BEGIN_EXPECTED = 2;                 // ожидается начало записи
    private static final byte MODE_VALUE_EXPECTED = 3;                        // ожидается начало value
    private static final byte MODE_PLAIN_VALUE = 4;                           // чтение value
    private static final byte MODE_VALUE_INSIDE_QUOTATION_MARK = 5;           // чтение value в кавычках
    private static final byte MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED = 6;        // ожидается запятая или кавычка


    private BufferedRandomAccessFileStream stream;
    
    private ParserRecordsStorage recordsStorage;
    private Throwable exception;
    private PredefinedFields addFields;
    private Filter filter;
    private int maxCount;
    private long validBytesRead = 0L;
    private long unfilteredCount = 0L;
    private long filteredCount;
    private final long gcPerRecords = GC_PER_RECORDS;
    private int delay;
    private int maxTokenLength;
    private int getTokenLength;

    private byte mode = MODE_RECORD_BEGIN_EXPECTED;
    private long firstBytePos = 0;
    private long startPos = 0;
    private long filePos = 0;
    private long fileLinesRead = 0;
    
    private byte icc = 0;  // текущий однобайтный UTF-символ
    
                                             // Key-Value Record - номера позиций файла с началом и окончанием 
                                             // ключа и значения для одной записи лога; предполагаем, что количество
                                             // пар ключ-значение в одной записи не более 64
    
    private KeyValuesRecord kvrc = new KeyValuesRecord();


    private static String makeParserErrorsLogDir(ParserParameters parameters) {
        String dirName = parameters.getParserErrorLog();
        String workdir = new File("").getAbsolutePath() + "/" + dirName;
        new File(workdir).mkdirs();
        return workdir;
    }


    protected static class KeyValueBounds {
        public long kb = 0;
        public long ke = 0;
        public long vb = 0;
        public long ve = 0;
        public String vv = null;           // значение строкой
        public KeyValuesRecord kvr = null; // если value не простое значение, а вложенный объект
        boolean isSimple() { return kvr == null; }
        boolean isComplex() { return kvr != null; }
        String getValue() { return vv; }
    }
    
    
    protected static class KeyValuesRecord {
        private static final int MAX_RECORDS = 64;
        public final KeyValueBounds[] kv = new KeyValueBounds[MAX_RECORDS];
        public final OneCSrvInfoRecord lr = new OneCSrvInfoRecord();
        public long endsAt = 0;                           // позиция конца записи в файле
        public long startsAt = 0;                         // позиция начала записи в файле
        public boolean isEmpty = true;                    // содержит ли записи
        public byte count = -1;                           // количество-1 прочитанных пар ключ-значение (длина kv - 1)
        public KeyValuesRecord() {
            for (int i = 0; i < kv.length; i++) kv[i] = new KeyValueBounds();
        }
        public String getSimple(int idx) { 
            if (idx > count) return "";
            KeyValueBounds kvb = kv[idx];
            if (kvb.isSimple()) return kvb.vv;
            return ""; // если запрашивали simple, а установлено complex-значение
        }
        public KeyValuesRecord getComplex(int idx) { 
            if (idx > count) return new KeyValuesRecord();
            KeyValueBounds kvb = kv[idx];
            if (kvb.isComplex()) return kvb.kvr;
            return new KeyValuesRecord(); // если запрашивали complex, а установлено simple-значение
        }
        public void clear() {
            lr.clear();
            endsAt = 0;
            startsAt = 0;
            isEmpty = true;
        }
        @Override
        public String toString() {
            String s = "{";
            for (byte i = 0; i <= count; i++) {
                if (i > 0) s = s + ", ";
                if (kv[i].kvr == null)
                    s = s + kv[i].vv;
                else
                    s = s + kv[i].kvr.toString();
            }
            return s + "}";
        }
    }
    
    
    public OneCSrvInfoParser() {
        recordsStorage = new ParserNullStorage(); // new ParserListStorage();
        validBytesRead = 0;
        unfilteredCount = 0;
        filteredCount = 0;
        fileLinesRead = 0;
        exception = null;
        delay = 0;
        maxTokenLength = Integer.MAX_VALUE;
        stream = null;
    }
    

    @Override
    public void setRecordsStorage(ParserRecordsStorage storage) {
        this.recordsStorage = storage;
    }

    
    @Override
    // Возвращает не фактическое количество прочитанных байтов, а количество байтов,
    // прочитанных во всех записях лога, не закончившихся ошибкой чтения данных, 
    // чтобы можно было начать следующее чтение с позиции начала записи лога,
    // в которой последний раз встретилась ошибка
    //
    public long getFilePos() {
        return validBytesRead - startPos;
    }

    
    @Override
    public Throwable getException() {
        return exception;
    }
    
    
    private boolean onLogRecord(KeyValuesRecord keyValueRecord) {
        boolean result;
        if (filteredCount >= maxCount) {
            return false;
        }
        if (keyValueRecord.isEmpty) {
            return true;
        }
        validBytesRead = keyValueRecord.endsAt - 1;
        OneCSrvInfoRecord logRecord = keyValueRecord.lr;
        result = filterAndStoreRecord(logRecord);

        unfilteredCount++;
        if (unfilteredCount % gcPerRecords == 0) {
            System.gc();
        }

        return result;
    }

    
    public boolean filterAndStoreRecord(LogRecord logRecord) {
        try {
            if (filter == null || filter.accept(logRecord)) {
                filteredCount++;
                if (addFields != null) {
                    logRecord.putAll(addFields);
                }
                if (beforeStoreRecord(logRecord)) {
                    recordsStorage.put(logRecord);
                }
            }
            if (delay > 0) {
                Thread.sleep(delay);
            }
        }
        catch (Exception ex) {
            OneCRLRecord message = new OneCRLRecord();
            message.put("LOGSERIALIZEERROR", ex.getMessage());
            try {
                recordsStorage.put(message);
            }
            catch (Exception ex1) {
                if (exception == null) exception = ex1;
            }
            return false;
        }
        return filteredCount < maxCount;
    }

    
    public boolean beforeStoreRecord(LogRecord logRecord) throws java.text.ParseException {
        return true;
    }
    
    
    protected OneCSrvInfoRecord buildClusterValue(KeyValuesRecord kvrecord) throws IOException {
        OneCSrvInfoRecord clusterValue = new OneCSrvInfoRecord();
        clusterValue.put("uuid", kvrecord.getSimple(0));
        clusterValue.put("name", kvrecord.getSimple(1));
        clusterValue.put("port", kvrecord.getSimple(2));
        clusterValue.put("server-name", kvrecord.getSimple(3));
        String cluster1 = kvrecord.getSimple(4);
        String secureConnection = kvrecord.getSimple(5);
        String secureConnectionStr;
        switch ((String) secureConnection) {
            case "0" : secureConnectionStr = "off"; break;               // выключено
            case "1" : secureConnectionStr = "only-connection"; break;   // только соединение
            case "2" : secureConnectionStr = "always-on"; break;         // постоянно
            default : secureConnectionStr = "";
        }
        clusterValue.put("secure-connection", secureConnectionStr);
        clusterValue.put("working-process-restart-interval", kvrecord.getSimple(6));
        clusterValue.put("problem-working-process-stop-delay", kvrecord.getSimple(7));
        String cluster5 = kvrecord.getSimple(8);
        String cluster6 = kvrecord.getSimple(9);
        String cluster7 = kvrecord.getSimple(10);
        KeyValuesRecord clusterComputerAndPort = kvrecord.getComplex(11).getComplex(1);
        OneCSrvInfoRecord serverAndPort = new OneCSrvInfoRecord();
        serverAndPort.put("name", clusterComputerAndPort.getSimple(0));
        serverAndPort.put("port", clusterComputerAndPort.getSimple(1));
        clusterValue.put("server", serverAndPort);
        String clusterPayloadDistribution = kvrecord.getSimple(12);
        String clusterPayloadDistributionStr;
        switch ((String) clusterPayloadDistribution) {
            case "0" : clusterPayloadDistributionStr = "by-memory"; break;       // приоритет по памяти
            case "1" : clusterPayloadDistributionStr = "by-perfomance"; break;   // приоритет по производительности
            default : clusterPayloadDistributionStr = "";
        }
        clusterValue.put("payload-distribution", clusterPayloadDistributionStr);
        String cluster10 = kvrecord.getSimple(13);
        clusterValue.put("problem-working-process-forced-stop", "1".equals(kvrecord.getSimple(14)) ? "true" : "false");
        clusterValue.put("working-process-dump-on-memory-overload", "1".equals(kvrecord.getSimple(15)) ? "true" : "false");
        
        return clusterValue;
    }

    protected void buildRecord(KeyValuesRecord kvrecord) throws IOException {
        if (DEBUG_RECORDS) { System.out.println("kvrecord = " + kvrecord); }

        OneCSrvInfoRecord logrec = kvrecord.lr;
        logrec.clear();
        byte kvreclength = kvrecord.count;
        
        if (kvreclength < 3) {
            return;
        }
        
        ArrayList<OneCSrvInfoRecord> clustersValues = new ArrayList();
        KeyValuesRecord clusters = kvrecord.getComplex(0);
        for (int ci = 1; ci <= clusters.count; ci++) { // первое значение пропускаем - там количество кластеров
            clustersValues.add(buildClusterValue(clusters.getComplex(ci)));
        }
        
        KeyValuesRecord clusters1 = kvrecord.getComplex(1);
        String clusters2 = kvrecord.getSimple(2);
        String clusters3 = kvrecord.getSimple(3);
        
        logrec.put("clusters", clustersValues);
        logrec.put("field1", new ArrayList());
        logrec.put("field2", clusters2);
        logrec.put("field3", clusters3);
        
        if (DEBUG_RECORDS) { System.out.println(logrec); }                
    }
    
    
    private KeyValuesRecord fillValues(KeyValuesRecord kvrecord) throws IOException {
        byte kvreclength = kvrecord.count;

        kvrecord.endsAt = filePos;
        kvrecord.isEmpty = (kvreclength == -1);
        
        Object vo;
        for (byte kv = 0; kv <= kvreclength; kv++) {
            
            KeyValueBounds kvi = kvrecord.kv[kv];

            if (kvi.kvr == null) {
                // это простое значение
                if (kvi.vb > 0) {
                    int vl = (int)(kvi.ve - kvi.vb);
                    int l = vl > getTokenLength ? maxTokenLength : vl;
                    stream.seek(kvi.vb);
                    String vs = stream.readStripNewLine(l, UTF8);
                    if (l != vl) {
                        vs = vs + " (... ещё " + (vl - l) + " симв.)";
                    }
                    vo = StringUtil.getRidOfUnprintables(vs);
                }
                else {
                    vo = "";
                }
                kvi.vv = (String) vo;
            }
            else {
                // вложенное значение
                fillValues(kvi.kvr);
            }
        }
        
        return kvrecord;
    }
    
    
    @Override
    public void parse(FileState state, String encoding, long fromPosition, int maxRecords, Filter fltr, 
            ParserParameters parameters) throws IOException, ParseException {
        
        if (maxRecords <= 0) {
            return;
        }
        
        maxCount = maxRecords;
        addFields = state.getFields();
        filter = Filter.and(state.getFilter(), fltr == null ? null : fltr.copy());
        exception = null;

        startPos = fromPosition;
        fileLinesRead = 0;
        unfilteredCount = 0;
        filteredCount = 0;
        delay = parameters == null ? 0 : parameters.getDelay();
        maxTokenLength = parameters == null ? Integer.MAX_VALUE : parameters.getMaxTokenLength();
        getTokenLength = maxTokenLength == Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (maxTokenLength * 1.05);

        mode = MODE_UNKNOWN;
        
        try (BufferedRandomAccessFileStream rafs = new BufferedRandomAccessFileStream(
                state.getOpenedRandomAccessFile(),
                STREAM_BUFFER_SIZE)) {
            stream = rafs;
            stream.seek(fromPosition);
            fileLinesRead = 0;
            KeyValuesRecord record = read();
            record = fillValues(record);
            buildRecord(record);
            onLogRecord(record);
        }
        catch (ParseException ex) {
            if (parameters != null && parameters.logParseExceptions()) {
                String parserErrorLog = makeParserErrorsLogDir(parameters);
                File errorFragmentFile = new File(String.format("%s/%s.%s.%s.parse_error", 
                        parserErrorLog,
                        state.getFile().getName(),
                        kvrc.startsAt - 1,
                        filePos));
                FileUtil.copyFileFragment(state.getFile(), 
                        kvrc.startsAt - 1,
                        filePos + 256, // в лог-файл с фрагментом ошибки запишем ещё несколько символов после ошибки
                        errorFragmentFile);
            }
            exception = ex;
            throw new ParseException(String.format("%s at line %d; file %s", 
                    ex.getMessage(),
                    fileLinesRead,
                    state.getFile().getPath()));
        }
        catch (Exception ex) {
            exception = ex;
            throw ex;
        }
        finally {
            kvrc.clear();
        }
        stream = null;
    }


    private KeyValuesRecord read() 
            throws IOException, ParseException {
        
        KeyValuesRecord kvr = new KeyValuesRecord();
        kvrc = kvr;

        long fromPosition = stream.getFilePointer();
        kvr.endsAt = fromPosition;
        kvr.startsAt = fromPosition;
        filePos = fromPosition;
        validBytesRead = fromPosition;

        if (mode == MODE_UNKNOWN) {
            mode = MODE_RECORD_BEGIN_EXPECTED;
        }
        
        byte[] b2 = new byte[2];                   // буфер UTF-символа для отладки
        byte[] b3 = new byte[3];                   // буфер UTF-символа для отладки
        byte[] b4 = new byte[4];                   // буфер UTF-символа для отладки
    
        byte ic2 = 0;                              // втророй UTF-байт
        byte ic3 = 0;                              // третий UTF-байт
        byte ic4 = 0;                              // четвёртый UTF-байт

        byte bytesInSym = 1;                       // количество байтов в прочитанном UTF-8 символе

        KeyValueBounds kvc = new KeyValueBounds(); // текущая пара ключ-значение
        
        boolean recfinished = false;               // признак - прочитана ли запись полностью
    
        do {
            firstBytePos = filePos;
            icc = stream.bread(); filePos++;
            if ((icc & 0b10000000) == 0) {                      // 0xxxxxxx
                bytesInSym = 1;
            }
            else if ((icc & 0b11100000) == 0b11000000) {        // 110xxxxx
                ic2 = stream.bread(); filePos++;
                if ((ic2 & 0b11000000) == 0b10000000) {         // 10xxxxxx
                    bytesInSym = 2;
                }
                else {
                    throw new ParseException("wrong utf-8 symbol");
                }
            }
            else if ((icc & 0b11110000) == 0b11100000) {        // 1110xxxx
                ic2 = stream.bread(); filePos++;
                if ((ic2 & 0b11000000) == 0b10000000) {         // 10xxxxxx
                    ic3 = stream.bread(); filePos++;
                    if ((ic3 & 0b11000000) == 0b10000000) {     // 10xxxxxx
                        bytesInSym = 3;
                    }
                    else {
                        throw new ParseException("wrong utf-8 symbol");
                    }
                }
                else {
                    throw new ParseException("wrong utf-8 symbol");
                }
            }
            else if ((icc & 0b11111000) == 0b11110000) {        // 11110xxx
                ic2 = stream.bread(); filePos++;
                if ((ic2 & 0b11000000) == 0b10000000) {         // 10xxxxxx
                    ic3 = stream.bread(); filePos++;
                    if ((ic3 & 0b11000000) == 0b10000000) {     // 10xxxxxx
                        ic4 = stream.bread(); filePos++;
                        if ((ic4 & 0b11000000) == 0b10000000) { // 10xxxxxx
                            bytesInSym = 4;
                        }
                        else {
                            throw new ParseException("wrong utf-8 symbol");
                        }
                    }
                    else {
                        throw new ParseException("wrong utf-8 symbol");
                    }
                }
                else {
                    throw new ParseException("wrong utf-8 symbol");
                }
            }
            else if (icc != EOF) {
                throw new ParseException("wrong utf-8 symbol");
            }
            
            if (DEBUG_SYMBOLS) {
                switch (bytesInSym) {
                    case 1:
                        System.out.println("" + (char) icc + " (" + icc + ") [" + mode + "]");
                        break;
                    case 2:
                        b2[0] = icc;
                        b2[1] = ic2;
                        System.out.println("" + new String(b2, UTF8) + " (" + icc + ", " + ic2 + ") [" + mode + "]");
                        break;
                    case 3:
                        b3[0] = icc;
                        b3[1] = ic2;
                        b3[2] = ic3;
                        System.out.println("" + new String(b3, UTF8) + " (" + icc + ", " + ic2 + ", " + ic3 + ") [" + mode + "] ");
                        break;
                    case 4:
                        b4[0] = icc;
                        b4[1] = ic2;
                        b4[2] = ic3;
                        b4[3] = ic4;
                        System.out.println("" + new String(b4, UTF8) + " (" + icc + ", " + ic2 + ", " + ic3 + ", " + ic4 + ") [" + mode + "] ");
                }
            }

            switch (icc) {
                case LEFT_CURLY_BRACKET:
                    switch (mode) {
                        case MODE_RECORD_BEGIN_EXPECTED:
                            mode = MODE_VALUE_EXPECTED;
                            break;
                        case MODE_VALUE_EXPECTED:
                            kvc = kvr.kv[++kvr.count];            // обнаружено новое [вложенное] значение
                            kvc.kvr = read();                     // чтение вложенного значения
                            mode = MODE_PLAIN_VALUE;
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            break;
                        default:
                            throw new ParseException("wrong left curly bracket appearing");
                    }
                    break;
                case RIGHT_CURLY_BRACKET:
                    switch (mode) {
                        case MODE_PLAIN_VALUE:
                            kvc.ve = firstBytePos;                // зафиксировать конец значения
                            recfinished = true;
                            mode = MODE_VALUE_EXPECTED;
                            break;
                        case MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED:
                            kvc.ve = firstBytePos - 1;            // зафиксировать конец значения (-1)
                            mode = MODE_VALUE_EXPECTED;
                            break;
                        case MODE_VALUE_EXPECTED:
                            kvc.ve = firstBytePos;                // зафиксировать конец значения
                            recfinished = true;
                            mode = MODE_VALUE_EXPECTED;
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            break;
                        default:
                            throw new ParseException("wrong right curly bracket appearing");
                    }
                    break;
                case COMMA:
                    switch (mode) {
                        case MODE_VALUE_EXPECTED:
                            kvc = kvr.kv[++kvr.count];            // обнаружено новое значение
                            kvc.vb = firstBytePos;                // зафиксировать конец значения (оно пустое)
                            kvc.ve = firstBytePos;
                            mode = MODE_VALUE_EXPECTED;
                            break;
                        case MODE_PLAIN_VALUE:
                            kvc.ve = firstBytePos;                // зафиксировать конец значения
                            mode = MODE_VALUE_EXPECTED;
                            break;
                        case MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED:
                            kvc.ve = firstBytePos - 1;            // зафиксировать конец значения
                            mode = MODE_VALUE_EXPECTED;
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            break;
                        case MODE_RECORD_TERMINATE:
                            // запись прочитана полностью
                            recfinished = true;
                            icc = EOF;
                            mode = MODE_RECORD_BEGIN_EXPECTED;
                            break;
                        case MODE_RECORD_BEGIN_EXPECTED:
                            break;
                        default:
                            throw new ParseException("wrong comma appearing");
                    }
                    break;
                case QUOTATION_MARK:
                    switch (mode) {
                        case MODE_VALUE_EXPECTED:
                            kvc = kvr.kv[++kvr.count];            // обнаружено новое значение в кавычках
                            kvc.vb = firstBytePos + 1;            // зафиксировать начало значения (+1)
                            mode = MODE_VALUE_INSIDE_QUOTATION_MARK;
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            mode = MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED;
                            break;
                        case MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED:
                            // это двойная кавычка внутри значения в кавычках - продолжаем чтение значения
                            mode = MODE_VALUE_INSIDE_QUOTATION_MARK;
                            break;
                        default:
                            throw new ParseException("wrong quotation mark appear");
                    }
                    break;
                case EOF:
                    recfinished = true;
                    break;
                case NEW_LINE:
                    fileLinesRead++;
                    // здесь break не нужен
                case CARRIAGE_RETURN:
                case TAB:
                    if (mode == MODE_VALUE_EXPECTED) {
                        break;
                    }
                    // здесь break не нужен
                default: // любой другой однобайтный символ (в т.ч. EOF) или символ из двух и более байтов
                    switch (mode) {
                        case MODE_RECORD_TERMINATE:
                            // запись прочитана полностью; сюда попадём
                            // только если встретился перевод строки, но не EOF
                            recfinished = true;
                            icc = EOF;
                            mode = MODE_RECORD_BEGIN_EXPECTED;
                            break;
                        case MODE_RECORD_BEGIN_EXPECTED:
                            if (bytesInSym != 1) break; // BOM
                            if (icc == EOF) {
                                recfinished = true;
                            }
                            break;
                        case MODE_VALUE_EXPECTED:
                            kvc = kvr.kv[++kvr.count];            // обнаружено новое значение
                            kvc.vb = firstBytePos;                // зафиксировать начало значения
                            mode = MODE_PLAIN_VALUE;
                            break;
                        case MODE_PLAIN_VALUE:
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            break;
                        default:
                            throw new ParseException("wrong symbol appearing");
                    }
            }
        }
        while (!recfinished);

        return kvr;
    }
    
    
}
