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

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OneCRLParser implements LogParser {
    
    public static boolean DEBUG_SYMBOLS = false;
    public static boolean DEBUG_RECORDS = false;
    
    private static final int STREAM_BUFFER_SIZE = 1024 * 1024 * 2; // 2Mb 

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern UNPRINTABLE_PATTERN = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]");    
    private static final long GC_PER_RECORDS = 10000L;
    private static final long TIME_BASE = - Instant.parse("0001-01-01T00:00:00.00Z").toEpochMilli();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("GMT+0");
    
    static {
        DATE_FORMAT.setTimeZone(TIME_ZONE);
    }
    
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
    public static final String TRANSACTION_ID = "tid";                        // идентификатор транзакции получаем из tdata
    public static final String USER_PROP_NAME = "user";                       // пользователь
    public static final String COMPUTER_PROP_NAME = "computer";               // компьютер
    public static final String APPLICATION_PROP_NAME = "application";         // приложение
    public static final String CONNECTION_PROP_NAME = "connection";           // соединение
    public static final String EVENT_PROP_NAME = "event";                     // событие
    public static final String LOG_LEVEL_PROP_NAME = "level";                 // уровень события (важность)
    public static final String COMMENT_PROP_NAME = "comment";                 // комментарий
    public static final String METADATA_PROP_NAME = "metadata";               // метаданные
    public static final String DATA_VALUE_PROP_NAME = "data";                 // данные
    public static final String DATA_PRESENTATION_PROP_NAME = "presentation";  // представление данных
    public static final String SERVER_PROP_NAME = "server";                   // сервер
    public static final String MAIN_PORT_PROP_NAME = "port";                  // основной порт
    public static final String SYNC_PORT_PROP_NAME = "syncport";              // вспомогательный порт
    public static final String SESSION_PROP_NAME = "session";                 // сеанс
    public static final String ADDITIONAL_DATA_PROP_NAME = "raw";             // дополнительные данные (массив)
    public static final String DATA_DIVIDER_PROP_NAME = "sdseparator";        // разделитель данных

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
    
    private boolean compact;
    private final HashSet usedUsers;
    private final HashSet usedComputers;
    private final HashSet usedApplications;
    private final HashSet usedEvents;
    private final HashSet usedMetadata;
    private final HashSet usedServers;
    
    private final HashMap<Object, Integer> tDict;     // кэш данных транзакций для преобразования в коды выгрузки
    private final HashMap<String, String> tIdModel;   // шаблон объекта, описывающего идентификатор транзакции
    
    private String prevTdata = null; // для ускорения преобразования данных транзакции в идентификатор транзакции
    private String prevTid = null;   // для ускорения преобразования данных транзакции в идентификатор транзакции
    
    private byte icc = 0;  // текущий однобайтный UTF-символ
    
                                             // Key-Value Record - номера позиций файла с началом и окончанием 
                                             // ключа и значения для одной записи лога; предполагаем, что количество
                                             // пар ключ-значение в одной записи не более 64
    
    private final KeyValuesRecord kvrc = new KeyValuesRecord();

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
        public String kv = null; // значение ключа строкой
    }
    
    
    private static class KeyValuesRecord {
        private static final int MAX_RECORDS = 64;
        public final KeyValueBounds[] kv = new KeyValueBounds[MAX_RECORDS];
        public final OneCRLRecord lr = new OneCRLRecord();  // хранит запись лога в режиме "compact"/"не compact"
        public final OneCRLRecord flr = new OneCRLRecord(); // хранит запись лога в режиме "не compact"
        public long endsAt = 0;                             // позиция конца записи в файле
        public long startsAt = 0;                           // позиция начала записи в файле
        public boolean isEmpty = true;                      // содержит ли записи
        public KeyValuesRecord() {
            for (int i = 0; i < kv.length; i++) kv[i] = new KeyValueBounds();
        }
        public void clear() {
            lr.clear();
            flr.clear();
            endsAt = 0;
            startsAt = 0;
            isEmpty = true;
        }
    }
    
    
    public OneCRLParser() {
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
        
        tDict = new HashMap();
        tIdModel = new HashMap<>();
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
        tDict.clear();
    }
    
    
    public String convertToTransactionId(String tdata) {
        // {24517ce81d1c0,3d}
        if (tdata.equals(prevTdata)) {
            return prevTid;
        }
        if (!tdata.startsWith("{") || !tdata.endsWith("}")) {
            return tdata;
        }
        int c = tdata.indexOf(',');
        if (c < 0) {
            return tdata;
        }
        prevTdata = tdata;
        String hexDate = tdata.substring(1, c);
        String hexOffset = tdata.substring(c + 1, tdata.length() - 1);
        prevTid = String.format("%s (%d)",
                DATE_FORMAT.format(new Date((Long.parseLong(hexDate, 16) / 10) - TIME_BASE)),
                Integer.parseInt(hexOffset, 16));
        return prevTid;
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
    
    
    private boolean canProceedRecord(KeyValuesRecord keyValueRecord) {
        if (filteredCount >= maxCount) {
            return false;
        }
        if (!keyValueRecord.isEmpty) {
            validBytesRead = keyValueRecord.endsAt - 1;
            unfilteredCount++;
            if (unfilteredCount % gcPerRecords == 0) {
                System.gc();
            }
        }
        return true;
    }

    
    private boolean filterAccepted(KeyValuesRecord keyValueRecord) throws Exception {
        if (keyValueRecord.isEmpty) {
            return false;
        }
        OneCRLRecord logRecord = keyValueRecord.lr;
        OneCRLRecord checkLogRecord = compact ? keyValueRecord.flr : logRecord;
        boolean accepted = (filter == null || filter.accept(checkLogRecord));
        if (accepted) {
            filteredCount++;
            if (addFields != null) {
                logRecord.putAll(addFields);
            }
        }
        if (delay > 0) {
            Thread.sleep(delay);
        }
        return accepted;
    }

    
    private boolean buildRecord() throws IOException {
        OneCRLRecord logrec = kvrc.lr;
        logrec.clear();

        OneCRLRecord fullrec = kvrc.flr;
        fullrec.clear();

        kvrc.endsAt = filePos;
        kvrc.isEmpty = (kvcc == -1);
        
        logrec.put(VOLUME_PROP_NAME, volume);
        logrec.put(REFERENCE_PROP_NAME, kvrc.startsAt);
        
        if (compact) {
            fullrec.put(VOLUME_PROP_NAME, volume);
            fullrec.put(REFERENCE_PROP_NAME, kvrc.startsAt);
        }
        
        ArrayList<Object> additionalData = null;
        if (kvcc > 18) {
            additionalData = new ArrayList<>();
            logrec.put(ADDITIONAL_DATA_PROP_NAME, additionalData);
            if (compact) {
                fullrec.put(ADDITIONAL_DATA_PROP_NAME, additionalData);
            }
        }
        
        Integer idx = null;
        Object user, computer, application, event, metadata, server, transaction;
        user = computer = application = event = metadata = server = transaction = null;

        Object vo, fvo;
        long fp = stream.getFilePointer();
        for (byte kv = 0; kv <= kvcc; kv++) {
            if (kv == 17) continue; // количество записей в дополнительных данных - не нужно возвращать
            
            KeyValueBounds kvi = kvrc.kv[kv];
            
            if (kvi.vb > 0) {
                int vl = (int)(kvi.ve - kvi.vb);
                int l = vl > getTokenLength ? maxTokenLength : vl;
                stream.seek(kvi.vb);
                String vs = stream.readStripNewLine(l, UTF8);
                if (l != vl) {
                    vs = vs + " (... ещё " + (vl - l) + " симв.)";
                }
                vo = fvo = getRidOfUnprintables(vs);
            }
            else {
                vo = fvo = "";
            }
            
            String k;
            switch (kv) {
                case 0: k = DATE_PROP_NAME; break;
                case 1: k = TRANSACTION_STATE_PROP_NAME; break;
                case 2: 
                    k = TRANSACTION_ID;
                    if ("{0,0}".equals(vo)) {
                        vo = fvo = "0";
                    }
                    else if (compact) {
                        tIdModel.put("value", (String) vo);
                        tIdModel.put("presentation", convertToTransactionId((String) vo));
                    //  tIdModel.put("decimal", tid.replaceAll("[\\.: ()]", ""));
                        idx = tDict.get(vo);
                        if (idx == null) {
                            idx = tDict.size() + 1;
                            transaction = vo;
                            tIdModel.put("index", idx.toString());
                            vo = fvo = tIdModel;
                        }
                        else {
                            tIdModel.put("index", idx.toString());
                            fvo = tIdModel;
                            vo = idx.toString();
                        }
                    }
                    else {
                        vo = convertToTransactionId((String) vo);
                    }
                    break;
                case 3: 
                    k = USER_PROP_NAME;
                    fvo = catalogs.users.get(vo);
                    if (compact) {
                        if (!usedUsers.contains(vo)) {
                            user = vo;
                            vo = fvo;
                        }
                    }
                    else {
                        vo = fvo;
                    }
                    break;
                case 4:
                    k = COMPUTER_PROP_NAME;
                    fvo = catalogs.computers.get(vo);
                    if (compact) {
                        if (!usedComputers.contains(vo)) {
                            computer = vo;
                            vo = fvo;
                        }
                    }
                    else {
                        vo = fvo;
                    }
                    break;
                case 5:
                    k = APPLICATION_PROP_NAME;
                    fvo = catalogs.applications.get(vo);
                    if (compact) {
                        if (!usedApplications.contains(vo)) {
                            application = vo;
                            vo = fvo;
                        }
                    }
                    else {
                        vo = fvo;
                    }
                    break;
                case 6: k = CONNECTION_PROP_NAME; break;
                case 7:
                    k = EVENT_PROP_NAME;
                    fvo = catalogs.events.get(vo);
                    if (compact) {
                        if (!usedEvents.contains(vo)) {
                            event = vo;
                            vo = fvo;
                        }
                    }
                    else {
                        vo = fvo;
                    }
                    break;
                case 8: k = LOG_LEVEL_PROP_NAME; break;
                case 9: k = COMMENT_PROP_NAME; break;
                case 10:
                    k = METADATA_PROP_NAME;
                    fvo = catalogs.metadata.get(vo);
                    if (compact) {
                        if (!usedMetadata.contains(vo)) {
                            metadata = vo;
                            vo = fvo;
                        }
                    }
                    else {
                        vo = fvo;
                    }
                    break;
                case 11: k = DATA_VALUE_PROP_NAME; break;
                case 12: k = DATA_PRESENTATION_PROP_NAME; break;
                case 13:
                    k = SERVER_PROP_NAME;
                    fvo = catalogs.servers.get(vo);
                    if (compact) {
                        if (!usedServers.contains(vo)) {
                            server = vo;
                            vo = fvo;
                        }
                    }
                    else {
                        vo = fvo;
                    }
                    break;
                case 14:
                    k = MAIN_PORT_PROP_NAME;
                    vo = fvo = catalogs.mainPorts.get(vo);
                    if (vo != null) vo = fvo = ((OneCRLCatalogRecord) vo).getValue();
                    break;
                case 15:
                    k = SYNC_PORT_PROP_NAME;
                    vo = fvo = catalogs.additionalPorts.get(vo);
                    if (vo != null) vo = fvo = ((OneCRLCatalogRecord) vo).getValue();
                    break;
                case 16: k = SESSION_PROP_NAME; break;
                default: k = kv == kvcc ? DATA_DIVIDER_PROP_NAME : "";
            }
            
            if (vo == null) vo = "0";
            if (compact && fvo == null) fvo = "0";

            if (kv > 17 && kv < kvcc) { // добавляем в массив дополнительных данных кроме последнего элемента
                assert additionalData != null;
                additionalData.add(vo);
            }
            else {
                logrec.put(k, vo);
                if (compact) {
                    fullrec.put(k, fvo);
                }
                if (DEBUG_RECORDS) { System.out.println(k + "=" + vo); }                
            }
        }
        stream.seek(fp);
        
        // разбор журнала будет продолжен только если количество
        // отфильтрованных записей не больше заданного количества
        //
        boolean continueParsing = false;
        if (canProceedRecord(kvrc)) {
        
            try {
                if (filterAccepted(kvrc)) {

                    recordsStorage.put(kvrc.lr);

                    if (transaction != null) tDict.put(transaction, idx);
                    if (user != null) usedUsers.add(user);
                    if (computer != null) usedComputers.add(computer);
                    if (application != null) usedApplications.add(application);
                    if (event != null) usedEvents.add(event);
                    if (metadata != null) usedMetadata.add(metadata);
                    if (server != null) usedServers.add(server);
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
            
            continueParsing = filteredCount < maxCount;

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
        
        catalogs = getDescriptorsCatalogs(state.getFile().getParentFile());
        clearUsedIndexes();

        volume = state.getFile().getName().split("\\.")[0]; // имя файла без расширения
        
        maxCount = maxRecords;
        addFields = state.getFields();
        filter = Filter.and(state.getFilter(), fltr == null ? null : fltr.copy());
        exception = null;
        compact = parameters.isCompact();
        
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
            tIdModel.clear();
            clearUsedIndexes();
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

        fileLinesRead = 0;
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
