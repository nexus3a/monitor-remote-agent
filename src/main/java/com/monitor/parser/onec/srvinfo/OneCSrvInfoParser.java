package com.monitor.parser.onec.srvinfo;


import com.monitor.parser.onec.reglog.*;
import com.monitor.agent.server.BufferedRandomAccessFileStream;
import com.monitor.agent.server.FileState;
import com.monitor.agent.server.PredefinedFields;
import com.monitor.agent.server.filter.Filter;
import com.monitor.parser.LogParser;
import com.monitor.parser.ParseException;
import com.monitor.parser.ParserParameters;
import com.monitor.parser.reader.ParserNullStorage;
import com.monitor.parser.reader.ParserRecordsStorage;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OneCSrvInfoParser implements LogParser {
    
    public static boolean DEBUG_SYMBOLS = false;
    public static boolean DEBUG_RECORDS = false;
    
    private static final int STREAM_BUFFER_SIZE = 1024 * 1024 * 2; // 2Mb 

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern UNPRINTABLE_PATTERN = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]");    
    private static final long GC_PER_RECORDS = 10000L;
    
    private static final byte EOF = -1;
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

    private static final byte MODE_RECORD_TERMINATE = 0;                      // чтение окончания записи
    private static final byte MODE_RECORD_BEGIN_EXPECTED = 1;                 // ожидается начало записи
    private static final byte MODE_VALUE_EXPECTED = 2;                        // ожидается начало value
    private static final byte MODE_PLAIN_VALUE = 3;                           // чтение value
    private static final byte MODE_VALUE_INSIDE_QUOTATION_MARK = 4;           // чтение value в кавычках
    private static final byte MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED = 5;        // ожидается запятая или кавычка


    private BufferedRandomAccessFileStream stream;
    
    private ParserRecordsStorage recordsStorage;
    private Throwable exception;
    private PredefinedFields addFields;
    private Filter filter;
    private String volume;
    private int maxCount;
    private long validBytesRead = 0L;
    private long unfilteredCount = 0L;
    private long filteredCount;
    private final long gcPerRecords = GC_PER_RECORDS;
    private int delay;
    private int maxTokenLength;
    private int getTokenLength;
    private OneCRLCatalogsStorage catalogs;

    private byte mode = MODE_RECORD_BEGIN_EXPECTED;
    private long firstBytePos = 0;
    private long startPos = 0;
    private long filePos = 0;
    private long fileLinesRead = 0;
    
    private final HashSet usedUsers;
    private final HashSet usedComputers;
    private final HashSet usedApplications;
    private final HashSet usedEvents;
    private final HashSet usedMetadata;
    private final HashSet usedServers;
    private final HashSet usedMainPorts;
    private final HashSet usedAdditionalPorts;
    
    private byte icc = 0;  // текущий однобайтный UTF-символ
    
                                             // Key-Value Record - номера позиций файла с началом и окончанием 
                                             // ключа и значения для одной записи лога; предполагаем, что количество
                                             // пар ключ-значение в одной записи не более 64
    
    private KeyValuesRecord kvrc = new KeyValuesRecord();

    private byte kvcc;                       // количество-1 прочитанных пар ключ-значение в текущей записи лога kvrc
    

    public static boolean copyFileFragment(File src, long fromPos, long toPos, File dest) throws IOException {
        if (dest.exists()) {
            return false;
        }
        try (
                BufferedRandomAccessFileStream in = new BufferedRandomAccessFileStream(new RandomAccessFile(src, "r"), 1024);
                OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            
            in.seek(fromPos);
            long bytesReadLeft = toPos - fromPos;

            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0 && bytesReadLeft > 0) {
                lengthRead = (int) Math.min(lengthRead, bytesReadLeft);
                bytesReadLeft = bytesReadLeft - lengthRead;
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        }
        catch (Exception ex) {
            return false;
        }
        return true;
    }
    
    
    private static String makeParserErrorsLogDir(ParserParameters parameters) {
        String dirName = parameters.getParserErrorLog();
        String workdir = new File("").getAbsolutePath() + "/" + dirName;
        new File(workdir).mkdirs();
        return workdir;
    }


    private String getRidOfUnprintables(String str) {
        Matcher matcher = UNPRINTABLE_PATTERN.matcher(str);
        return matcher.replaceAll("?");
    }

    
    private static class KeyValueBounds {
        public long kb = 0;
        public long ke = 0;
        public long vb = 0;
        public long ve = 0;
        public String kv = null;           // значение ключа строкой
        public KeyValuesRecord kvr = null; // если value не простое значение, а вложенный объект
    }
    
    
    private static class KeyValuesRecord {
        private static final int MAX_RECORDS = 64;
        public final KeyValueBounds[] kv = new KeyValueBounds[MAX_RECORDS];
        public final OneCRLDescriptorsRecord lr = new OneCRLDescriptorsRecord();
        public long endsAt = 0;                           // позиция конца записи в файле
        public long startsAt = 0;                         // позиция начала записи в файле
        public boolean isEmpty = true;                    // содержит ли записи
        public byte count = -1;                           // количество-1 прочитанных пар ключ-значение (длина kv - 1)
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
    
    
    public OneCSrvInfoParser() {
        recordsStorage = new ParserNullStorage(); // new ParserListStorage();
        validBytesRead = 0;
        unfilteredCount = 0;
        filteredCount = 0;
        fileLinesRead = 0;
        exception = null;
        delay = 0;
        maxTokenLength = Integer.MAX_VALUE;
        catalogs = null;

        kvcc = -1;
        stream = null;
        volume = "";
        
        usedUsers = new HashSet();
        usedComputers = new HashSet();
        usedApplications = new HashSet();
        usedEvents = new HashSet();
        usedMetadata = new HashSet();
        usedServers = new HashSet();
        usedMainPorts = new HashSet();
        usedAdditionalPorts = new HashSet();
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
    
    
    private void clearUsedIndexes() {
        usedUsers.clear();
        usedComputers.clear();
        usedApplications.clear();
        usedEvents.clear();
        usedMetadata.clear();
        usedServers.clear();
        usedMainPorts.clear();
        usedAdditionalPorts.clear();
    }
    
    
    private OneCRLCatalogsStorage getDescriptorsCatalogs(File regLogCatalog) throws IOException, ParseException {
        OneCRLDescriptorsParser parser = new OneCRLDescriptorsParser();
        
        OneCRLCatalogsStorage storage = new OneCRLCatalogsStorage();
        parser.setRecordsStorage(storage);
        ParserParameters parameters = new ParserParameters();
        parameters.setDelay(0);
        parameters.setMaxTokenLength(1024 * 32);
        parameters.setLogParseExceptions(false);
        FileState state = new FileState(new File(regLogCatalog, "1Cv8.lgf"));
        
        parser.parse(state, "UTF-8", 0, 999999999, null, parameters);
        return storage;
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
        OneCRLDescriptorsRecord logRecord = keyValueRecord.lr;
        result = filterAndStoreRecord(logRecord);

        unfilteredCount++;
        if (unfilteredCount % gcPerRecords == 0) {
            System.gc();
        }

        return result;
    }

    
    public boolean filterAndStoreRecord(OneCRLDescriptorsRecord logRecord) {
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
            OneCRLDescriptorsRecord message = new OneCRLDescriptorsRecord();
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

    
    public boolean beforeStoreRecord(OneCRLDescriptorsRecord logRecord) throws java.text.ParseException {
        return true;
    }
    
    
    private boolean buildRecord(KeyValuesRecord kvrecord) throws IOException {
        OneCRLDescriptorsRecord logrec = kvrecord.lr;
        logrec.clear();
        byte kvreclength = kvrecord.count;

        kvrecord.endsAt = filePos;
        kvrecord.isEmpty = (kvreclength == -1);
        
        logrec.put(VOLUME_PROP_NAME, volume);
        logrec.put(REFERENCE_PROP_NAME, kvrecord.startsAt);
        
        ArrayList<Object> additionalData = null;
        if (kvreclength > 18) {
            additionalData = new ArrayList<>();
            logrec.put(ADDITIONAL_DATA_PROP_NAME, additionalData);
        }

        Object vo;
        long fp = stream.getFilePointer();
        for (byte kv = 0; kv <= kvreclength; kv++) {
            if (kv == 17) continue; // количество записей в дополнительных данных - не нужно возвращать
            
            KeyValueBounds kvi = kvrecord.kv[kv];
            
            if (kvi.vb > 0) {
                int vl = (int)(kvi.ve - kvi.vb);
                int l = vl > getTokenLength ? maxTokenLength : vl;
                stream.seek(kvi.vb);
                String vs = stream.readStripNewLine(l, UTF8);
                if (l != vl) {
                    vs = vs + " (... ещё " + (vl - l) + " симв.)";
                }
                vo = getRidOfUnprintables(vs);
            }
            else {
                vo = "";
            }
            
            String k;
            switch (kv) {
                case 0: k = DATE_PROP_NAME; break;
                case 1: k = TRANSACTION_STATE_PROP_NAME; break;
                case 2: k = TRANSACTION_DATA_PROP_NAME; break;
                case 3: 
                    k = USER_PROP_NAME;
                    vo = catalogs.users.get(vo);
                    break;
                case 4:
                    k = COMPUTER_PROP_NAME;
                    vo = catalogs.computers.get(vo);
                    break;
                case 5:
                    k = APPLICATION_PROP_NAME;
                    vo = catalogs.applications.get(vo);
                    break;
                case 6: k = CONNECTION_PROP_NAME; break;
                case 7:
                    k = EVENT_PROP_NAME;
                    vo = catalogs.events.get(vo);
                    break;
                case 8: k = LOG_LEVEL_PROP_NAME; break;
                case 9: k = COMMENT_PROP_NAME; break;
                case 10:
                    k = METADATA_PROP_NAME;
                    vo = catalogs.metadata.get(vo);
                    break;
                case 11: k = DATA_VALUE_PROP_NAME; break;
                case 12: k = DATA_PRESENTATION_PROP_NAME; break;
                case 13:
                    k = SERVER_PROP_NAME;
                    vo = catalogs.servers.get(vo);
                    break;
                case 14:
                    k = MAIN_PORT_PROP_NAME;
                    vo = catalogs.mainPorts.get(vo);
                    if (vo != null) vo = ((OneCRLCatalogRecord) vo).getValue();
                    break;
                case 15:
                    k = ADDITIONAL_PORT_PROP_NAME;
                    vo = catalogs.additionalPorts.get(vo);
                    if (vo != null) vo = ((OneCRLCatalogRecord) vo).getValue();
                    break;
                case 16: k = SESSION_PROP_NAME; break;
                default: k = kv == kvreclength ? DATA_DIVIDER_PROP_NAME : "";
            }

            if (kv > 17 && kv < kvreclength) { // добавляем в массив дополнительных данных кроме последнего элемента
                assert additionalData != null;
                additionalData.add(vo);
            }
            else {
                logrec.put(k, vo);
                if (DEBUG_RECORDS) { System.out.println(k + "=" + vo); }                
            }
        }
        stream.seek(fp);
        
        // разбор журнала будет продолжен только если количество отфильтрованных записей
        // не больше заданного количества
        //
        boolean continueParsing = onLogRecord(kvrecord);
        if (continueParsing) {
            kvrecord.startsAt = kvrecord.endsAt;
        }
        
        kvreclength = -1;
        
        return continueParsing;
    }
    
    
    @Override
    public void parse(FileState state, String encoding, long fromPosition, int maxRecords, Filter fltr, 
            ParserParameters parameters) throws IOException, ParseException {
        
        if (maxRecords <= 0) {
            return;
        }
        
        catalogs = getDescriptorsCatalogs(state.getFile().getParentFile());
        clearUsedIndexes();

        volume = state.getFile().getName().split("\\.")[0]; // имя файла без расширения
        
        maxCount = maxRecords;
        addFields = state.getFields();
        filter = Filter.and(state.getFilter(), fltr == null ? null : fltr.copy());
        exception = null;
        
        try (BufferedRandomAccessFileStream rafs = new BufferedRandomAccessFileStream(
                state.getOpenedRandomAccessFile(),
                STREAM_BUFFER_SIZE)) {
            stream = rafs;
            stream.seek(fromPosition);
            fileLinesRead = 0;
            KeyValuesRecord raw = read(stream, parameters);
            buildRecord(raw);
        }
        catch (ParseException ex) {
            if (parameters.logParseExceptions()) {
                String parserErrorLog = makeParserErrorsLogDir(parameters);
                File errorFragmentFile = new File(String.format("%s/%s.%s.%s.parse_error", 
                        parserErrorLog,
                        state.getFile().getName(),
                        kvrc.startsAt - 1,
                        filePos));
                copyFileFragment(state.getFile(), 
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


    private KeyValuesRecord read(BufferedRandomAccessFileStream stream, ParserParameters parameters) 
            throws IOException, ParseException {
        
        KeyValuesRecord kvr = new KeyValuesRecord();
        kvrc = kvr;

        long fromPosition = stream.getFilePointer();
        kvr.endsAt = fromPosition;
        kvr.startsAt = fromPosition;
        startPos = fromPosition;
        filePos = fromPosition;
        validBytesRead = fromPosition;

        fileLinesRead = 0;
        unfilteredCount = 0;
        filteredCount = 0;
        delay = parameters == null ? 0 : parameters.getDelay();
        maxTokenLength = parameters == null ? Integer.MAX_VALUE : parameters.getMaxTokenLength();
        getTokenLength = maxTokenLength == Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (maxTokenLength * 1.05);
        
        int nestedCurvedValueLevel = 0;            // уровень вложенности значения в фигурных скобках

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
                                kvc = kvr.kv[++kvr.count];
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
                                kvc = kvr.kv[++kvr.count];
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
                                kvc = kvr.kv[++kvr.count];
                                // зафиксировать начало значения (+1)
                                kvc.vb = firstBytePos + 1;
                            }
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            // если это значение ключа "Locks", то парсим значение
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
                                kvc = kvr.kv[++kvr.count];
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

        return kvr;
    }
    
    
}
