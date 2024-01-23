package com.monitor.parser.onecf;


import com.monitor.agent.server.BufferedRandomAccessFileStream;
import com.monitor.agent.server.FileState;
import com.monitor.agent.server.PredefinedFields;
import com.monitor.agent.server.filter.Filter;
import com.monitor.parser.LogParser;
import com.monitor.parser.ParseException;
import com.monitor.parser.ParserParameters;
import com.monitor.parser.onec.TokenMgrError;
import com.monitor.parser.reader.ParserNullStorage;
import com.monitor.parser.reader.ParserRecordsStorage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

/*
 * Copyright 2023 Алексей.
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

/**
 *
 * @author Алексей
 */
public class FastTJParser implements LogParser {
    
    private static final int STREAM_BUFFER_SIZE = 1024 * 1024 * 100; // 200Mb 
    private static final boolean DEBUG_SYMBOLS = false;
    private static final boolean DEBUG_RECORDS = false;
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final long MICROSECONDS_TO_1970 = 62135596800000L * 1000L;
    private static final String SQL_PARAMETERS_PROP_MARKER = "\np_0:";
    private static final Pattern UNPRINTABLE_PATTERN = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]");    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("GMT+0");
    
    static {
        DATE_FORMAT.setTimeZone(TIME_ZONE);
    }

    private static final byte EOF = -1;
    private static final byte TAB = 9;
    private static final byte NEW_LINE = 10;
    private static final byte CARRIAGE_RETURN = 13;
    private static final byte SPACE = 32;
    private static final byte QUOTATION_MARK = 34;
    private static final byte APOSTROPHE = 39;
    private static final byte COMMA = 44;
    private static final byte HYPHEN = 45;
    private static final byte EQUALS = 61;
    private final static byte CHR0 = '0';
    
    private static final byte MODE_RECORD_TERMINATE = 0;                   // чтение окончания записи
    private static final byte MODE_TIMESTAMP = 1;                          // чтение времени события
    private static final byte MODE_DURATION_EXPECTED = 2;                  // ожидается начало продолжительности события
    private static final byte MODE_DURATION = 3;                           // чтение продолжительности события
    private static final byte MODE_EVENT_EXPECTED = 4;                     // ожидается именя события
    private static final byte MODE_EVENT = 5;                              // чтение имени события
    private static final byte MODE_EVENT_LEVEL_EXPECTED = 6;               // ожидается уровень события
    private static final byte MODE_EVENT_LEVEL = 7;                        // чтение уровня события
    private static final byte MODE_KEY_EXPECTED = 8;                       // ожидается начало key
    private static final byte MODE_KEY = 9;                                // чтение key
    private static final byte MODE_VALUE_EXPECTED = 10;                    // ожидается начало value
    private static final byte MODE_VALUE = 11;                             // чтение value
    private static final byte MODE_VALUE_INSIDE_QUOTATION_MARK = 12;       // чтение value в кавычках
    private static final byte MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED = 13;    // ожидается запятая или кавычка
    private static final byte MODE_VALUE_INSIDE_APOSTROPHE = 14;           // чтение value в апострофах
    private static final byte MODE_VALUE_IA_COMMA_OR_APO_EXPECTED = 15;    // ожидается запятая или апостороф
    
    private static final byte VALUE_MODE_GENERAL = 0;
    private static final byte VALUE_MODE_LOCKS = 1;
    
    private static final byte TOKEN_KIND_UNDEFINED = 0;
    private static final byte TOKEN_KIND_LOCK_SPACE = 1;
    private static final byte TOKEN_KIND_LOCK_TYPE = 2;
    private static final byte TOKEN_KIND_LOCK_KEY = 3;
    private static final byte TOKEN_KIND_LOCK_VALUE = 4;
    
    private BufferedRandomAccessFileStream stream;
    
    private byte mode = MODE_TIMESTAMP;
    private byte valueMode = VALUE_MODE_GENERAL;
    private long firstBytePos = 0;
    private long filePos = 0;
    private long fileLinesRead = 0;

    private final byte[] b2 = new byte[2];
    private final byte[] b3 = new byte[3];
    private final byte[] b4 = new byte[4];
    
    private byte icc = 0;  // текущий однобайтный символ
    private byte ic2 = 0;
    private byte ic3 = 0;
    private byte ic4 = 0;
    
    private byte bytesInSym;                 // количество байтов в прочитанном UTF-8 символе

                                             // Key-Value Record - номера позиций файла с началом и окончанием 
                                             // ключа и значения для одной записи лога; предполагаем, что количество
                                             // пар ключ-значение в одной записи не более 64
    
    private final KeyValuesRecord kvr0 = new KeyValuesRecord();
    private final KeyValuesRecord kvr1 = new KeyValuesRecord();
    
    private KeyValuesRecord kvrc = kvr0;     // границы ключей и значений для текущей записи
    private KeyValuesRecord kvrp = kvr1;     // границы ключей и значений для предыдущей записи
    
    private boolean kvsh = false;            // на какой из рабочих массивов ссылается kvrc - на kvr0 или kvr0
    
    private byte kvcc;                       // количество-1 прочитанных пар ключ-значение в текущей записи лога kvrc
    
    private final StringConstructor sc = new StringConstructor();
    
    
    // locks variables
       
    private final EqualsMatcher locksMatcher = new EqualsMatcher(LOCKS_PROP_NAME);
    private final EqualsMatcher contextMatcher = new EqualsMatcher(CONTEXT_PROP_NAME);
    private final EqualsMatcher processNameMatcher = new EqualsMatcher(PROCESS_NAME_PROP_NAME);
    private final EqualsMatcher computerNameMatcher = new EqualsMatcher(COMPUTER_NAME_PROP_NAME);
    private final EqualsMatcher usrMatcher = new EqualsMatcher(USR_PROP_NAME);
    private final EqualsMatcher escalatingMatcher = new EqualsMatcher(ESCALATING_PROP_NAME);
    private final EqualsMatcher contextEventMatcher = new EqualsMatcher(CONTEXT_EVENT_NAME);
    private final EqualsMatcher sharedMatcher = new EqualsMatcher("Shared");
    private final EqualsMatcher exclusiveMatcher = new EqualsMatcher("Exclusive");
    
    private Lock lck = new Lock();                 // воплощение управляемой блокировки
    private LockElement lckel = new LockElement(); // текущий заполняемый элемент управляемой блокировки
    private Token lckelt = new Token();            // текущий токен заполняемого элемента управляемой блокировки
    
    long tokenBegin = 0;
    int  tokenRowLevel = 0;
    
    private static final byte IN_LOCK_MODE_LOCK_VALUE_EXPECTED = 0;
    private static final byte IN_LOCK_MODE_LOCK_TYPE_EXPECTED = 1;
    private static final byte IN_LOCK_MODE_LOCK_KEY_EXPECTED = 2;
    private static final byte IN_LOCK_MODE_SPACENAME_EXPECTED = 3;
    private static final byte IN_LOCK_MODE_SPACENAME = 4;
    private static final byte IN_LOCK_MODE_LOCK_KEY = 5;
    private static final byte IN_LOCK_MODE_LOCK_VALUE = 6;
    private static final byte IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK = 7;
    private static final byte IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE = 8;
    private static final byte IN_LOCK_MODE_LOCK_TYPE = 9;
    private static final byte IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED = 10;
    private static final byte IN_LOCK_MODE_LOCK_VALUE_IA_SPACE_OR_APO_EXPECTED = 11;
    private static final byte IN_LOCK_MODE_RECORD_TERMINATE = 12;
    private static final byte IN_LOCK_MODE_SECOND_QUOTATION_MARK_EXPECTED = 13;
    private static final byte IN_LOCK_MODE_SECOND_APOSTROPHE_EXPECTED = 14;
    
    private byte inLockMode;                       // режим парсинга управляемой блокировки
    private byte inLockModeNext;                   // режим парсинга управляемой блокировки для установки следующим
    private byte inLockModeBackup;                 // режим парсинга управляемой блокировки для восстановления
    
    
    



    private final static String CONTEXT_EVENT_NAME = "Context";
    
    private final static String TIMESTAMP_PROP_NAME = "timestamp";
    private final static String DURATION_PROP_NAME = "duration";
    private final static String EVENT_PROP_NAME = "event";
    private final static String EVENT_HASH_PROP_NAME = "event.hash()"; // TODO: надо?
    private final static String LEVEL_PROP_NAME = "level";
    private final static String START_DATE_TIME_PROP_NAME = "startDateTime";
    private final static String ONLY_TIME_PROP_NAME = "onlyTime";
    private final static String SQL_PARAMETERS_PROP_NAME = "Sql.p_N";
    private final static String DATE_TIME_PROP_NAME = "dateTime";
    private final static String CONTEXT_PROP_NAME = "Context";
    private final static String PROCESS_NAME_PROP_NAME = "p:processName";
    private final static String COMPUTER_NAME_PROP_NAME = "t:computerName";
    private final static String USR_PROP_NAME = "Usr";
    private final static String ESCALATING_PROP_NAME = "escalating";
    private final static String CONTEXT_LAST_LINE_PROP_NAME = "ContextLastLine";

    private final static String LOCKS_PROP_NAME = "Locks";
    private final static String LOCK_SPACE_NAME_PROP_NAME = "space";
    private final static String LOCK_SPACE_TYPE_PROP_NAME = "type";
    private final static String LOCK_SPACE_RECORDS_PROP_NAME = "records";
    private final static String LOCK_SPACE_RECORDS_COUNT_PROP_NAME = "recordsCount";
    private final static String LOCK_GRANULARITY_PROP_NAME = "granularity";

    private String syear, smonth, sday, shours, sminutes, sseconds, yyyyMMddhh;
    private long mm, ss, ms, timestamp;
    private long microsecondsBase;
    private int tssymbols;
    private long duration = 0;
    private boolean v82format;
    private String eventName;
    private int eventLevel;

    private ParserRecordsStorage recordsStorage;
    private Throwable exception;
    private PredefinedFields addFields;
    private Filter filter;
    private int maxCount;
    private long unfilteredCount = 0L;
    private long filteredCount;
    private int delay;
    

    
    
    
    private static List<File> filesFromDirectory(String directoryName) {
        List<File> result = new ArrayList<>();
        filesFromDirectory(new File(directoryName), result);
        return result;
    }
    
    
    private static void filesFromDirectory(File file, List<File> filesList) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File child : files) {
                filesFromDirectory(child, filesList);
            }
        }
        else if (file.isFile()) {
            filesList.add(file);
        }
    }
    
    
    public static boolean copyFile(File src, File dest, boolean rewrite) throws IOException {
        if (dest.exists()) {
            if (!rewrite) {
                return false;
            }
            if (!dest.delete()) {
                return false;
            }
        }
        try (
                InputStream in = new BufferedInputStream(new FileInputStream(src));
                OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {

            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        }
        catch (Exception ex) {
            return false;
        }
        return true;
    }
    
    
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

    
    private static String right(String str, int count) {
        return str.substring(str.length() - count);
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
        return kvrp.bytesRead;
    }

    
    @Override
    public Throwable getException() {
        return exception;
    }
    
    
    private class StringConstructor {
        private final byte[] strb = new byte[128];
        private int size = 0;
        private void reset() { size = 0; }
        private void addByte(byte b) { strb[size++] = b; }
        @Override
        public String toString() { return new String(strb, 0, size, UTF8); }
    }
    
    
    private class KeyValueBounds {
        public long kb = 0;
        public long ke = 0;
        public long vb = 0;
        public long ve = 0;
        public boolean isLocks = false;
        public boolean isContext = false;
        public boolean isProcessName = false;
        public boolean isComputerName = false;
        public boolean isUsr = false;
        public boolean isEscalating = false;
    }
    
    
    private class KeyValuesRecord {
        public KeyValueBounds[] kv = new KeyValueBounds[64];
        public OneCTJRecord lr = new OneCTJRecord();
        public long bytesRead = 0;                           // поизиция конца записи в файле
        public boolean isReadyToStore = false;               // готова к помещению в хранилище?
        public boolean isContext = false;                    // event == Context?
        public Lock lck = new Lock();                        // данные значения ключа Locks (если имеется в записи ЖР)
        public String context = null;                        // значение поля Context (если есть)
        public String processName = null;                    // значение поля p:processName (если есть)
        public String computerName = null;                   // значение поля t:computerName (если есть)
        public String usr = null;                            // значение поля Usr (если есть)
        public KeyValuesRecord() {
            for (int i = 0; i < kv.length; i++) kv[i] = new KeyValueBounds();
        }
        
    }
    
    
    private class Token {
        public long tb = 0;                     // token begin file pos
        public long te = 0;                     // token end file pos
        public byte tk = TOKEN_KIND_UNDEFINED;  // token kind
        public int  tl = 0;                     // token [row] level
    }
    
    
    private class LockElement {
        // последовательнсть токенов - это описание элемента блокировки:
        // 1) пространство блокировки
        // 2) тип блокировки (shared|exclusive)
        // 3) ключ пространства блокировки
        // 4) значение ключа пространства блокировки
        // ... повторяются 3 и 4 (0..N раз), возможно изменение tl (token [row] level) у пары ключ-значение
        //
        public ArrayList<Token> letl;  // lock element tokens list
        public int tc;                 // tokens count
        public int maxtc;              // maximum tokens count after previous iterations
        
        public LockElement() {
            letl = new ArrayList<>();
            tc = maxtc = 0;
        }
        
        public Token addToken(long tb, long te, byte tk, int tl) {
            Token res;
            if (tc == maxtc) {
                res = new Token();
                letl.add(res);
                tc = ++maxtc;
            }
            else {
                res = letl.get(tc++);
            }
            res.tb = tb;
            res.te = te;
            res.tk = tk;
            res.tl = tl;
            return res;
        }
        
        public void reinit() {
            tc = 0;
        }
    }
    
    
    private class Lock {
        public ArrayList<LockElement> lel; // lock elements list
        public int lec;                    // lock elements count
        public int maxlec;                 // maximum lock elements count after previous iterations
        
        public Lock() {
            lel = new ArrayList<>();
            lec = maxlec = 0;
        }
        
        public LockElement addLockElement() {
            LockElement res;
            if (lec == maxlec) {
                res = new LockElement();
                lel.add(res);
                lec = ++maxlec;
            }
            else {
                res = lel.get(lec++);
                res.reinit();
            }
            return res;
        }
        
        public Lock reinit() {
            lec = 0;
            return this;
        }
    }
    
    
    private class ContainsMatcher {
        
        private final byte[] src;     // байты исходной строки
        private int[] searches;       // список следующих проверяемых позищий в src (когда предыдущие подошли)
        private int maxSearch;        // id последнего рабочего элемента в списке позиций поиска (<= searches.length)

        public ContainsMatcher(String src) {
            this.src = src.getBytes(UTF8);
            this.searches = new int[] {0, 0};
            this.maxSearch = 0;
        }
        
        public int newSearch() {
            maxSearch++;
            if (maxSearch == searches.length) { 
                expandSearches();
            }
            return maxSearch;
        }
        
        private void expandSearches() {
            int[] expanded = new int[searches.length + 1];
            System.arraycopy(searches, 0, expanded, 0, searches.length);
            searches = expanded;
            searches[searches.length - 1] = 0;
        }
        
        public int delSearch(int id) {
            if (id == maxSearch) {
                searches[id] = 0;
                return maxSearch;
            }
            for (int i = id; i < maxSearch; i++) {
                searches[i] = searches[i + 1];
            }
            searches[maxSearch] = 0;
            if (maxSearch > 0) maxSearch--;
            return maxSearch;
        }
        
        public int maxSearch() {
            return maxSearch;
        }
        
        public int length() {
            return src.length;
        }
        
        // если на момент проверки очередного символа b, выраженного в байтах,
        // обнаружено совпадение, то возвращается true, иначе возвращается false;
        // это универсальная функция, но так как она работает с массивами, то
        // в класс добавлены ещё 4 аналогичные функции, работающие только с байтами,
        // исключительно для повышения производительности
        //
        public boolean checkNext(byte[] b) {
            int cs = 0;            // current search id
            int ms = maxSearch();  // max search id
            boolean res = false;
            while (cs <= ms) {
                boolean delete = false;
                int pos = searches[cs];
                if (pos + b.length > src.length) {
                    delete = true;
                }
                else {
                    for (byte s : b) {
                        if (s != src[pos]) {
                            delete = true;
                            break;
                        }
                        pos++;
                    }
                }
                if (delete) {
                    int nms = delSearch(cs);
                    if (cs == ms) cs++;
                    ms = nms;
                }
                else {
                    searches[cs] = pos;
                    res = res || pos == src.length;
                    cs++;
                }
            }
            if (searches[ms] > 0) newSearch();
            return res;
        }
        
        public boolean checkNext1(byte b1) {
            int cs = 0;            // current search id
            int ms = maxSearch();  // max search id
            boolean res = false;
            while (cs <= ms) {
                int pos = searches[cs];
                if (pos + 1 <= src.length && b1 == src[pos]) {
                    pos = pos + 1;
                    searches[cs] = pos;
                    res = res || pos == src.length;
                    cs++;
                }
                else {
                    int nms = delSearch(cs);
                    if (cs == ms) cs++;
                    ms = nms;
                }
            }
            if (searches[ms] > 0) newSearch();
            return res;
        }
        
        public boolean checkNext2(byte b1, byte b2) {
            int cs = 0;            // current search id
            int ms = maxSearch();  // max search id
            boolean res = false;
            while (cs <= ms) {
                int pos = searches[cs];
                if (pos + 2 <= src.length && b1 == src[pos] && b2 == src[pos + 1]) {
                    pos = pos + 2;
                    searches[cs] = pos;
                    res = res || pos == src.length;
                    cs++;
                }
                else {
                    int nms = delSearch(cs);
                    if (cs == ms) cs++;
                    ms = nms;
                }
            }
            if (searches[ms] > 0) newSearch();
            return res;
        }
        
        public boolean checkNext3(byte b1, byte b2, byte b3) {
            int cs = 0;            // current search id
            int ms = maxSearch();  // max search id
            boolean res = false;
            while (cs <= ms) {
                int pos = searches[cs];
                if (pos + 3 <= src.length && b1 == src[pos] && b2 == src[pos + 1] && b3 == src[pos + 2]) {
                    pos = pos + 3;
                    searches[cs] = pos;
                    res = res || pos == src.length;
                    cs++;
                }
                else {
                    int nms = delSearch(cs);
                    if (cs == ms) cs++;
                    ms = nms;
                }
            }
            if (searches[ms] > 0) newSearch();
            return res;
        }
        
        public boolean checkNext4(byte b1, byte b2, byte b3, byte b4) {
            int cs = 0;            // current search id
            int ms = maxSearch();  // max search id
            boolean res = false;
            while (cs <= ms) {
                int pos = searches[cs];
                if (pos + 4 <= src.length && b1 == src[pos] && b2 == src[pos + 1] && b3 == src[pos + 2] && b4 == src[pos + 3]) {
                    pos = pos + 4;
                    searches[cs] = pos;
                    res = res || pos == src.length;
                    cs++;
                }
                else {
                    int nms = delSearch(cs);
                    if (cs == ms) cs++;
                    ms = nms;
                }
            }
            if (searches[ms] > 0) newSearch();
            return res;
        }
        
        public boolean checkNext(byte b1, byte b2, byte b3, byte b4, byte count) {
            switch (count) {
                case 1:
                    return checkNext1(b1);
                case 2:
                    return checkNext2(b1, b2);
                case 3:
                    return checkNext3(b1, b2, b3);
                default:
                    return checkNext4(b1, b2, b3, b4);
            }
        }
        
        public boolean checkBytes(byte[] bytes, int from, int to) {
            if (src.length != to - from) { return false; }
            int pos = from;
            for (int i = 0; i < src.length; i++) {
                if (src[i] != bytes[pos++]) { return false; }
            }
            return true;
        }
        
    }

    
    private class EqualsMatcher {
        
        private final byte[] src;     // байты исходной строки
        private int srcPos;           // проверяемая позиция исходной строки
        private boolean matches;

        public EqualsMatcher(String src) {
            this.src = src.getBytes(UTF8);
            this.srcPos = 0;
            this.matches = true;
        }
        
        public void newSearch() {
            srcPos = 0;
            matches = true;
        }
        
        public int length() {
            return src.length;
        }
        
        public boolean matches() {
            return matches && srcPos == src.length;
        }
        
        // если на момент проверки очередного символа b, выраженного в байтах,
        // обнаружено совпадение, то возвращается true, иначе возвращается false;
        // это универсальная функция, но так как она работает с массивами, то
        // в класс добавлены ещё 4 аналогичные функции, работающие только с байтами,
        // исключительно для повышения производительности
        //
        public boolean checkNext(byte[] b) {
            if (!matches) { return false; }
            if (srcPos + b.length > src.length) { 
                matches = false;
                return false;
            }
            for (byte s : b) {
                if (s != src[srcPos]) {
                    matches = false;
                    return false;
                }
                srcPos++;
            }
            return true;
        }
        
        public boolean checkNext1(byte b1) {
            if (!matches) { return false; }
            if (srcPos + 1 > src.length) { 
                matches = false;
                return false;
            }
            if (b1 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            return true;
        }
        
        public boolean checkNext2(byte b1, byte b2) {
            if (!matches) { return false; }
            if (srcPos + 2 > src.length) { 
                matches = false;
                return false;
            }
            if (b1 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            if (b2 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            return true;
        }
        
        public boolean checkNext3(byte b1, byte b2, byte b3) {
            if (!matches) { return false; }
            if (srcPos + 3 > src.length) { 
                matches = false;
                return false;
            }
            if (b1 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            if (b2 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            if (b3 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            return true;
        }
        
        public boolean checkNext4(byte b1, byte b2, byte b3, byte b4) {
            if (!matches) { return false; }
            if (srcPos + 4 > src.length) { 
                matches = false;
                return false;
            }
            if (b1 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            if (b2 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            if (b3 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            if (b4 != src[srcPos]) {
                matches = false;
                return false;
            }
            srcPos++;
            return true;
        }

        public boolean checkNext(byte b1, byte b2, byte b3, byte b4, byte count) {
            switch (count) {
                case 1:
                    return checkNext1(b1);
                case 2:
                    return checkNext2(b1, b2);
                case 3:
                    return checkNext3(b1, b2, b3);
                default:
                    return checkNext4(b1, b2, b3, b4);
            }
        }
    }
    
    
    public FastTJParser() {
        recordsStorage = new ParserNullStorage(); // new ParserListStorage();
        unfilteredCount = 0;
        filteredCount = 0;
        fileLinesRead = 0;
        exception = null;
        delay = 0;

        kvcc = -1;
        stream = null;
    }
    
    
    private Set<String> getLockMemberId(Map<String, Object> lockMember) {
        HashSet<String> id = new HashSet<>();
        id.add((String) lockMember.get(LOCK_SPACE_NAME_PROP_NAME));
        id.add((String) lockMember.get(LOCK_SPACE_TYPE_PROP_NAME));
        ArrayList records = (ArrayList) lockMember.get(LOCK_SPACE_RECORDS_PROP_NAME);
        if (!records.isEmpty()) {
            id.addAll(((HashMap) records.get(0)).keySet());
        }
        return id;
    }

    
    private boolean onLogRecord(OneCTJRecord logRecord) {
        if (filteredCount >= maxCount) {
            return false;
        }
        unfilteredCount++;
        if (logRecord.containsLocks) {
            // отдельная обработка, если у события есть свойство "Locks":
            // каждая запись пространства блокировок со своими данными
            // должна быть организована в отдельную запись журнала
            //
            boolean result = true;

            ArrayList<HashMap<String, Object>> lockMembers = (ArrayList<HashMap<String, Object>>) logRecord.get(LOCKS_PROP_NAME);
            if (lockMembers != null && !lockMembers.isEmpty()) {
                // в полученном массиве элементов блокировки lockSpaces может встретиться ситуация, когда
                // в нём существует несколько элементов с одним и тем же пространством блокриовок и с одним
                // и тем же набором полей - в таком случае элементы блокировки нужно объединить, соединяя
                // значения записей, по которым делается блокировка
                //
                if (lockMembers.size() > 1) {
                    // предварительный проход по всем элементам блокировки: если все пространства блокировок
                    // разные, то это самый хороший случай - никаких дополнительных исследований на одинаковость
                    // наборов измерений проводить не нужно
                    //
                    boolean hasSameSpace = false;
                    HashSet<String> spaceNames = new HashSet<>();
                    for (HashMap<String, Object> lockMember : lockMembers) {
                        if (!spaceNames.add((String) lockMember.get(LOCK_SPACE_NAME_PROP_NAME))) {
                            hasSameSpace = true;
                            break;
                        }
                    }
                    if (hasSameSpace) {
                        // самый неприятный случай: придётся перебирать все элементы блокировки, проверять
                        // уникальность триады {имя-пространства-блокировки, тип-блокировки, набор-измерений};
                        // у всех, имеющих одну и ту же триаду, объединять значения измерений;
                        // идентификатор триады - HashSet, состоящий из всех компонент триады - его будем
                        // использовать для сравнения триад
                        //
                        ArrayList<Set<String>> lockMemberIds = new ArrayList<>(lockMembers.size());
                        lockMemberIds.add(getLockMemberId(lockMembers.get(0)));
                        int i = 1;
                        while (i < lockMembers.size()) {
                            HashMap<String, Object> lockMemberI = lockMembers.get(i);
                            Set<String> idI = getLockMemberId(lockMemberI);
                            boolean foundSameId = false;
                            int j = 0;
                            while (j < i) {
                                Set<String> idJ = lockMemberIds.get(j);
                                foundSameId = idI.equals(idJ);
                                if (foundSameId) {
                                    break;
                                }
                                j = j + 1;
                            }
                            if (foundSameId) {
                                // добавляем все значения измерений элемента блокировки к такому же элементу,
                                // который встречался ранее, а текущий элемент блокировки исключаем из списка
                                // элементов всей блокировки
                                //
                                HashMap<String, Object> lockMemberJ = lockMembers.get(j);
                                ArrayList<?> lockMemberRecords = (ArrayList<?>) lockMemberJ.get(LOCK_SPACE_RECORDS_PROP_NAME);
                                lockMemberRecords.addAll((ArrayList) lockMemberI.get(LOCK_SPACE_RECORDS_PROP_NAME));
                                lockMemberJ.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME,
                                        (Integer) lockMemberJ.get(LOCK_SPACE_RECORDS_COUNT_PROP_NAME)
                                        + (Integer) lockMemberI.get(LOCK_SPACE_RECORDS_COUNT_PROP_NAME));
                                lockMembers.remove(i);
                            }
                            else {
                                lockMemberIds.add(idI);
                                i = i + 1;
                            }
                        }
                    }
                }

                int lockMemberNum = 0;
                long tstamp = 0L;
                for (HashMap<String, Object> lockMember : lockMembers) {
                    if (++lockMemberNum > 1) {
                        // здесь по-хорошему нужно делать клон logRecord, чтобы записи лога были
                        // разными объектами, но, в целях производительности, этого не будем делать;
                        // считаем, что logRecord, передаваемый далее в filterAndStoreRecord, неизменяемый
                        // в рамках обработки, и требуется только для чтения из него данных (свойств);
                        // здесь заменим в текущем logRecord только некоторые свойства (timestamp, locks)
                        //
                        if (lockMemberNum == 2) {
                            tstamp = logRecord.timestamp;
                        }
                        logRecord.put(TIMESTAMP_PROP_NAME, String.valueOf(++tstamp));
                    }
                    // из-за того, что в парсере предусмотрена возможность не заполнять данные блокировок
                    // при том, что знания о количестве данных блокировок влияют на вычисляемые реквизиты
                    // исходящих данных, необходимо где-то помнить признак - встречались ли в записи лога
                    // данные блокировок или нет; для этого предусмотрено свойство пространства блокировок
                    // LOCK_SPACE_RECORDS_COUNT_PROP_NAME - оно вычисляется всегда, даже если не заполняются
                    // данные блокировок в свойстве пространства LOCK_SPACE_RECORDS_PROP_NAME
                    //
                    ArrayList<?> lockMemberRecords = (ArrayList<?>) lockMember.get(LOCK_SPACE_RECORDS_PROP_NAME);
                    int lockMemberRecordsCount = (int) lockMember.get(LOCK_SPACE_RECORDS_COUNT_PROP_NAME);
                    boolean tableGranularity = logRecord.escalating || lockMemberRecordsCount == 0;

                    // тут для гранулярности делаем допущение: для случая, когда нет эскалации блокировки,
                    // и при этом нет никаких условий на поля пространства блокировки, то может показаться,
                    // что гранулярность будет "таблица" (явно блокируются все записи), а не "запись"; 
                    // но в случае, когда пользователем блокируются все записи в пределах разделителя 
                    // (если он установлен в данных), то фактически гранулярность будет "запись", так как
                    // будут блокировная все записи с указанным разделителем, а не вообще все записи таблицы;
                    // поэтму гранулярность "таблица" ("table") всегда точная, а гранулярность "запись" ("field")
                    // может быть не всегда точной; нужно знать - имеется ли разделитель в блокируемой
                    // таблице или нет, чтобы всегда точно знать - "field" или "table" в случае отсутствия
                    // условий в пространстве блокировок (это на будущее)
                    //
                    lockMember.put(LOCK_GRANULARITY_PROP_NAME, tableGranularity ? "table" : "field");

                    lockMember.put(LOCK_SPACE_RECORDS_PROP_NAME + ".hash()", String.valueOf(lockMemberRecords.hashCode()));
                    logRecord.put(LOCKS_PROP_NAME, lockMember);
                    result = filterAndStoreRecord(logRecord) && result; // здесь && result должен быть в конце!
                }
            }
            else {
                logRecord.put(LOCKS_PROP_NAME, "");
                result = filterAndStoreRecord(logRecord);
            }
            return result;
        }
        else {
            return filterAndStoreRecord(logRecord);
        }
    }

    
    public boolean filterAndStoreRecord(OneCTJRecord logRecord) {
        try {
            if (filter == null || filter.accept(logRecord)) {
                filteredCount++;
                if (addFields != null) {
                    logRecord.putAll(addFields);
                }
                recordsStorage.put(logRecord);
            }
            if (delay > 0) {
                Thread.sleep(delay);
            }
        }
        catch (Exception ex) {
            OneCTJRecord message = new OneCTJRecord();
            message.put("LOGSERIALIZEERROR", ex.getMessage());
            try {
                recordsStorage.put(message);
            }
            catch (Exception ex1) {
                if (exception == null) exception = ex1;
            }
        }
        return filteredCount < maxCount;
    }

    
    private boolean buildRecord() throws IOException {
        kvrc.lr.clear();
        kvrc.lr.timestamp = timestamp;
        
        String smmss = sminutes + sseconds;
        kvrc.lr.put(DATE_TIME_PROP_NAME, yyyyMMddhh + smmss);
        kvrc.lr.put(ONLY_TIME_PROP_NAME, shours + smmss);
        
        kvrc.lr.put(TIMESTAMP_PROP_NAME, String.valueOf(timestamp));
        kvrc.lr.put(DURATION_PROP_NAME, String.valueOf(duration));
        kvrc.lr.put(START_DATE_TIME_PROP_NAME, DATE_FORMAT.format(new Date((timestamp - MICROSECONDS_TO_1970 - duration) / 1000L)));
        
        kvrc.lr.put(EVENT_PROP_NAME, eventName);
        kvrc.lr.put(EVENT_HASH_PROP_NAME, String.valueOf(eventName.hashCode())); // TODO: надо?
        
        kvrc.lr.put(LEVEL_PROP_NAME, eventLevel);
        
        kvrc.bytesRead = filePos;
        kvrc.isReadyToStore = false;
        kvrc.lr.escalating = false;
        kvrc.lr.containsLocks = false;
        
        kvrc.context = null;
        kvrc.processName = null;
        kvrc.computerName = null;
        kvrc.usr = null;
        
        long fp = stream.getFilePointer();
        for (byte kv = 0; kv <= kvcc; kv++) {
            KeyValueBounds kvi = kvrc.kv[kv];

            int kl = (int)(kvi.ke - kvi.kb);
            stream.seek(kvi.kb);
            String k = stream.readString(kl, UTF8);

            if (kvi.vb > 0) {
                if (!kvi.isLocks) {
                    int vl = (int)(kvi.ve - kvi.vb);
                    stream.seek(kvi.vb);
                    String v = stream.readString(vl, UTF8);
                    kvrc.lr.put(k, v);
                    kvrc.lr.put(k + ".hash()", String.valueOf(v.hashCode()));
                    if (kvi.isContext) kvrc.context = v;
                    else if (kvi.isProcessName) kvrc.processName = v;
                    else if (kvi.isComputerName) kvrc.computerName = v;
                    else if (kvi.isUsr) kvrc.usr = v;
                    else if (kvi.isEscalating) kvrc.lr.escalating = true;
                    if (DEBUG_RECORDS) { System.out.println(k + "=" + v); }                
                }
                else {
                    // для значения ключа Locks
                    ArrayList<HashMap<String, Object>> lckm = new ArrayList<>();
                    HashMap<String, Object> lelm;
                    Lock lock = kvrc.lck;
                    for (int j = 0; j < lock.lec; j++) {
                        LockElement lel = lock.lel.get(j);
                        lelm = new HashMap<>();
                        ArrayList<HashMap<String, String>> rl = new ArrayList<>();  // lock element records list
                        HashMap<String, String> rm = null;                          // lock element record
                        String rk = null;                                           // lock element record key
                        int rlvl = -1;                                              // lock element record row level
                        lelm.put(LOCK_SPACE_RECORDS_PROP_NAME, rl);
                        for (int i = 0; i < lel.tc; i++) {
                            Token lelt = lel.letl.get(i);
                            int vl = (int)(lelt.te - lelt.tb);
                            stream.seek(lelt.tb);
                            String v = stream.readString(vl, UTF8);
                            switch (lelt.tk) {
                                case TOKEN_KIND_LOCK_SPACE:
                                    lelm.put(LOCK_SPACE_NAME_PROP_NAME, v);
                                    break;
                                case TOKEN_KIND_LOCK_TYPE:
                                    lelm.put(LOCK_SPACE_TYPE_PROP_NAME, v);
                                    break;
                                case TOKEN_KIND_LOCK_KEY:
                                    if (rlvl != lelt.tl) {
                                        rlvl = lelt.tl;
                                        rm = new HashMap<>();
                                        rl.add(rm);
                                    }
                                    rk = v;
                                    break;
                                case TOKEN_KIND_LOCK_VALUE:
                                    assert rm != null;
                                    rm.put(rk, v);
                                    break;
                            }
                        }
                        lelm.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, rl.size());
                        lckm.add(lelm);
                    }
                    kvrc.lr.containsLocks = true;
                    kvrc.lr.put(k, lckm);
                    if (DEBUG_RECORDS) { System.out.println(k + "=" + lckm); }
                }
            }
            else {
                kvrc.lr.put(k, null);
                if (DEBUG_RECORDS) { System.out.println(k); }
            }

        }
        stream.seek(fp);
        
        // обрабатывается ситуация, когда контекст события выделен в отдельную запись,
        // следующую за записью события (у такой записи event == Context); будем считать,
        // что контекст из отдельной записи относится к предыдущей записи события, если
        // в обеих записях совпадают поля p:processName, t:computerName, Usr
        //
        if (!kvrp.isContext) {
            if (kvrc.isContext) {
                if (kvrc.context != null
                        && (kvrp.context == null || kvrp.context.isEmpty())
                        && (kvrc.processName != null ? kvrc.processName.equals(kvrp.processName) : true)
                        && (kvrc.computerName != null ? kvrc.computerName.equals(kvrp.computerName) : true)
                        && (kvrc.usr != null ? kvrc.usr.equals(kvrp.usr) : true)) {
                    kvrp.lr.put("Context", kvrc.lr.get("Context"));
                    kvrp.lr.put("Context.hash()", kvrc.lr.get("Context.hash()"));
                    kvrp.lr.put("ContextLastLine", kvrc.lr.get("ContextLastLine"));
                    kvrp.lr.put("ContextLastLine.hash()", kvrc.lr.get("ContextLastLine.hash()"));
                    kvrp.bytesRead = kvrc.bytesRead;
                    kvrp.isReadyToStore = true;
                }
            }
            else {
                kvrp.isReadyToStore = true;
            }
        }
        
        // разбор журнала будет продолжен только если количество отфильтрованных записей
        // не больше заданного количества; фильтрацию делам для предыдущей записи, так
        // как пессимистично может быть такое, что текущая запись - Context предыдущей
        //
        boolean continueParsing = true;
        if (kvrp.isReadyToStore) {
            continueParsing = onLogRecord(kvrp.lr);
        }
        
        kvcc = -1;
        
        if (kvrp.isReadyToStore || !kvrc.isContext) {
            if (kvsh) {
                kvsh = false;
                kvrc = kvr0;
                kvrp = kvr1;
            }
            else {
                kvsh = true;
                kvrc = kvr1;
                kvrp = kvr0;
            }
        }
        
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
        
        kvrp.bytesRead = fromPosition;
        kvrc.bytesRead = fromPosition;
        
        String fileName = state.getFile().getName();
        boolean isTJName = fileName.matches("\\d{8}.*\\.log");
        
        try (BufferedRandomAccessFileStream rafs = new BufferedRandomAccessFileStream(
                state.getOpenedRandomAccessFile(),
                STREAM_BUFFER_SIZE)) {
            stream = rafs;
            stream.seek(fromPosition);
            filePos = fromPosition;
            read(stream,
                    isTJName ? Integer.parseInt(fileName.substring(0, 2)) + 2000 : 1970,
                    isTJName ? Integer.parseInt(fileName.substring(2, 4)) : 0,
                    isTJName ? Integer.parseInt(fileName.substring(4, 6)) : 1,
                    isTJName ? Integer.parseInt(fileName.substring(6, 8)) : 0,
                    parameters);
            // в конце (finally) будет rafs.close()
        }
        catch (ParseException ex) {
            // ошибкой будем считать любое исключение, возникшее при чтении лог-файла, за исключением
            // случая чтения записи лога в процессе её записи, когда она записана не полностью
            //
            String message = ex.getMessage();
            if (message != null 
                    && !message.contains("Encountered: <EOF> after :")
                    && !message.contains("Encountered \"<EOF>\" at")) {
                /*
                String parserErrorLog = makeParserErrorsLogDir(parameters);
                File errorFragmentFile = new File(String.format("%s/%s.%s.%s.parse_error", 
                        parserErrorLog,
                        state.getFile().getName(),
                        fromPosition + kvrp.bytesRead,
                        fromPosition + super.getBytesRead() + 256));
                copyFileFragment(state.getFile(), 
                        fromPosition + kvrp.bytesRead,
                        fromPosition + super.getBytesRead() + 256,
                        errorFragmentFile);
                */
                exception = ex;
                throw ex;
            }
        }
        catch (Exception ex) {
            exception = ex;
            throw ex;
        }
    }


    public void read(BufferedRandomAccessFileStream stream, int year, int month, int day, int hour,
            ParserParameters parameters) throws IOException, ParseException {
        
        syear = right("0000" + year, 4);
        smonth = right("00" + month, 2);
        sday = right("00" + day, 2);
        shours = right("00" + hour, 2);
        yyyyMMddhh = syear + smonth + sday + shours;

        Calendar calendar = new GregorianCalendar(TIME_ZONE);
        calendar.set(year, month - 1, day, hour, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        microsecondsBase = calendar.getTimeInMillis() * 1000L + MICROSECONDS_TO_1970;

        unfilteredCount = 0;
        filteredCount = 0;
        delay = parameters == null ? 0 : parameters.getDelay();

        ContainsMatcher matcher;
        if (true) {
            matcher = new ContainsMatcher("57:18.039118-1,DBMSSQL,5,p:processName=Торговля_АА");
        }
        
        fileLinesRead = 0;
        
        kvcc = -1;
        mode = MODE_TIMESTAMP;
        
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
                        System.out.println("" + new String(b3, UTF8) + " (" + icc + ", " + ic2 + ", " + ic3 + ") [" + mode + "]");
                        break;
                    case 4:
                        b4[0] = icc;
                        b4[1] = ic2;
                        b4[2] = ic3;
                        b4[3] = ic4;
                        System.out.println("" + new String(b4, UTF8) + " (" + icc + ", " + ic2 + ", " + ic3 + ", " + ic4 + ") [" + mode + "]");
                }
            }

            if (matcher != null && matcher.checkNext(icc, ic2, ic3, ic4, bytesInSym)) System.out.println("matched at " + (filePos - matcher.length()));

            switch (icc) {
                case CARRIAGE_RETURN:
                    break;
                case NEW_LINE:
                    switch (mode) {
                        case MODE_KEY:
                            mode = MODE_RECORD_TERMINATE;
                            // зафиксировать конец ключа
                            kvrc.kv[kvcc].ke = firstBytePos - 1;
                            break;
                        case MODE_VALUE:
                            mode = MODE_RECORD_TERMINATE;
                            // зафиксировать конец значения
                            kvrc.kv[kvcc].ve = firstBytePos - 1;
                            // зафиксировать конец последнего токена занчения Locks
                            if (valueMode == VALUE_MODE_LOCKS) { lckelt.te = firstBytePos - 1; }
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                        case MODE_VALUE_INSIDE_APOSTROPHE:
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        case MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED:
                        case MODE_VALUE_IA_COMMA_OR_APO_EXPECTED:
                            mode = MODE_RECORD_TERMINATE;
                            // зафиксировать конец значения (-2)
                            kvrc.kv[kvcc].ve = firstBytePos - 2;
                            // зафиксировать конец последнего токена занчения Locks
                            if (valueMode == VALUE_MODE_LOCKS) { lckelt.te = firstBytePos - 2; }
                            break;
                        default:
                    }
                    fileLinesRead++;
                    break;
                case COMMA:
                    switch (mode) {
                        case MODE_DURATION:
                            // TODO: зафиксировать конец продолжительности
                            mode = MODE_EVENT_EXPECTED;
                            break;
                        case MODE_EVENT:
                            // TODO: зафиксировать конец имени события
                            mode = MODE_EVENT_LEVEL_EXPECTED;
                            break;
                        case MODE_EVENT_LEVEL:
                            // TODO: зафиксировать конец уровня события
                            mode = MODE_KEY_EXPECTED;
                            break;
                        case MODE_VALUE_EXPECTED:
                            mode = MODE_KEY_EXPECTED;
                            // зафиксировать конец значения (оно пустое)
                            kvrc.kv[kvcc].ve = firstBytePos;
                            break;
                        case MODE_VALUE:
                            mode = MODE_KEY_EXPECTED;
                            // зафиксировать конец значения
                            kvrc.kv[kvcc].ve = firstBytePos;
                            // зафиксировать конец последнего токена занчения Locks
                            if (valueMode == VALUE_MODE_LOCKS) { lckelt.te = firstBytePos; }
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                        case MODE_VALUE_INSIDE_APOSTROPHE:
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        case MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED:
                        case MODE_VALUE_IA_COMMA_OR_APO_EXPECTED:
                            mode = MODE_KEY_EXPECTED;
                            // зафиксировать конец значения (-1)
                            kvrc.kv[kvcc].ve = firstBytePos - 1;
                            // зафиксировать конец последнего токена занчения Locks
                            if (valueMode == VALUE_MODE_LOCKS) { lckelt.te = firstBytePos - 1; }
                            break;
                        default:
                            throw new ParseException("wrong comma appearing");
                    }
                    break;
                case HYPHEN:
                    switch (mode) {
                        case MODE_TIMESTAMP:
                            // TODO: зафиксировать конец момента времени события
                            mode = MODE_DURATION_EXPECTED;
                            break;
                        case MODE_EVENT:
                        case MODE_KEY:
                            break;
                        case MODE_VALUE:
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                        case MODE_VALUE_INSIDE_APOSTROPHE:
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        default:
                            throw new ParseException("wrong hyphen appearing");
                    }
                    break;
                case EQUALS:
                    switch (mode) {
                        case MODE_KEY:
                            mode = MODE_VALUE_EXPECTED;
                            // зафиксировать конец ключа
                            kvrc.kv[kvcc].ke = firstBytePos;
                            break;
                        case MODE_EVENT:
                            break;
                        case MODE_VALUE:
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                        case MODE_VALUE_INSIDE_APOSTROPHE:
                            // если это символ "=" в значении ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        default:
                            throw new ParseException("wrong equals appearing");
                    }
                    break;
                case QUOTATION_MARK:
                    switch (mode) {
                        case MODE_VALUE_EXPECTED:
                            mode = MODE_VALUE_INSIDE_QUOTATION_MARK;
                            // зафиксировать начало значения (+1)
                            kvrc.kv[kvcc].vb = firstBytePos + 1;
                            // если ключ для этого значения равен "Locks", начинается парсинг блокировок
                            if (kvrc.kv[kvcc].isLocks = locksMatcher.matches()) { // здесь присвоение, а не равенство!
                                // фиксируем начало значения ключа "Locks"
                                valueMode = VALUE_MODE_LOCKS;
                                // инициализируем данные блокировки и переходим
                                // в режим ожидания токена пространства блокировки
                                lck = kvrc.lck.reinit();
                                inLockMode = IN_LOCK_MODE_SPACENAME_EXPECTED;
                            }
                            else {
                                valueMode = VALUE_MODE_GENERAL;
                            }
                            break;
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { 
                                locksValueRead();
                            }
                            else {
                                mode = MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED;
                            }
                            break;
                        case MODE_VALUE_IQM_COMMA_OR_QM_EXPECTED:
                            // это двойная кавычка внутри значения в кавычках - продолжаем чтение значения
                            mode = MODE_VALUE_INSIDE_QUOTATION_MARK;
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        case MODE_VALUE:
                        case MODE_VALUE_INSIDE_APOSTROPHE:
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        default:
                            throw new ParseException("wrong quotation mark appear");
                    }
                    break;
                case APOSTROPHE:
                    switch (mode) {
                        case MODE_VALUE_EXPECTED:
                            mode = MODE_VALUE_INSIDE_APOSTROPHE;
                            // зафиксировать начало значения (+1)
                            kvrc.kv[kvcc].vb = firstBytePos + 1;
                            // если ключ для этого значения равен "Locks", начинается парсинг блокировок
                            if (kvrc.kv[kvcc].isLocks = locksMatcher.matches()) { // здесь присвоение, а не равенство!
                                // фиксируем начало значения ключа "Locks"
                                valueMode = VALUE_MODE_LOCKS;
                                // инициализируем данные блокировки и переходим
                                // в режим ожидания токена пространства блокировки
                                lck = kvrc.lck.reinit();
                                inLockMode = IN_LOCK_MODE_SPACENAME_EXPECTED;
                            }
                            else {
                                valueMode = VALUE_MODE_GENERAL;
                            }
                            break;
                        case MODE_VALUE:
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        case MODE_VALUE_INSIDE_APOSTROPHE:
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { 
                                locksValueRead();
                            }
                            else {
                                mode = MODE_VALUE_IA_COMMA_OR_APO_EXPECTED;
                            }
                            break;
                        case MODE_VALUE_IA_COMMA_OR_APO_EXPECTED:
                            // это двойной апостроф внутри значения в апострофах - продолжаем чтение значения
                            mode = MODE_VALUE_INSIDE_APOSTROPHE;
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        default:
                            throw new ParseException("wrong apostrophe appear");
                    }
                    break;
                default: // любой другой однобайтный символ (в т.ч. EOF) или символ из двух и более байтов
                    switch (mode) {
                        case MODE_RECORD_TERMINATE:
                            // зафиксировать запись в коллекции; сюда попадём
                            // только если встретился перевод строки, но не EOF
                            if (!buildRecord()) {
                                icc = EOF;
                                break;
                            }
                            mode = MODE_TIMESTAMP;
                            mm = ss = ms = tssymbols = 0;
                            break;
                        case MODE_TIMESTAMP:
                            tssymbols++;
                            switch (tssymbols) {
                                case 1:
                                    sc.reset();
                                    sc.addByte(icc);
                                    mm = icc - CHR0;
                                case 2:
                                    sc.addByte(icc);
                                    sminutes = sc.toString();
                                    mm = mm * 10 + icc - CHR0;
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    sc.reset();
                                    sc.addByte(icc);
                                    ss = icc - CHR0;
                                case 5:
                                    sc.addByte(icc);
                                    sseconds = sc.toString();
                                    ss = ss * 10 + icc - CHR0;
                                    break;
                                case 6:
                                    sc.reset();
                                    break;
                                default:
                                    sc.addByte(icc);
                                    ms = ms * 10 + icc - CHR0;
                            }
                            break;
                        case MODE_DURATION_EXPECTED:
                            // обработаем милисекунды от MODE_TIMESTAMP;
                            // в логах технологического журнала платформы 8.2 длительность события указана в бОльших
                            // единицах времени, чем в логах платформы 8.3, поэтому в таком случае приведём значение
                            // к формату для платформы 8.3 (x100)
                            //
                            v82format = tssymbols < 12; // 00:00.000000
                            if (v82format) {
                                ms = ms * 100;
                            }
                            timestamp = microsecondsBase + mm * 60 + ss * 1000000L + ms;
                            // начнём обработку продолжительности события
                            mode = MODE_DURATION;
                            duration = 0;
                            // здесь break не нужен
                        case MODE_DURATION:
                            duration = duration * 10 + icc - CHR0;
                            break;
                        case MODE_EVENT_EXPECTED:
                            // обработаем продолжительность события от MODE_DURATION;
                            // в логах технологического журнала платформы 8.2 длительность события указана в бОльших
                            // единицах времени, чем в логах платформы 8.3, поэтому в таком случае приведём значение
                            // к формату для платформы 8.3 (x100)
                            //
                            if (v82format) {
                                duration = duration * 100;
                            }
                            // начнём обработку имени события
                            mode = MODE_EVENT;
                            sc.reset();
                            contextEventMatcher.newSearch();
                            // здесь break не нужен
                        
                        case MODE_EVENT:
                            sc.addByte(icc);
                            contextEventMatcher.checkNext(icc, ic2, ic3, ic4, bytesInSym);
                            break;
                        
                        case MODE_EVENT_LEVEL_EXPECTED:
                            // обработаем имя события от MODE_EVENT
                            eventName = sc.toString();
                            kvrc.isContext = contextEventMatcher.matches();
                            // начнём обработку уровня события
                            mode = MODE_EVENT_LEVEL;
                            eventLevel = 0;
                            // здесь break не нужен
                        
                        case MODE_EVENT_LEVEL:
                            eventLevel = eventLevel * 10 + icc - CHR0;
                            break;
                        
                        case MODE_KEY_EXPECTED:
                            mode = MODE_KEY;
                            // зафиксировать начало ключа
                            kvcc++;
                            kvrc.kv[kvcc].kb = firstBytePos;
                            kvrc.kv[kvcc].vb = 0;
                            // организовать проверку на равенство ключей значениям из списка
                            locksMatcher.newSearch();
                            contextMatcher.newSearch();
                            processNameMatcher.newSearch();
                            computerNameMatcher.newSearch();
                            usrMatcher.newSearch();
                            escalatingMatcher.newSearch();
                            // здесь break не нужен
                        case MODE_KEY:
                            locksMatcher.checkNext(icc, ic2, ic3, ic4, bytesInSym);
                            contextMatcher.checkNext(icc, ic2, ic3, ic4, bytesInSym);
                            processNameMatcher.checkNext(icc, ic2, ic3, ic4, bytesInSym);
                            computerNameMatcher.checkNext(icc, ic2, ic3, ic4, bytesInSym);
                            usrMatcher.checkNext(icc, ic2, ic3, ic4, bytesInSym);
                            escalatingMatcher.checkNext(icc, ic2, ic3, ic4, bytesInSym);
                            break;
                        case MODE_VALUE_EXPECTED:
                            mode = MODE_VALUE;
                            // обработка проверки ключа на равенство значению из списка
                            kvrc.kv[kvcc].isContext = contextMatcher.matches();
                            kvrc.kv[kvcc].isProcessName = processNameMatcher.matches();
                            kvrc.kv[kvcc].isComputerName = computerNameMatcher.matches();
                            kvrc.kv[kvcc].isUsr = usrMatcher.matches();
                            kvrc.kv[kvcc].isEscalating = escalatingMatcher.matches();
                            // зафиксировать начало значения
                            kvrc.kv[kvcc].vb = firstBytePos;
                            // если ключ для этого значения равен "Locks", начинается парсинг блокировок
                            if (kvrc.kv[kvcc].isLocks = locksMatcher.matches()) { // здесь присвоение, а не равенство!
                                // фиксируем начало значения ключа "Locks"
                                valueMode = VALUE_MODE_LOCKS;
                                // инициализируем данные блокировки и переходим
                                // в режим ожидания токена пространства блокировки
                                lck = kvrc.lck.reinit();
                                inLockMode = IN_LOCK_MODE_SPACENAME_EXPECTED;
                            }
                            else {
                                valueMode = VALUE_MODE_GENERAL;
                            }
                            break;
                        case MODE_VALUE:
                        case MODE_VALUE_INSIDE_QUOTATION_MARK:
                        case MODE_VALUE_INSIDE_APOSTROPHE:
                            // если это значение ключа "Locks", то парсим значение
                            if (valueMode == VALUE_MODE_LOCKS) { locksValueRead(); }
                            break;
                        default:
                            throw new ParseException("wrong symbol appearing");
                    }
            }
            
            
            
            if (filePos % 10240000 == 0) { // 10Mb
                System.out.println("" + filePos + "    " 
                        + stream.getBackSeekCount() + "    "
                        + recordsStorage.size() + "    "
                );
            }
            
        }
        while (icc != EOF);

        // на этот момент текущая прочитання запись лога (kvrc) стала предыдущей (kvrp),
        // и если она ещё не записана в хранилище и не является записью с event == Context,
        // то запишем её в хранилище (если там есть место)
        //
        if (!kvrp.isReadyToStore && !kvrp.isContext) {
            onLogRecord(kvrp.lr);
        }

        stream.close();
        
        System.out.println("total records count = " + unfilteredCount);
        System.out.println("filtered records count = " + filteredCount);
    }
    
    
    private void locksValueRead() throws ParseException {
        
        // InfoRg43101.DIMS Exclusive Fld43102=308:b90d0050569db98a11ec9947003dff6f Fld43103=63834609629554
        // 'InfoRg43101.DIMS Exclusive Fld43102=308:b90d0050569db98a11ec9947003dff6f Fld43103=63834609629554 Fld43104=2721719 Fld43105=T"20231103120000", Fld43102=308:b90d0050569db98a11ec9947003dff70 Fld43103=63834609629523 Fld43104=2721719 Fld43105=T"20231103120000", Fld43102=308:b90d0050569db98a11ec9947003dff71 Fld43103=63834609619665 Fld43104=2721719 Fld43105=T"20231103120000", Fld43102=308:b90d0050569db98a11ec9947003dff72 Fld43103=63834609619665 Fld43104=2721719 Fld43105=T"20231103120000", Fld43102=308:b90d0050569db98a11ec9947003dff73 Fld43103=63834609619665 Fld43104=2721719 Fld43105=T"20231103120000"'
        // 'InfoRg81385X1.DIMS Exclusive Fld81386="b157092d-b4fc-11ec-a746-005056b38f20" Fld81389="out/ReplyClient_v2/651184" Fld81390=302:00000000000000000000000000000000 Fld81391="" Fld81393="cb2e2dbc-9d20-427d-8057-bbc12f2d42fa" Fld81394=302:00000000000000000000000000000000 Fld82196="010060704142"'
        // 'Reference87.REFLOCK Exclusive Fld2734=0 ID=87:bc0a0050569d9ae211ee79453630d271,Reference87.REFLOCK Exclusive Fld2734=0 ID=87:bc0a0050569d9ae211ee79453630d271'
        // Locks="Reference605X1.REFLOCK Exclusive Description=""Тест''''''''''123""""""""""" ("Тест''''''''''123""""")
        
        switch (icc) {
            case NEW_LINE:
                switch (inLockMode) {
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                        break;
                    case IN_LOCK_MODE_LOCK_VALUE:
                    case IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED:
                    case IN_LOCK_MODE_LOCK_VALUE_IA_SPACE_OR_APO_EXPECTED:
                        // зафиксировать конец текущего токена (-1 \r)
                        lckelt = lckel.addToken(tokenBegin, firstBytePos - 1, TOKEN_KIND_LOCK_VALUE, tokenRowLevel);
                        inLockMode = IN_LOCK_MODE_RECORD_TERMINATE;
                        break;
                    case IN_LOCK_MODE_SECOND_QUOTATION_MARK_EXPECTED:
                        if (mode == MODE_VALUE_INSIDE_QUOTATION_MARK) {
                            mode = MODE_RECORD_TERMINATE;
                            // зафиксировать конец текущего токена (-1 \r)
                            lckelt = lckel.addToken(tokenBegin, firstBytePos - 1, TOKEN_KIND_LOCK_VALUE, tokenRowLevel);
                            inLockMode = IN_LOCK_MODE_RECORD_TERMINATE;
                            break;
                        }
                        throw new ParseException("wrong line break appearing");
                    case IN_LOCK_MODE_SECOND_APOSTROPHE_EXPECTED:
                        if (mode == MODE_VALUE_INSIDE_APOSTROPHE) {
                            mode = MODE_RECORD_TERMINATE;
                            // зафиксировать конец текущего токена (-1 \r)
                            lckelt = lckel.addToken(tokenBegin, firstBytePos - 1, TOKEN_KIND_LOCK_VALUE, tokenRowLevel);
                            inLockMode = IN_LOCK_MODE_RECORD_TERMINATE;
                            break;
                        }
                        throw new ParseException("wrong line break appearing");
                    default:
                        throw new ParseException("wrong line break appearing");
                } 
                break;
            case COMMA:
                switch (inLockMode) {
                    case IN_LOCK_MODE_LOCK_VALUE:
                    case IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED:
                    case IN_LOCK_MODE_LOCK_VALUE_IA_SPACE_OR_APO_EXPECTED:
                        // на самом деле после запятой может следовать или новое пространство блокировок,
                        // или новая запись в наборе блокируемых значений; по умолчанию будем предполагать,
                        // что будет новое пространство блокировок, а если поймём, что ошиблись, изменим
                        // ветку парсинга (см. здесь же ветку с EQUALS); фиксируем завершение токена
                        //
                        lckelt = lckel.addToken(tokenBegin, firstBytePos, TOKEN_KIND_LOCK_VALUE, tokenRowLevel);
                        tokenRowLevel++;
                        inLockMode = IN_LOCK_MODE_SPACENAME_EXPECTED;
                        break;
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                        break;
                    case IN_LOCK_MODE_SECOND_QUOTATION_MARK_EXPECTED:
                        if (mode == MODE_VALUE_INSIDE_QUOTATION_MARK) {
                            mode = MODE_KEY_EXPECTED;
                            // зафиксировать конец текущего токена (-1 \r)
                            lckelt = lckel.addToken(tokenBegin, firstBytePos - 1, TOKEN_KIND_LOCK_VALUE, tokenRowLevel);
                            inLockMode = IN_LOCK_MODE_RECORD_TERMINATE;
                            break;
                        }
                        throw new ParseException("wrong comma appearing");
                    case IN_LOCK_MODE_SECOND_APOSTROPHE_EXPECTED:
                        if (mode == MODE_VALUE_INSIDE_APOSTROPHE) {
                            mode = MODE_KEY_EXPECTED;
                            // зафиксировать конец текущего токена (-1 \r)
                            lckelt = lckel.addToken(tokenBegin, firstBytePos - 1, TOKEN_KIND_LOCK_VALUE, tokenRowLevel);
                            inLockMode = IN_LOCK_MODE_RECORD_TERMINATE;
                            break;
                        }
                        throw new ParseException("wrong comma appearing");
                    default:
                        throw new ParseException("wrong comma appearing");
                }
                break;
            case EQUALS:
                switch (inLockMode) {
                    case IN_LOCK_MODE_SPACENAME:
                        // в случае (inLockMode == IN_LOCK_MODE_SPACENAME) в этом месте файла
                        // ожидали имя пространства блокировок, а наткнулись на очередную запись
                        // в наборе блокируемых значений элемента блокировки - "превращаем"
                        // токен имени пространства блокировок в токен ключа записи и удем собирать
                        // новую последовательность пар ключ-значение; фиксируем завершение токена
                        //
                        inLockMode = IN_LOCK_MODE_LOCK_KEY;
                        // break здесь не нужен!
                    case IN_LOCK_MODE_LOCK_KEY:
                        lckelt = lckel.addToken(tokenBegin, firstBytePos, TOKEN_KIND_LOCK_KEY, tokenRowLevel);
                        inLockMode = IN_LOCK_MODE_LOCK_VALUE_EXPECTED;
                        break;
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                        break;
                    default:
                        throw new ParseException("wrong equals appearing");
                }
                break;
            case SPACE:
                switch (inLockMode) {
                    case IN_LOCK_MODE_SPACENAME:
                        lckel = lck.addLockElement();
                        tokenRowLevel = 0;
                        // фиксируем завершение первого токена элемента блокировки
                        lckelt = lckel.addToken(tokenBegin, firstBytePos, TOKEN_KIND_LOCK_SPACE, tokenRowLevel);
                        inLockMode = IN_LOCK_MODE_LOCK_TYPE_EXPECTED;
                        break;
                    case IN_LOCK_MODE_LOCK_TYPE:
                        // фиксируем завершение токена
                        lckelt = lckel.addToken(tokenBegin, firstBytePos, TOKEN_KIND_LOCK_TYPE, tokenRowLevel);
                        inLockMode = IN_LOCK_MODE_LOCK_KEY_EXPECTED;
                        break;
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                    case IN_LOCK_MODE_SPACENAME_EXPECTED:
                    case IN_LOCK_MODE_LOCK_TYPE_EXPECTED:
                    case IN_LOCK_MODE_LOCK_KEY_EXPECTED:
                    case IN_LOCK_MODE_LOCK_VALUE_EXPECTED:
                        break;
                    case IN_LOCK_MODE_LOCK_VALUE:
                    case IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED:
                    case IN_LOCK_MODE_LOCK_VALUE_IA_SPACE_OR_APO_EXPECTED:
                        // фиксируем завершение токена
                        lckelt = lckel.addToken(tokenBegin, firstBytePos, TOKEN_KIND_LOCK_VALUE, tokenRowLevel);
                        inLockMode = IN_LOCK_MODE_LOCK_KEY_EXPECTED;
                        break;
                    default:
                        // в противном случае пробел - это разделитель токенов
                }
                break;
            case QUOTATION_MARK:
                if (mode == MODE_VALUE_INSIDE_QUOTATION_MARK) {
                    // внутри глобального значения, ограниченного кавычками, кавычки экранируются
                    // ещё одной кавычкой, поэтому любая кавычка, встреченная здесь впервые, требует
                    // проверки на пару - не экранирует ли она следующую
                    //
                    switch (inLockMode) {
                        case IN_LOCK_MODE_LOCK_VALUE_EXPECTED:
                            // зафиксировать начало токена значения
                            tokenBegin = firstBytePos + 2; // + кавычка + будущая экранирующая кавычка
                            inLockModeBackup = inLockMode;
                            inLockModeNext = IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK;
                            inLockMode = IN_LOCK_MODE_SECOND_QUOTATION_MARK_EXPECTED;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                            inLockModeBackup = inLockMode;
                            inLockModeNext = IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED;
                            inLockMode = IN_LOCK_MODE_SECOND_QUOTATION_MARK_EXPECTED;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED:
                            // это двойная кавычка внутри значения в кавычках - продолжаем чтение значения
                            inLockModeBackup = inLockMode;
                            inLockModeNext = IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK;
                            inLockMode = IN_LOCK_MODE_SECOND_QUOTATION_MARK_EXPECTED;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE: // Fld43122=T"20231103120000"
                        case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                            inLockModeBackup = inLockMode;
                            inLockModeNext = inLockMode;
                            inLockMode = IN_LOCK_MODE_SECOND_QUOTATION_MARK_EXPECTED;
                            break;
                        case IN_LOCK_MODE_SECOND_QUOTATION_MARK_EXPECTED:
                            inLockMode = inLockModeNext;
                            break;
                        default:
                            throw new ParseException("wrong quotation mark appear");
                    }
                }
                else {
                    switch (inLockMode) {
                        case IN_LOCK_MODE_LOCK_VALUE_EXPECTED:
                            // зафиксировать начало токена значения
                            tokenBegin = firstBytePos + 1; // + кавычка
                            inLockMode = IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                            inLockMode = IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED:
                            // это двойная кавычка внутри значения в кавычках - продолжаем чтение значения
                            inLockMode = IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE: // Fld43122=T"20231103120000"
                        case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                            break;
                        default:
                            throw new ParseException("wrong quotation mark appear");
                    }
                }
                break;
            case APOSTROPHE:
                if (mode == MODE_VALUE_INSIDE_APOSTROPHE) {
                    // внутри глобального значения, ограниченного апострофами, апострофы экранируются
                    // ещё одним апострофом, поэтому любой апостроф, встреченный здесь впервые, требует
                    // проверки на пару - не экранирует ли он следующий апостроф
                    //
                    switch (inLockMode) {
                        case IN_LOCK_MODE_LOCK_VALUE_EXPECTED:
                            // зафиксировать начало токена значения
                            tokenBegin = firstBytePos + 2; // + апостроф + будущий экранирующий апостороф
                            inLockModeBackup = inLockMode;
                            inLockModeNext = IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE;
                            inLockMode = IN_LOCK_MODE_SECOND_APOSTROPHE_EXPECTED;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE: // Fld43122=T'20231103120000'
                        case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                            inLockModeBackup = inLockMode;
                            inLockModeNext = inLockMode;
                            inLockMode = IN_LOCK_MODE_SECOND_APOSTROPHE_EXPECTED;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_IA_SPACE_OR_APO_EXPECTED:
                            // это апостроф внутри значения в апострофах - продолжаем чтение значения
                            inLockModeBackup = inLockMode;
                            inLockModeNext = IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE;
                            inLockMode = IN_LOCK_MODE_SECOND_APOSTROPHE_EXPECTED;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                            inLockModeBackup = inLockMode;
                            inLockModeNext = IN_LOCK_MODE_LOCK_VALUE_IA_SPACE_OR_APO_EXPECTED;
                            inLockMode = IN_LOCK_MODE_SECOND_APOSTROPHE_EXPECTED;
                            break;
                        case IN_LOCK_MODE_SECOND_APOSTROPHE_EXPECTED:
                            inLockMode = inLockModeNext;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_IQM_SPACE_OR_QM_EXPECTED:
                            mode = mode = MODE_VALUE_IA_COMMA_OR_APO_EXPECTED;
                            // зафиксировать конец текущего токена (-1 \r)
                            lckelt = lckel.addToken(tokenBegin, firstBytePos - 1, TOKEN_KIND_LOCK_VALUE, tokenRowLevel);
                            inLockMode = IN_LOCK_MODE_RECORD_TERMINATE;
                            break;
                        default:
                            throw new ParseException("wrong apostrophe appear");
                    }
                }
                else {
                    switch (inLockMode) {
                        case IN_LOCK_MODE_LOCK_VALUE_EXPECTED:
                            // зафиксировать начало токена значения
                            tokenBegin = firstBytePos + 1; // + апостроф
                            inLockMode = IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE: // Fld43122=T'20231103120000'
                        case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_IA_SPACE_OR_APO_EXPECTED:
                            // это апостроф внутри значения в апострофах - продолжаем чтение значения
                            inLockMode = IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE;
                            break;
                        case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                            inLockMode = IN_LOCK_MODE_LOCK_VALUE_IA_SPACE_OR_APO_EXPECTED;
                            break;
                        default:
                            throw new ParseException("wrong apostrophe appear");
                    }
                }
                break;
            default: // любой другой однобайтный символ (в т.ч. EOF) или символ из двух и более байтов
                switch (inLockMode) {
                    case IN_LOCK_MODE_RECORD_TERMINATE:
                        // зафиксировать конец текущего токена
                        valueMode = VALUE_MODE_GENERAL;
                        break;
                    case IN_LOCK_MODE_SPACENAME_EXPECTED:
                        // зафиксировать начало токена имени пространства блокировки
                        tokenBegin = firstBytePos;
                        inLockMode = IN_LOCK_MODE_SPACENAME;
                        break;
                    case IN_LOCK_MODE_LOCK_TYPE_EXPECTED:
                        // зафиксировать начало токена типа блокировки
                        tokenBegin = firstBytePos;
                        inLockMode = IN_LOCK_MODE_LOCK_TYPE;
                        break;
                    case IN_LOCK_MODE_LOCK_KEY_EXPECTED:
                        // зафиксировать начало токена ключа элемента блокировки
                        tokenBegin = firstBytePos;
                        inLockMode = IN_LOCK_MODE_LOCK_KEY;
                        break;
                    case IN_LOCK_MODE_LOCK_VALUE_EXPECTED:
                        // зафиксировать начало токена значения элемента блокировки
                        tokenBegin = firstBytePos;
                        inLockMode = IN_LOCK_MODE_LOCK_VALUE;
                        break;
                    case IN_LOCK_MODE_SPACENAME:
                    case IN_LOCK_MODE_LOCK_TYPE:
                    case IN_LOCK_MODE_LOCK_KEY:
                    case IN_LOCK_MODE_LOCK_VALUE:
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_QUOTATION_MARK:
                    case IN_LOCK_MODE_LOCK_VALUE_INSIDE_APOSTROPHE:
                        break;
                    default:
                        throw new ParseException("wrong symbol appearing");
                }
        }
        
    }
    
    
    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) throws IOException, InterruptedException {
        ParserRecordsStorage recordsStorage = new ParserNullStorage(); // new ParserListStorage();
        FastTJParser parser = new FastTJParser();
        parser.setRecordsStorage(recordsStorage);
        
        List<File> sources = new ArrayList<>();
        
//      sources = filesFromDirectory("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L70");
//      sources.add(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L72\\MonitorLogs\\Торговля_АА_1541\\Queries\\rphost_31828\\23103119.log"));
//      sources.add(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L72\\MonitorLogs\\Торговля_АА_1541\\SQL_Locks\\rphost_58776\\23103119.log")); // 2Gb
//      sources.add(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\24011700.log")); // Locks
//      sources.add(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\23120112.log")); // Transactions
//      sources.add(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L71\\23021816.log")); // Locks
        sources.add(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L70\\ERP_prod_1541\\1C_Locks\\rphost_5456\\23110314.log")); // Locks 648 M
//      sources.add(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L70\\ERP_prod_1541\\1C_Locks\\rphost_5456\\23110312.log")); // Locks 1 G
//      sources.add(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\test.txt"));

        String filterJson = "{'or' : "
                + "[ "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11880' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443433' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11881' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443434' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11882' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443435' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11883' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443433' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11884' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443434' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11885' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443435' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11886' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443436' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11887' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443437' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11888' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443438' } "
                + "    ]"
                + "}, "
                + "{ 'and' : "
                + "    [ "
                + "    { 'property' : 'duration', 'operation' : 'n>=', 'pattern' : '3' }, "
                + "    { 'property' : 'OSThread', 'pattern' : '11889' }, "
                + "    { 'property' : 't:connectID', 'pattern' : '443439' } "
                + "    ]"
                + "} "
                + "]}";
        Filter filter = Filter.fromJson(filterJson);
        
        filter = null;

        long duration = 0;
        long TESTS_COUNT = 1; // 100;
        
        for (int i = 0; i < TESTS_COUNT; i++) {
            
            for (File file : sources) {
                FileState state = new FileState(file);
                state.setPointer(0);
                System.out.println("file " + file.getAbsolutePath());
                long m = new Date().getTime();
                try {
                    parser.parse(state, "UTF-8", state.getPointer(), /* 999999999 */ 1000 * 10000000, filter, null);
                }
                catch (ParseException | TokenMgrError ex) {
                    System.out.println("!!! parse error: " + ex.getMessage());
                }
                duration = duration + (new Date().getTime() - m);
                System.out.println("found rows: " + recordsStorage.size());
                System.out.println(" file size: " + file.length());
                System.out.println("bytes read: " + (parser.getFilePos() - 1));
                if (parser.getFilePos() - 1 != file.length()) {
                    System.out.println("      ====: ^");
                }
                recordsStorage.clear();
            }
            
            Thread.sleep(1000);
            System.out.println(String.format(
                    "intermediate avg test time after %d tests: %f sec\n",
                    i + 1,
                    (duration / (i + 1))/ 1000.0f));
        
        }
        
        System.out.println("avg test time: " + ((duration / TESTS_COUNT)/ 1000.0f) + " sec");

    }

}
