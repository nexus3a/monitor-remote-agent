/*
 * Copyright 2025 Алексей Андреев
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
package com.monitor.parser.onec.reglog;

import com.monitor.agent.server.BufferedRandomAccessFileStream;
import com.monitor.agent.server.FileState;
import com.monitor.agent.server.PredefinedFields;
import com.monitor.agent.server.filter.Filter;
import com.monitor.parser.LogParser;
import com.monitor.parser.ParseException;
import com.monitor.parser.ParserParameters;
import com.monitor.parser.reader.ParserNullStorage;
import com.monitor.parser.reader.ParserRecordsStorage;
import com.monitor.util.FileUtil;
import com.monitor.util.StringUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;


/**
 * Чтение словаря журнала регистрации 1С
 */
public class OneCRLDescriptorsParser implements LogParser {
    
    public static boolean DEBUG_SYMBOLS = false;
    public static boolean DEBUG_RECORDS = false;
    
    private static final int STREAM_BUFFER_SIZE = 1024 * 1024 * 2; // 2Mb 

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final long GC_PER_RECORDS = 10000L;
    
    private static final byte EOF = -1;
    private static final byte NEW_LINE = 10;
    private static final byte CARRIAGE_RETURN = 13;
    private static final byte QUOTATION_MARK = 34;
    private static final byte COMMA = 44;
    private final static byte LEFT_CURLY_BRACKET = 123;
    private final static byte RIGHT_CURLY_BRACKET = 125;
    
    public static final String CATALOG_PROP_NAME = "catalog";               // код каталога
    public static final String DATA_VALUE_PROP_NAME = "value";              // значение записи каталога
    public static final String DATA_DESCRIPTION_PROP_NAME = "description";  // описание записи каталога
    public static final String DATA_INDEX_PROP_NAME = "index";              // индекс записи каталога

    private static final byte MODE_RECORD_TERMINATE = 0;                    // чтение окончания записи
    private static final byte MODE_RECORD_BEGIN_EXPECTED = 1;               // ожидается начало записи
    private static final byte MODE_VALUE_EXPECTED = 2;                      // ожидается начало value
    private static final byte MODE_PLAIN_VALUE = 3;                         // чтение value
    private static final byte MODE_VALUE_INSIDE_QUOTATION_MARK = 4;         // чтение value в кавычках
    private static final byte MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED = 5;      // ожидается запятая или кавычка


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
    
    private final KeyValuesRecord kvrc = new KeyValuesRecord();

    private byte kvcc;                       // количество-1 прочитанных пар ключ-значение в текущей записи лога kvrc
    

    private static String makeParserErrorsLogDir(ParserParameters parameters) {
        String dirName = parameters.getParserErrorLog();
        String workdir = new File("").getAbsolutePath() + "/" + dirName;
        new File(workdir).mkdirs();
        return workdir;
    }


    private static class KeyValueBounds {
        public long kb = 0;
        public long ke = 0;
        public long vb = 0;
        public long ve = 0;
        public String kv = null; // значение ключа строкой
    }
    
    
    private static class KeyValuesRecord {
        private static final int MAX_RECORDS = 64;
        public final KeyValueBounds[] kv = new KeyValueBounds[MAX_RECORDS];
        public final OneCRLRecord lr = new OneCRLRecord();
        public long endsAt = 0;                           // позиция конца записи в файле
        public long startsAt = 0;                         // позиция начала записи в файле
        public boolean isEmpty = true;                    // содержит ли записи
        public KeyValuesRecord() {
            for (int i = 0; i < kv.length; i++) kv[i] = new KeyValueBounds();
        }
        public void clear() {
            lr.clear();
            endsAt = 0;
            startsAt = 0;
            isEmpty = true;
        }
    }
    
    
    public OneCRLDescriptorsParser() {
        recordsStorage = new ParserNullStorage(); // new ParserListStorage();
        validBytesRead = 0;
        unfilteredCount = 0;
        filteredCount = 0;
        fileLinesRead = 0;
        exception = null;
        delay = 0;
        maxTokenLength = Integer.MAX_VALUE;

        kvcc = -1;
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
        OneCRLRecord logRecord = keyValueRecord.lr;
        result = filterAndStoreRecord(logRecord);

        unfilteredCount++;
        if (unfilteredCount % gcPerRecords == 0) {
            System.gc();
        }

        return result;
    }

    
    public boolean filterAndStoreRecord(OneCRLRecord logRecord) {
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

    
    public boolean beforeStoreRecord(OneCRLRecord logRecord) throws java.text.ParseException {
        return true;
    }
    
    
    private boolean buildRecord() throws IOException {
        OneCRLRecord logrec = kvrc.lr;
        logrec.clear();

        kvrc.endsAt = filePos;
        kvrc.isEmpty = (kvcc == -1);
        
        String vo;
        long fp = stream.getFilePointer();
        for (byte kv = 0; kv <= kvcc; kv++) {
            KeyValueBounds kvi = kvrc.kv[kv];
            
            String k;
            switch (kv) {
                case 0: k = CATALOG_PROP_NAME; break;
                case 1: k = DATA_VALUE_PROP_NAME; break;
                case 2: k = kvcc == 3 ? DATA_DESCRIPTION_PROP_NAME : DATA_INDEX_PROP_NAME; break;
                case 3: k = DATA_INDEX_PROP_NAME; break;
                default: k = "";
            }

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
            
            logrec.put(k, vo);
            if (DEBUG_RECORDS) { System.out.println(k + "=" + vo); }                

        }
        stream.seek(fp);
        
        // разбор журнала будет продолжен только если количество отфильтрованных записей
        // не больше заданного количества
        //
        boolean continueParsing = onLogRecord(kvrc);
        if (continueParsing) {
            kvrc.startsAt = kvrc.endsAt;
        }
        
        kvcc = -1;
        
        return continueParsing;
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

        try (BufferedRandomAccessFileStream rafs = new BufferedRandomAccessFileStream(
                state.getOpenedRandomAccessFile(),
                STREAM_BUFFER_SIZE)) {
            stream = rafs;
            if (fromPosition == 0) {
                stream.seek(0);
                stream.skipLine();
                stream.skipLine();
                stream.skipLine();
                fileLinesRead = 3;
            }
            else {
                stream.seek(fromPosition);
                fileLinesRead = 0;
            }
            read(stream, parameters);
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


    private void read(BufferedRandomAccessFileStream stream, ParserParameters parameters) 
            throws IOException, ParseException {
        
        kvrc.clear();

        long fromPosition = stream.getFilePointer();
        kvrc.endsAt = fromPosition;
        kvrc.startsAt = fromPosition;
        filePos = fromPosition;
        validBytesRead = fromPosition;

        unfilteredCount = 0;
        filteredCount = 0;
        delay = parameters == null ? 0 : parameters.getDelay();
        maxTokenLength = parameters == null ? Integer.MAX_VALUE : parameters.getMaxTokenLength();
        getTokenLength = maxTokenLength == Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (maxTokenLength * 1.05);
        
        int nestedCurvedValueLevel = 0;            // уровень вложенности значения в фигурных скобках

        kvcc = -1;
        mode = MODE_RECORD_BEGIN_EXPECTED;
        
        byte[] b2 = new byte[2];                   // буфер UTF-символа для отладки
        byte[] b3 = new byte[3];                   // буфер UTF-символа для отладки
        byte[] b4 = new byte[4];                   // буфер UTF-символа для отладки
    
        byte ic2 = 0;                              // втророй UTF-байт
        byte ic3 = 0;                              // третий UTF-байт
        byte ic4 = 0;                              // четвёртый UTF-байт

        byte bytesInSym = 1;                       // количество байтов в прочитанном UTF-8 символе

        KeyValueBounds kvc = new KeyValueBounds(); // текущая пара ключ-значение
    
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
                case CARRIAGE_RETURN:
                    break;
                case LEFT_CURLY_BRACKET:
                    switch (mode) {
                        case MODE_RECORD_BEGIN_EXPECTED:
                            mode = MODE_VALUE_EXPECTED;
                            break;
                        case MODE_VALUE_EXPECTED:
                            mode = MODE_VALUE_EXPECTED;
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать новое значение
                                kvcc++;
                                kvc = kvrc.kv[kvcc];
                                // зафиксировать начало значения
                                kvc.vb = firstBytePos;
                            }
                            nestedCurvedValueLevel++;
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
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать конец значения
                                kvc.ve = firstBytePos;
                                mode = MODE_RECORD_TERMINATE;
                            }
                            else {
                                nestedCurvedValueLevel--;
                                mode = MODE_PLAIN_VALUE;
                            }
                            break;
                        case MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED:
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать конец значения (-1)
                                kvc.ve = firstBytePos - 1;
                                mode = MODE_RECORD_TERMINATE;
                            }
                            else {
                                nestedCurvedValueLevel--;
                                mode = MODE_PLAIN_VALUE;
                            }
                            break;
                        case MODE_VALUE_EXPECTED:
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать конец значения - оно пустое
                                kvc.ve = firstBytePos;
                                mode = MODE_RECORD_TERMINATE;
                            }
                            else {
                                nestedCurvedValueLevel--;
                                mode = MODE_PLAIN_VALUE;
                            }
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
                            mode = MODE_VALUE_EXPECTED;
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать новое значение
                                kvcc++;
                                kvc = kvrc.kv[kvcc];
                                // зафиксировать конец значения (оно пустое)
                                kvc.vb = firstBytePos;
                                kvc.ve = firstBytePos; // = kvc.vb ?
                            }
                            break;
                        case MODE_PLAIN_VALUE:
                            mode = MODE_VALUE_EXPECTED;
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать конец значения
                                kvc.ve = firstBytePos;
                            }
                            break;
                        case MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED:
                            mode = MODE_VALUE_EXPECTED;
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать конец значения (-1)
                                kvc.ve = firstBytePos - 1;
                            }
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            break;
                        case MODE_RECORD_TERMINATE:
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать запись в коллекции
                                if (!buildRecord()) {
                                    icc = EOF;
                                    break;
                                }
                            }
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
                            mode = MODE_VALUE_INSIDE_QUOTATION_MARK;
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать новое значение
                                kvcc++;
                                kvc = kvrc.kv[kvcc];
                                // зафиксировать начало значения (+1)
                                kvc.vb = firstBytePos + 1;
                            }
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
                case NEW_LINE:
                    fileLinesRead++;
                    if (mode == MODE_VALUE_EXPECTED) {
                        break;
                    }
                    // здесь break не нужен
                default: // любой другой однобайтный символ (в т.ч. EOF) или символ из двух и более байтов
                    switch (mode) {
                        
                        case MODE_RECORD_TERMINATE:
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать запись в коллекции; сюда попадём
                                // только если встретился перевод строки, но не EOF
                                if (!buildRecord()) {
                                    icc = EOF;
                                    break;
                                }
                            }
                            mode = MODE_RECORD_BEGIN_EXPECTED;
                            // здесь break не нужен
                        case MODE_RECORD_BEGIN_EXPECTED:
                            if (bytesInSym != 1) break; // BOM
                            if (icc == EOF) break;
                            break;
                        
                        case MODE_VALUE_EXPECTED:
                            mode = MODE_PLAIN_VALUE;
                            if (nestedCurvedValueLevel == 0) {
                                // зафиксировать новое значение
                                kvcc++;
                                kvc = kvrc.kv[kvcc];
                                // зафиксировать начало значения
                                kvc.vb = firstBytePos;
                            }
                            break;
                        case MODE_PLAIN_VALUE:
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            break;
                        default:
                            throw new ParseException("wrong symbol appearing");
                    }
            }
        }
        while (icc != EOF);

        kvrc.clear();
    }
    
    
}
