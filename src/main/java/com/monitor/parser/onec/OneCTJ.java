package com.monitor.parser.onec;

/*
 * Copyright 2021 Aleksei Andreev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
import com.monitor.parser.Token;
import com.monitor.parser.ParseException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OneCTJ implements OneCTJConstants {

    private final static long MICROSECONDS_TO_1970 = 62135596800000L * 1000L;
    private final static byte[] ZEROES = "0000000".getBytes();
    private final static byte CHR0 = '0';
    private final static String SQL_PARAMETERS_PROP_MARKER = "\np_0:";
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private final static TimeZone TIME_ZONE = TimeZone.getTimeZone("GMT+0");

    private final static String SQL_PARAMETERS_PROP_NAME = "Sql.p_N";
    private final static String DATE_TIME_PROP_NAME = "dateTime";
    private final static String START_DATE_TIME_PROP_NAME = "startDateTime";
    private final static String ONLY_TIME_PROP_NAME = "onlyTime";
    private final static String CONTEXT_LAST_LINE_PROP_NAME = "ContextLastLine";
    private final static String TIMESTAMP_PROP_NAME = "timestamp";

    private final static String LOCKS_PROP_NAME = "Locks";
    private final static String LOCK_SPACE_NAME_PROP_NAME = "space";
    private final static String LOCK_SPACE_TYPE_PROP_NAME = "type";
    private final static String LOCK_SPACE_RECORDS_PROP_NAME = "records";
    private final static String LOCK_SPACE_RECORDS_COUNT_PROP_NAME = "recordsCount";
    private final static String LOCK_GRANULARITY_PROP_NAME = "granularity";

    private static final Pattern UNPRINTABLE_PATTERN = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]");

    static {
        DATE_FORMAT.setTimeZone(TIME_ZONE);
    }
    
    private final OneCTJRecord logRecord0 = new OneCTJRecord();
    private final OneCTJRecord logRecord1 = new OneCTJRecord();
    private OneCTJRecord logRecord = logRecord1;
    private OneCTJRecord readyLogRecord = logRecord0;
    private long bytesRead;
    private long readyBytesRead;

    private final byte[] ms6 = new byte[6];
    private String propertyName;
    private String propertyValue;
    protected long recordsCount = 0L;
    private long perfomance = 0L;
    private boolean v82format;
    private String event;
    private String year;
    private String month;
    private String day;
    private String hours;
    private String minutes;
    private String seconds;
    private String microseconds;
    private String yyyyMMddhh;
    private long microsecondsBase;
    private long microsecondsValue;
    private String sqlParametersPropValue;
    private String lastLineParameterValue;

    private ArrayList<HashMap<String, Object>> lockSpaces;  // список наборов свойств пространств блокировки 
    private HashMap<String, Object> lockSpaceProps;         // набор свойств текущего пространства блокировок
    private ArrayList<HashMap<String, String>> lockRecords; // список записей блокировок (их свойств) для текущего пространства
    private int lockRecordsCount;                           // количество записей блокировок (их свойств) для текущего пространства
    private HashMap<String, String> lockRecordProps;        // набор свойств текущей записи блокировки
    private boolean includeLockRecords;                     // нужно ли собирать данные полей блокировки

    public static void main(String args[]) throws Throwable {
        /*
        final OneCTJ parser = new OneCTJ();
        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\20092816.DBMSSQL.log"), "UTF-8");
        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\21112514.DBMSSQL.log"), "UTF-8");
        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\21120914.EXCP.log"), "UTF-8");
        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\21121011.SDBL.log"), "UTF-8");
        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\21121011.TDEADLOCK.log"), "UTF-8");
//      System.out.println("perfomance: " + parser.getPerfomance());
//      System.out.println("records: " + parser.getRecordsCount());
         */
        
        System.out.println("test = " + DATE_FORMAT.format(new Date((63827362557274001L - MICROSECONDS_TO_1970 - 999983) / 1000L)));

        final OneCTJ parser = new OneCTJ();
        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L70\\00000001.log"), "UTF-8");
        parser.onParseEnd();
//        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L70\\23040608_0.log"), "UTF-8");
//        parser.onParseEnd();
//        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L70\\23040608_0.log"), "UTF-8");
//        parser.onParseEnd();
        System.out.println("perfomance: " + parser.getPerfomance());
        System.out.println("records: " + parser.getRecordsCount());
        System.out.println("bytes read: " + parser.getBytesRead());
    }

    public void parse(InputStream inputStream, String encoding, int year, int month, int day, int hour,
            Map<String, Object> parameters) throws ParseException {

        ReInit(inputStream, encoding);

        this.year = right("0000" + year, 4);
        this.month = right("00" + month, 2);
        this.day = right("00" + day, 2);
        this.hours = right("00" + hour, 2);
        yyyyMMddhh = this.year + this.month + this.day + this.hours;
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TIME_ZONE);
        calendar.set(year, month - 1, day, hour, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        microsecondsBase = calendar.getTimeInMillis() * 1000L + MICROSECONDS_TO_1970;

        includeLockRecords = parameters == null
                || !((Set) parameters.getOrDefault("exclude-data", new HashSet<>())).contains("lock-fields");

        logRecord.clear();
        readyLogRecord.clear();
        bytesRead = 0;
        readyBytesRead = 0;
        recordsCount = 0;
        perfomance = 0;
        v82format = false;
        long m1 = System.currentTimeMillis();
        onParseBegin();

        LogRecords();

        perfomance = System.currentTimeMillis() - m1;
        onParseEnd();

    }

    public void parse(File file, String encoding) throws FileNotFoundException, IOException, ParseException {
        FileInputStream inputStream = new FileInputStream(file);

        String fileName = file.getName();
        boolean isTJName = fileName.matches("\\d{8}.*\\.log");

        parse(inputStream, encoding,
                isTJName ? Integer.parseInt(fileName.substring(0, 2)) + 2000 : 1970,
                isTJName ? Integer.parseInt(fileName.substring(2, 4)) : 0,
                isTJName ? Integer.parseInt(fileName.substring(4, 6)) : 1,
                isTJName ? Integer.parseInt(fileName.substring(6, 8)) : 0,
                null);
    }

    public final long getPerfomance() {
        return perfomance;
    }

    public final long getRecordsCount() {
        return recordsCount;
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

    private boolean onLogRecordInternal(OneCTJRecord logRecord) {
        recordsCount++;
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
                long timestamp = 0L;
                for (HashMap<String, Object> lockMember : lockMembers) {
                    if (++lockMemberNum > 1) {
                        // здесь по-хорошему нужно делать клон logRecord, чтобы записи лога были
                        // разными объектами, но, в целях производительности, этого не будем делать;
                        // считаем, что logRecord, передаваемый далее в onLogRecord, неизменяемый
                        // в рамках обработки, и требуется только для чтения из него данных (свойств);
                        // здесь заменим в текущем logRecord только некоторые свойства (timestamp, locks)
                        //
                        if (lockMemberNum == 2) {
                            timestamp = Long.parseLong((String) logRecord.get(TIMESTAMP_PROP_NAME));
                        }
                        logRecord.put(TIMESTAMP_PROP_NAME, String.valueOf(++timestamp));
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
                    result = onLogRecord(logRecord) && result; // здесь && result должен быть в конце!
                }
            }
            else {
                logRecord.put(LOCKS_PROP_NAME, "");
                result = onLogRecord(logRecord);
            }
            return result;
        }
        else {
            return onLogRecord(logRecord);
        }
    }

    public boolean onLogRecord(OneCTJRecord logRecord) {
        return true;
    }

    public void onParseBegin() {
    }

    public void onParseEnd() {
        if (jj_input_stream != null) {
            jj_input_stream.Done();
            jj_input_stream = null;
        }
        token_source = new OneCTJTokenManager(jj_input_stream);
        token = null;
        logRecord0.clear();
        logRecord1.clear();
        propertyValue = "";
        
        /*
        if (lockSpaces != null) lockSpaces.clear();
        if (lockSpaceProps != null) lockSpaceProps.clear();
        if (lockRecords != null) lockRecords.clear();
        if (lockRecordProps != null) lockRecordProps.clear();
        if (lockSpaces != null) lockSpaces.clear();
        */
        lockSpaceProps = null;
        lockRecords = null;
        lockRecordProps = null;
        lockSpaces = null;
        
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = null;
        }
    }

    public OneCTJ() {
        this((java.io.InputStream) null, "UTF-8");
    }

    public OneCTJ(java.io.InputStream stream, String encoding) {
        if (stream != null) {
            try {
                jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1);
            }
            catch (java.io.UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            jj_input_stream = null;
        }
        token_source = new OneCTJTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 46; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public void ReInit(java.io.InputStream stream, String encoding) {
        try {
            if (jj_input_stream == null) {
                jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1);
            }
            else {
                jj_input_stream.ReInit(stream, encoding, 1, 1);
            }
        }
        catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 46; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    private String deQuoted(String str) {
        return str.substring(
                str.charAt(1) == '\r'
                ? (str.charAt(2) == '\n' ? 3 : 2)
                : (str.charAt(1) == '\n' ? 2 : 1),
                str.length() - (str.charAt(str.length() - 2) == '\n' ? 2 : 1));
    }

    private String deQuotedChecked(String str) {
        char quotTest = str.length() == 0 ? ' ' : str.charAt(0);
        return (quotTest == '\'' || quotTest == '"') ? deQuoted(str) : str;
    }

    private String cutParameters(String str) {
        int markerPos = str.indexOf(SQL_PARAMETERS_PROP_MARKER);
        if (markerPos >= 0) {
            propertyValue = str.substring(
                    str.charAt(1) == '\n' ? 2 : 1,
                    markerPos);
            sqlParametersPropValue = str.substring(
                    markerPos + 1,
                    str.length() - (str.charAt(str.length() - 2) == '\n' ? 2 : 1));
            logRecord.put(SQL_PARAMETERS_PROP_NAME, sqlParametersPropValue);
            logRecord.put(SQL_PARAMETERS_PROP_NAME + ".hash()", String.valueOf(sqlParametersPropValue.hashCode()));
        }
        else {
            propertyValue = deQuotedChecked(str);
        }
        return propertyValue;
    }

    private String right(String str, int count) {
        return str.substring(str.length() - count);
    }

    private String strip(String value) {
        byte[] btvalue = value.getBytes();
        int left = indexOfNonWhitespace(btvalue);
        if (left == btvalue.length) {
            return "";
        }
        int right = lastIndexOfNonWhitespace(btvalue);
        return ((left > 0) || (right < btvalue.length)) ? new String(btvalue, left, right - left) : value;
    }

    private int indexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int left = 0;
        while (left < length) {
            char ch = (char) (value[left] & 0xff);
            if (ch != ' ' && ch != '\t' && !Character.isWhitespace(ch)) {
                break;
            }
            left++;
        }
        return left;
    }

    private int lastIndexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int right = length;
        while (0 < right) {
            char ch = (char) (value[right - 1] & 0xff);
            if (ch != ' ' && ch != '\t' && !Character.isWhitespace(ch)) {
                break;
            }
            right--;
        }
        return right;
    }

    protected String lastLine(String str) {
        int markerPos = str.length();
        while (true) {
            while (--markerPos >= 0 && str.charAt(markerPos) == '\n') {
            }
            if (markerPos < 0) {
                return "";
            }
            int last = markerPos;
            markerPos = str.lastIndexOf('\n', last) + 1;
            String result = strip(str.substring(markerPos, last + 1));
            if (!result.isEmpty()) {
                return result;
            }
        }
    }

    protected String accentLastLine(String str) {
        propertyValue = deQuotedChecked(str);
        lastLineParameterValue = lastLine(propertyValue);
        logRecord.put(CONTEXT_LAST_LINE_PROP_NAME, lastLineParameterValue);
        logRecord.put(CONTEXT_LAST_LINE_PROP_NAME + ".hash()", String.valueOf(lastLineParameterValue.hashCode()));
        return propertyValue;
    }

    protected String startDateTime(long timestamp, String duration) {
        return DATE_FORMAT.format(new Date((timestamp - MICROSECONDS_TO_1970 - Long.parseLong(duration)) / 1000L));
    }

    private String[] uniqueArray(String str) {
        if (!str.contains(",")) {
            String[] result = new String[1];
            result[0] = str;
            return result;
        }
        String[] splitted = str.replaceAll("['\"]", "").split(",");
        HashSet<String> set = new HashSet(splitted.length);
        for (String s : splitted) {
            set.add(s);
        }
        if (set.size() == splitted.length) {
            return splitted;
        }
        String[] result = new String[set.size()];
        set.toArray(result);
        return result;
    }
    
    private String getRidOfUnprintables(String str) {
        Matcher matcher = UNPRINTABLE_PATTERN.matcher(str);
        return matcher.replaceAll("?");
    }

    public long getReadyBytesRead() {
        return readyBytesRead;
    }

    public long getBytesRead() {
        return token == null ? 0 : token.bytesRead;
    }

    final public void LogRecords() throws ParseException {
        boolean getMoreRecords = true;
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case DL_EOL:
            case DL_BOM: {
                label_1:
                while (true) {
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case DL_BOM: {
                            jj_consume_token(DL_BOM);
                            break;
                        }
                        case DL_EOL: {
                            jj_consume_token(DL_EOL);
                            break;
                        }
                        default:
                            jj_la1[0] = jj_gen;
                            jj_consume_token(-1);
                            throw new ParseException();
                    }
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case DL_EOL:
                        case DL_BOM: {
                            ;
                            break;
                        }
                        default:
                            jj_la1[1] = jj_gen;
                            break label_1;
                    }
                }
                break;
            }
            default:
                jj_la1[2] = jj_gen;
                ;
        }
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case DL_TIMESTAMP: {
                getMoreRecords = SequenceOfLogRecords();
                break;
            }
            default:
                jj_la1[3] = jj_gen;
                ;
        }
        if (getMoreRecords) {
            jj_consume_token(0);
        }
    }

    final public boolean SequenceOfLogRecords() throws ParseException {
        boolean getMoreRecords = false;
        label_2:
        while (true) {
            getMoreRecords = LogRecord();
            if (!getMoreRecords) {
                break;
            }
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case DL_EOL:
                case DL_BOM: {
                    label_3:
                    while (true) {
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case DL_BOM: {
                                jj_consume_token(DL_BOM);
                                break;
                            }
                            case DL_EOL: {
                                jj_consume_token(DL_EOL);
                                break;
                            }
                            default:
                                jj_la1[4] = jj_gen;
                                jj_consume_token(-1);
                                throw new ParseException();
                        }
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case DL_EOL:
                            case DL_BOM: {
                                ;
                                break;
                            }
                            default:
                                jj_la1[5] = jj_gen;
                                break label_3;
                        }
                    }
                    break;
                }
                default:
                    jj_la1[6] = jj_gen;
                    ;
            }
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case DL_TIMESTAMP: {
                    ;
                    break;
                }
                default:
                    jj_la1[7] = jj_gen;
                    break label_2;
            }
        }
        if (!logRecord.isEmpty() && getMoreRecords) {
            readyBytesRead = bytesRead;
            getMoreRecords = onLogRecordInternal(logRecord);
        }
        return getMoreRecords;
    }

    final public boolean LogRecord() throws ParseException {
        boolean result = true;
        readyBytesRead = bytesRead;
        readyLogRecord = logRecord;
        logRecord = logRecord == logRecord0 ? logRecord1 : logRecord0;
        logRecord.clear();

        TimeStamp();

        // в логах технологического журнала платформы 8.2 длительность события указана в бОльших
        // единицах времени, поэтому в таком случае приведём значение к формату для платформы 8.3 (x100)
        jj_consume_token(69);
        jj_consume_token(DL_INTEGER);
        String duration = v82format ? token.image + "00" : token.image;
        logRecord.put("duration", duration);
        logRecord.put(START_DATE_TIME_PROP_NAME, startDateTime(microsecondsValue, duration));

        jj_consume_token(DL_COMMA);
        EventName();
        logRecord.put("event", event = token.image);
        logRecord.put("event.hash()", String.valueOf(token.image.hashCode()));

        jj_consume_token(EDL_COMMA);
        jj_consume_token(EDL_INTEGER);
        logRecord.put("level", token.image);

        jj_consume_token(EDL_COMMA);
        label_4:
        while (true) {
            KeyValuePair();
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case EDL_COMMA: {
                    jj_consume_token(EDL_COMMA);
                    break;
                }
                case VL_COMMA: {
                    jj_consume_token(VL_COMMA);
                    break;
                }
                case LLP_COMMA: {
                    jj_consume_token(LLP_COMMA);
                    break;
                }
                case LLP_EOL: {
                    jj_consume_token(LLP_EOL);
                    break;
                }
                case EDL_EOL: {
                    jj_consume_token(EDL_EOL);
                    break;
                }
                case VL_EOL: {
                    jj_consume_token(VL_EOL);
                    break;
                }
                case 0: {
                    jj_consume_token(0);
                    break;
                }
                default:
                    jj_la1[8] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
            }
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case EDP_CONTEXT:
                case EDP_ESCALATING:
                case EDP_LKSRC:
                case EDP_LOCKS:
                case EDP_SQL:
                case EDP_WAIT_CONNECTIONS:
                case EDP_ANY_OTHER: {
                    ;
                    break;
                }
                default:
                    jj_la1[9] = jj_gen;
                    break label_4;
            }
        }
        bytesRead = token.bytesRead;
        if ("Context".equals(event)) {
            if (!readyLogRecord.isEmpty()
                    && logRecord.containsKey("Context")
                    && (!readyLogRecord.containsKey("Context") || readyLogRecord.get("Context").toString().isEmpty())
                    && (logRecord.containsKey("p:processName") ? logRecord.get("p:processName").equals(readyLogRecord.get("p:processName")) : true)
                    && (logRecord.containsKey("t:computerName") ? logRecord.get("t:computerName").equals(readyLogRecord.get("t:computerName")) : true)
                    && (logRecord.containsKey("Usr") ? logRecord.get("Usr").equals(readyLogRecord.get("Usr")) : true)) {
                readyLogRecord.put("Context", logRecord.get("Context"));
                readyLogRecord.put("Context.hash()", logRecord.get("Context.hash()"));
                readyLogRecord.put("ContextLastLine", logRecord.get("ContextLastLine"));
                readyLogRecord.put("ContextLastLine.hash()", logRecord.get("ContextLastLine.hash()"));
            }
            readyBytesRead = bytesRead;
            logRecord.clear();
        }
        if (!readyLogRecord.isEmpty()) {
            result = onLogRecordInternal(readyLogRecord);
        }
        return result;
    }

    final public void TimeStamp() throws ParseException {
        jj_consume_token(DL_TIMESTAMP);
        minutes = token.image.substring(0, 2);
        seconds = token.image.substring(3, 5);
        microseconds = token.image.substring(6);
        v82format = microseconds.length() < 6;
        if (v82format) {
            System.arraycopy(microseconds.getBytes(), 0, ms6, 0, microseconds.length());
            System.arraycopy(ZEROES, 0, ms6, microseconds.length(), 6 - microseconds.length());
            microseconds = new String(ms6);
        }
        /*
        logRecord.put("dyy", year);
        logRecord.put("dmm", month);
        logRecord.put("ddd", day);
        logRecord.put("thh", hours);
        logRecord.put("tmm", minutes);
        logRecord.put("tss", seconds);
        logRecord.put("tms", microseconds);
         */
        logRecord.put(DATE_TIME_PROP_NAME, yyyyMMddhh + minutes + seconds);
        logRecord.put(ONLY_TIME_PROP_NAME, hours + minutes + seconds);
        microsecondsValue
                = microsecondsBase
                + (((minutes.charAt(0) - CHR0) * 10 + minutes.charAt(1) - CHR0) * 60
                + (seconds.charAt(0) - CHR0) * 10 + seconds.charAt(1) - CHR0) * 1000000L
                + ((microseconds.charAt(0) - CHR0) * 100000
                + (microseconds.charAt(1) - CHR0) * 10000
                + (microseconds.charAt(2) - CHR0) * 1000
                + (microseconds.charAt(3) - CHR0) * 100
                + (microseconds.charAt(4) - CHR0) * 10
                + (microseconds.charAt(5) - CHR0));
        logRecord.put(TIMESTAMP_PROP_NAME, String.valueOf(microsecondsValue));
    }

    final public void EventName() throws ParseException {
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case DE_CONTEXT: {
                jj_consume_token(DE_CONTEXT);
                break;
            }
            case DE_DB2: {
                jj_consume_token(DE_DB2);
                break;
            }
            case DE_DBMSSQL: {
                jj_consume_token(DE_DBMSSQL);
                break;
            }
            case DE_DBORACLE: {
                jj_consume_token(DE_DBORACLE);
                break;
            }
            case DE_DBPOSTGRS: {
                jj_consume_token(DE_DBPOSTGRS);
                break;
            }
            case DE_DBV8DB_ENG: {
                jj_consume_token(DE_DBV8DB_ENG);
                break;
            }
            case DE_EXCP: {
                jj_consume_token(DE_EXCP);
                break;
            }
            case DE_SCALL: {
                jj_consume_token(DE_SCALL);
                break;
            }
            case DE_SDBL: {
                jj_consume_token(DE_SDBL);
                break;
            }
            case DE_TDEADLOCK: {
                jj_consume_token(DE_TDEADLOCK);
                break;
            }
            case DE_TLOCK: {
                jj_consume_token(DE_TLOCK);
                break;
            }
            case DE_ANY_OTHER: {
                jj_consume_token(DE_ANY_OTHER);
                break;
            }
            default:
                jj_la1[10] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
    }

    final public StringBuilder LockSpace() throws ParseException {
        StringBuilder spaceName = new StringBuilder();
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case LLP_IDENTIFIER: {
                jj_consume_token(LLP_IDENTIFIER);
                spaceName.append(token.image);
                label_5:
                while (true) {
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case LLP_DOT: {
                            ;
                            break;
                        }
                        default:
                            jj_la1[11] = jj_gen;
                            break label_5;
                    }
                    jj_consume_token(LLP_DOT);
                    spaceName.append(token.image);
                    jj_consume_token(LLP_IDENTIFIER);
                    spaceName.append(token.image);
                }
                break;
            }
            case LLSQP_IDENTIFIER: {
                jj_consume_token(LLSQP_IDENTIFIER);
                spaceName.append(token.image);
                label_6:
                while (true) {
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case LLSQP_DOT: {
                            ;
                            break;
                        }
                        default:
                            jj_la1[12] = jj_gen;
                            break label_6;
                    }
                    jj_consume_token(LLSQP_DOT);
                    spaceName.append(token.image);
                    jj_consume_token(LLSQP_IDENTIFIER);
                    spaceName.append(token.image);
                }
                break;
            }
            case LLDQP_IDENTIFIER: {
                jj_consume_token(LLDQP_IDENTIFIER);
                spaceName.append(token.image);
                label_7:
                while (true) {
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case LLDQP_DOT: {
                            ;
                            break;
                        }
                        default:
                            jj_la1[13] = jj_gen;
                            break label_7;
                    }
                    jj_consume_token(LLDQP_DOT);
                    spaceName.append(token.image);
                    jj_consume_token(LLDQP_IDENTIFIER);
                    spaceName.append(token.image);
                }
                break;
            }
            default:
                jj_la1[14] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        return spaceName;
    }

    final public String LockComplexSpace() throws ParseException {
        StringBuilder spaceName;
        spaceName = LockSpace();
        label_8:
        while (true) {
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case LLP_COMMA:
                case LLSQP_COMMA:
                case LLDQP_COMMA: {
                    ;
                    break;
                }
                default:
                    jj_la1[15] = jj_gen;
                    break label_8;
            }
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case LLP_COMMA: {
                    jj_consume_token(LLP_COMMA);
                    break;
                }
                case LLSQP_COMMA: {
                    jj_consume_token(LLSQP_COMMA);
                    break;
                }
                case LLDQP_COMMA: {
                    jj_consume_token(LLDQP_COMMA);
                    break;
                }
                default:
                    jj_la1[16] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
            }
            spaceName.append(token.image);
            LockSpace();
            spaceName.append(token.image);
        }
        return spaceName.toString();
    }

    final public String LockType() throws ParseException {
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case LLP_EXCLUSIVE: {
                jj_consume_token(LLP_EXCLUSIVE);
                break;
            }
            case LLP_SHARED: {
                jj_consume_token(LLP_SHARED);
                break;
            }
            case LLSQP_EXCLUSIVE: {
                jj_consume_token(LLSQP_EXCLUSIVE);
                break;
            }
            case LLSQP_SHARED: {
                jj_consume_token(LLSQP_SHARED);
                break;
            }
            case LLDQP_EXCLUSIVE: {
                jj_consume_token(LLDQP_EXCLUSIVE);
                break;
            }
            case LLDQP_SHARED: {
                jj_consume_token(LLDQP_SHARED);
                break;
            }
            default:
                jj_la1[17] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        return token.image;
    }

    final public void LockProperties() throws ParseException {
        if (includeLockRecords) {
            lockRecordProps = new HashMap();
            lockRecords.add(lockRecordProps);
        }
        lockRecordsCount++;
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case LLP_IDENTIFIER: {
                label_9:
                while (true) {
                    LockKeyValuePair();
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case LLP_IDENTIFIER: {
                            ;
                            break;
                        }
                        default:
                            jj_la1[18] = jj_gen;
                            break label_9;
                    }
                }
                break;
            }
            case LLSQP_IDENTIFIER: {
                LockSQPKeyValuePair();
                label_10:
                while (true) {
                    if (jj_2_1(3)) {
                        ;
                    }
                    else {
                        break label_10;
                    }
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case LLSQP_COMMA: {
                            jj_consume_token(LLSQP_COMMA);
                            if (includeLockRecords) {
                                lockRecordProps = new HashMap();
                                lockRecords.add(lockRecordProps);
                            }
                            lockRecordsCount++;
                            break;
                        }
                        default:
                            jj_la1[19] = jj_gen;
                            ;
                    }
                    LockSQPKeyValuePair();
                }
                break;
            }
            case LLDQP_IDENTIFIER: {
                LockDQPKeyValuePair();
                label_11:
                while (true) {
                    if (jj_2_2(3)) {
                        ;
                    }
                    else {
                        break label_11;
                    }
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case LLDQP_COMMA: {
                            jj_consume_token(LLDQP_COMMA);
                            if (includeLockRecords) {
                                lockRecordProps = new HashMap();
                                lockRecords.add(lockRecordProps);
                            }
                            lockRecordsCount++;
                            break;
                        }
                        default:
                            jj_la1[20] = jj_gen;
                            ;
                    }
                    LockDQPKeyValuePair();
                }
                break;
            }
            default:
                jj_la1[21] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
    }

    final public void LockKeyValuePair() throws ParseException {
        jj_consume_token(LLP_IDENTIFIER);
        String key = token.image;
        jj_consume_token(LLP_EQUALS);
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case LLP_STRING: {
                jj_consume_token(LLP_STRING);
                break;
            }
            case LLP_STRING_SEQUENCE: {
                jj_consume_token(LLP_STRING_SEQUENCE);
                break;
            }
            default:
                jj_la1[22] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        if (includeLockRecords) {
            lockRecordProps.put(key, token.image);
        }
    }

    final public void LockSQPKeyValuePair() throws ParseException {
        jj_consume_token(LLSQP_IDENTIFIER);
        String key = token.image;
        jj_consume_token(LLSQP_EQUALS);
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case LLSQP_STRING: {
                jj_consume_token(LLSQP_STRING);
                break;
            }
            case LLSQP_STRING_SEQUENCE: {
                jj_consume_token(LLSQP_STRING_SEQUENCE);
                break;
            }
            default:
                jj_la1[23] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        if (includeLockRecords) {
            lockRecordProps.put(key, token.image);
        }
    }

    final public void LockDQPKeyValuePair() throws ParseException {
        jj_consume_token(LLDQP_IDENTIFIER);
        String key = token.image;
        jj_consume_token(LLDQP_EQUALS);
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case LLDQP_STRING: {
                jj_consume_token(LLDQP_STRING);
                break;
            }
            case LLDQP_STRING_SEQUENCE: {
                jj_consume_token(LLDQP_STRING_SEQUENCE);
                break;
            }
            default:
                jj_la1[24] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        if (includeLockRecords) {
            lockRecordProps.put(key, token.image);
        }
    }

    final public void KeyValuePair() throws ParseException {
        propertyValue = "";
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case EDP_LKSRC:
            case EDP_WAIT_CONNECTIONS: {
				/*
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case EDP_LKSRC: {
                        jj_consume_token(EDP_LKSRC);
                        break;
                    }
                    case EDP_WAIT_CONNECTIONS: {
                        jj_consume_token(EDP_WAIT_CONNECTIONS);
                        break;
                    }
                    default:
                        jj_la1[25] = jj_gen;
                        jj_consume_token(-1);
                        throw new ParseException();
                }
                */
                jj_consume_token(jj_ntk);
                propertyName = token.image;
                jj_consume_token(EDL_EQUALS);
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case VL_INTEGER:
                    case VL_STRING:
                    case VL_STRING_SEQUENCE: {
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case VL_INTEGER: {
                                jj_consume_token(VL_INTEGER);
                                propertyValue = token.image;
                                break;
                            }
                            case VL_STRING: {
                                jj_consume_token(VL_STRING);
                                propertyValue = deQuoted(token.image);
                                break;
                            }
                            case VL_STRING_SEQUENCE: {
                                jj_consume_token(VL_STRING_SEQUENCE);
                                propertyValue = token.image;
                                break;
                            }
                            default:
                                jj_la1[26] = jj_gen;
                                jj_consume_token(-1);
                                throw new ParseException();
                        }
                        break;
                    }
                    default:
                        jj_la1[27] = jj_gen;
                        ;
                }
                logRecord.put(propertyName, uniqueArray(propertyValue));
                break;
            }
            case EDP_CONTEXT: {
                jj_consume_token(EDP_CONTEXT);
                propertyName = token.image;
                jj_consume_token(EDL_EQUALS);
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case VL_INTEGER:
                    case VL_STRING:
                    case VL_STRING_SEQUENCE: {
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case VL_INTEGER: {
                                jj_consume_token(VL_INTEGER);
                                propertyValue = token.image;
                                break;
                            }
                            case VL_STRING: {
                                jj_consume_token(VL_STRING);
                                propertyValue = accentLastLine(token.image);
                                break;
                            }
                            case VL_STRING_SEQUENCE: {
                                jj_consume_token(VL_STRING_SEQUENCE);
                                propertyValue = accentLastLine(token.image);
                                break;
                            }
                            default:
                                jj_la1[28] = jj_gen;
                                jj_consume_token(-1);
                                throw new ParseException();
                        }
                        break;
                    }
                    default:
                        jj_la1[29] = jj_gen;
                        ;
                }
                logRecord.put(propertyName, propertyValue);
                logRecord.put(propertyName + ".hash()", String.valueOf(propertyValue.hashCode()));
                break;
            }
            case EDP_SQL: {
                jj_consume_token(EDP_SQL);
                propertyName = token.image;
                jj_consume_token(EDL_EQUALS);
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case VL_INTEGER:
                    case VL_STRING:
                    case VL_STRING_SEQUENCE: {
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case VL_INTEGER: {
                                jj_consume_token(VL_INTEGER);
                                propertyValue = token.image;
                                break;
                            }
                            case VL_STRING: {
                                jj_consume_token(VL_STRING);
                                propertyValue = cutParameters(token.image).replaceAll("#tt\\d+", "#tt");
                                break;
                            }
                            case VL_STRING_SEQUENCE: {
                                jj_consume_token(VL_STRING_SEQUENCE);
                                propertyValue = cutParameters(token.image).replaceAll("#tt\\d+", "#tt");
                                break;
                            }
                            default:
                                jj_la1[30] = jj_gen;
                                jj_consume_token(-1);
                                throw new ParseException();
                        }
                        break;
                    }
                    default:
                        jj_la1[31] = jj_gen;
                        ;
                }
                logRecord.put(propertyName, propertyValue);
                logRecord.put(propertyName + ".hash()", String.valueOf(propertyValue.hashCode()));
                break;
            }
            case EDP_LOCKS: {
                lockSpaces = new ArrayList();
                jj_consume_token(EDP_LOCKS);
                propertyName = token.image;
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case LL_EQUALS: {
                        jj_consume_token(LL_EQUALS);
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case LLP_IDENTIFIER:
                            case LLSQP_IDENTIFIER:
                            case LLDQP_IDENTIFIER: {
                                lockRecords = new ArrayList();
                                lockRecordsCount = 0;
                                lockSpaceProps = new HashMap(4);
                                lockSpaceProps.put(LOCK_SPACE_NAME_PROP_NAME, LockComplexSpace());
                                lockSpaceProps.put(LOCK_SPACE_TYPE_PROP_NAME, LockType());
                                lockSpaceProps.put(LOCK_SPACE_RECORDS_PROP_NAME, lockRecords);
                                lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                                lockSpaces.add(lockSpaceProps);
                                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                                    case LLP_IDENTIFIER:
                                    case LLSQP_IDENTIFIER:
                                    case LLDQP_IDENTIFIER: {
                                        LockProperties(); // заполняются lockRecords, lockRecordsCount
                                        lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                                        break;
                                    }
                                    default:
                                        jj_la1[32] = jj_gen;
                                        ;
                                }
                                break;
                            }
                            default:
                                jj_la1[33] = jj_gen;
                                ;
                        }
                        break;
                    }
                    case LL_EQUALS_SINGLE_QUOTES: {
                        jj_consume_token(LL_EQUALS_SINGLE_QUOTES);
                        lockRecords = new ArrayList();
                        lockRecordsCount = 0;
                        lockSpaceProps = new HashMap(4);
                        lockSpaceProps.put(LOCK_SPACE_NAME_PROP_NAME, LockComplexSpace());
                        lockSpaceProps.put(LOCK_SPACE_TYPE_PROP_NAME, LockType());
                        lockSpaceProps.put(LOCK_SPACE_RECORDS_PROP_NAME, lockRecords);
                        lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                        lockSpaces.add(lockSpaceProps);
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case LLP_IDENTIFIER:
                            case LLSQP_IDENTIFIER:
                            case LLDQP_IDENTIFIER: {
                                LockProperties(); // заполняются lockRecords, lockRecordsCount
                                lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                                break;
                            }
                            default:
                                jj_la1[34] = jj_gen;
                                ;
                        }
                        label_12:
                        while (true) {
                            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                                case LLSQP_COMMA: {
                                    ;
                                    break;
                                }
                                default:
                                    jj_la1[35] = jj_gen;
                                    break label_12;
                            }
                            jj_consume_token(LLSQP_COMMA);
                            if (jj_2_3(2)) {
                                lockRecords = new ArrayList();
                                lockRecordsCount = 0;
                                lockSpaceProps = new HashMap(4);
                                lockSpaceProps.put(LOCK_SPACE_NAME_PROP_NAME, LockComplexSpace());
                                lockSpaceProps.put(LOCK_SPACE_TYPE_PROP_NAME, LockType());
                                lockSpaceProps.put(LOCK_SPACE_RECORDS_PROP_NAME, lockRecords);
                                lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                                lockSpaces.add(lockSpaceProps);
                            }
                            else {
                                ;
                            }
                            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                                case LLP_IDENTIFIER:
                                case LLSQP_IDENTIFIER:
                                case LLDQP_IDENTIFIER: {
                                    LockProperties(); // заполняются lockRecords, lockRecordsCount
                                    lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                                    break;
                                }
                                default:
                                    jj_la1[36] = jj_gen;
                                    ;
                            }
                        }
                        jj_consume_token(LLSQP_SINGLE_QUOTE);
                        break;
                    }
                    case LL_EQUALS_DOUBLE_QUOTES: {
                        jj_consume_token(LL_EQUALS_DOUBLE_QUOTES);
                        lockRecords = new ArrayList();
                        lockRecordsCount = 0;
                        lockSpaceProps = new HashMap(4);
                        lockSpaceProps.put(LOCK_SPACE_NAME_PROP_NAME, LockComplexSpace());
                        lockSpaceProps.put(LOCK_SPACE_TYPE_PROP_NAME, LockType());
                        lockSpaceProps.put(LOCK_SPACE_RECORDS_PROP_NAME, lockRecords);
                        lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                        lockSpaces.add(lockSpaceProps);
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case LLP_IDENTIFIER:
                            case LLSQP_IDENTIFIER:
                            case LLDQP_IDENTIFIER: {
                                LockProperties(); // заполняются lockRecords, lockRecordsCount
                                lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                                break;
                            }
                            default:
                                jj_la1[37] = jj_gen;
                                ;
                        }
                        label_13:
                        while (true) {
                            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                                case LLDQP_COMMA: {
                                    ;
                                    break;
                                }
                                default:
                                    jj_la1[38] = jj_gen;
                                    break label_13;
                            }
                            jj_consume_token(LLDQP_COMMA);
                            if (jj_2_4(2)) {
                                lockRecords = new ArrayList();
                                lockRecordsCount = 0;
                                lockSpaceProps = new HashMap(4);
                                lockSpaceProps.put(LOCK_SPACE_NAME_PROP_NAME, LockComplexSpace());
                                lockSpaceProps.put(LOCK_SPACE_TYPE_PROP_NAME, LockType());
                                lockSpaceProps.put(LOCK_SPACE_RECORDS_PROP_NAME, lockRecords);
                                lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                                lockSpaces.add(lockSpaceProps);
                            }
                            else {
                                ;
                            }
                            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                                case LLP_IDENTIFIER:
                                case LLSQP_IDENTIFIER:
                                case LLDQP_IDENTIFIER: {
                                    LockProperties(); // заполняются lockRecords, lockRecordsCount
                                    lockSpaceProps.put(LOCK_SPACE_RECORDS_COUNT_PROP_NAME, lockRecordsCount);
                                    break;
                                }
                                default:
                                    jj_la1[39] = jj_gen;
                                    ;
                            }
                        }
                        jj_consume_token(LLDQP_DOUBLE_QUOTE);
                        break;
                    }
                    default:
                        jj_la1[40] = jj_gen;
                        jj_consume_token(-1);
                        throw new ParseException();
                }
                logRecord.put(propertyName, lockSpaces);
                logRecord.containsLocks = true;
                break;
            }
            case EDP_ESCALATING: {
                jj_consume_token(EDP_ESCALATING);
                propertyName = token.image;
                jj_consume_token(EDL_EQUALS);
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case VL_INTEGER:
                    case VL_STRING:
                    case VL_STRING_SEQUENCE: {
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case VL_INTEGER: {
                                jj_consume_token(VL_INTEGER);
                                propertyValue = token.image;
                                break;
                            }
                            case VL_STRING: {
                                jj_consume_token(VL_STRING);
                                propertyValue = deQuoted(token.image);
                                break;
                            }
                            case VL_STRING_SEQUENCE: {
                                jj_consume_token(VL_STRING_SEQUENCE);
                                propertyValue = token.image;
                                break;
                            }
                            default:
                                jj_la1[41] = jj_gen;
                                jj_consume_token(-1);
                                throw new ParseException();
                        }
                        break;
                    }
                    default:
                        jj_la1[42] = jj_gen;
                        ;
                }
                logRecord.put(propertyName, propertyValue);
                logRecord.escalating = true;
                break;
            }
            case EDP_ANY_OTHER: {
                jj_consume_token(EDP_ANY_OTHER);
                propertyName = token.image;
                jj_consume_token(EDL_EQUALS);
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case VL_INTEGER:
                    case VL_STRING:
                    case VL_STRING_SEQUENCE: {
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case VL_INTEGER: {
                                jj_consume_token(VL_INTEGER);
                                propertyValue = token.image;
                                break;
                            }
                            case VL_STRING: {
                                jj_consume_token(VL_STRING);
                                propertyValue = getRidOfUnprintables(deQuoted(token.image));
                                break;
                            }
                            case VL_STRING_SEQUENCE: {
                                jj_consume_token(VL_STRING_SEQUENCE);
                                propertyValue = getRidOfUnprintables(token.image);
                                break;
                            }
                            default:
                                jj_la1[43] = jj_gen;
                                jj_consume_token(-1);
                                throw new ParseException();
                        }
                        break;
                    }
                    default:
                        jj_la1[44] = jj_gen;
                        ;
                }
                logRecord.put(propertyName, propertyValue);
                break;
            }
            default:
                jj_la1[45] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
    }

    private boolean jj_2_1(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_1();
        }
        catch (LookaheadSuccess ls) {
            return true;
        }
        finally {
            jj_save(0, xla);
        }
    }

    private boolean jj_2_2(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_2();
        }
        catch (LookaheadSuccess ls) {
            return true;
        }
        finally {
            jj_save(1, xla);
        }
    }

    private boolean jj_2_3(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_3();
        }
        catch (LookaheadSuccess ls) {
            return true;
        }
        finally {
            jj_save(2, xla);
        }
    }

    private boolean jj_2_4(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_4();
        }
        catch (LookaheadSuccess ls) {
            return true;
        }
        finally {
            jj_save(3, xla);
        }
    }

    private boolean jj_3R_15() {
        if (jj_scan_token(LLSQP_IDENTIFIER)) {
            return true;
        }
        if (jj_scan_token(LLSQP_EQUALS)) {
            return true;
        }
        Token xsp;
        xsp = jj_scanpos;
        if (jj_scan_token(42)) {
            jj_scanpos = xsp;
            if (jj_scan_token(43)) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3R_21() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_scan_token(31)) {
            jj_scanpos = xsp;
            if (jj_scan_token(40)) {
                jj_scanpos = xsp;
                if (jj_scan_token(49)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean jj_3R_18() {
        if (jj_3R_20()) {
            return true;
        }
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3R_21()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    private boolean jj_3R_27() {
        if (jj_scan_token(LLDQP_DOT)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_26() {
        if (jj_scan_token(LLSQP_DOT)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_25() {
        if (jj_scan_token(LLP_DOT)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_16() {
        if (jj_scan_token(LLDQP_COMMA)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_24() {
        if (jj_scan_token(LLDQP_IDENTIFIER)) {
            return true;
        }
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3R_27()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    private boolean jj_3R_14() {
        if (jj_scan_token(LLSQP_COMMA)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_23() {
        if (jj_scan_token(LLSQP_IDENTIFIER)) {
            return true;
        }
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3R_26()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    private boolean jj_3R_22() {
        if (jj_scan_token(LLP_IDENTIFIER)) {
            return true;
        }
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3R_25()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    private boolean jj_3R_20() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_22()) {
            jj_scanpos = xsp;
            if (jj_3R_23()) {
                jj_scanpos = xsp;
                if (jj_3R_24()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean jj_3R_17() {
        if (jj_scan_token(LLDQP_IDENTIFIER)) {
            return true;
        }
        if (jj_scan_token(LLDQP_EQUALS)) {
            return true;
        }
        Token xsp;
        xsp = jj_scanpos;
        if (jj_scan_token(51)) {
            jj_scanpos = xsp;
            if (jj_scan_token(52)) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3_4() {
        if (jj_3R_18()) {
            return true;
        }
        if (jj_3R_19()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_2() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_16()) {
            jj_scanpos = xsp;
        }
        if (jj_3R_17()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_1() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_14()) {
            jj_scanpos = xsp;
        }
        if (jj_3R_15()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_3() {
        if (jj_3R_18()) {
            return true;
        }
        if (jj_3R_19()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_19() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_scan_token(26)) {
            jj_scanpos = xsp;
            if (jj_scan_token(27)) {
                jj_scanpos = xsp;
                if (jj_scan_token(35)) {
                    jj_scanpos = xsp;
                    if (jj_scan_token(36)) {
                        jj_scanpos = xsp;
                        if (jj_scan_token(44)) {
                            jj_scanpos = xsp;
                            if (jj_scan_token(45)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Generated Token Manager.
     */
    public OneCTJTokenManager token_source;
    SimpleCharStream jj_input_stream;
    /**
     * Current token.
     */
    public Token token;
    /**
     * Next token.
     */
    public Token jj_nt;
    private int jj_ntk;
    private Token jj_scanpos, jj_lastpos;
    private int jj_la;
    private int jj_gen;
    final private int[] jj_la1 = new int[46];
    static private int[] jj_la1_0;
    static private int[] jj_la1_1;
    static private int[] jj_la1_2;

    static {
        jj_la1_init_0();
        jj_la1_init_1();
        jj_la1_init_2();
    }

    private static void jj_la1_init_0() {
        jj_la1_0 = new int[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x80000001, 0x7f0000, 0xfff0, 0x20000000, 0x0, 0x0, 0x10000000, 0x80000000, 0x80000000, 0xc000000, 0x10000000, 0x0, 0x0, 0x10000000, 0x0, 0x0, 0x0, 0x240000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x10000000, 0x10000000, 0x10000000, 0x0, 0x10000000, 0x10000000, 0x0, 0x10000000, 0x3800000, 0x0, 0x0, 0x0, 0x0, 0x7f0000,};
    }

    private static void jj_la1_init_1() {
        jj_la1_1 = new int[]{0x1800000, 0x1800000, 0x1800000, 0x2000000, 0x1800000, 0x1800000, 0x1800000, 0x2000000, 0x90000001, 0x0, 0x0, 0x0, 0x40, 0x8000, 0x4020, 0x20100, 0x20100, 0x3018, 0x0, 0x100, 0x20000, 0x4020, 0x6, 0xc00, 0x180000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x4020, 0x4020, 0x4020, 0x100, 0x4020, 0x4020, 0x20000, 0x4020, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,};
    }

    private static void jj_la1_init_2() {
        jj_la1_2 = new int[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x11, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0xe, 0xe, 0xe, 0xe, 0xe, 0xe, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0xe, 0xe, 0xe, 0xe, 0x0,};
    }
    final private JJCalls[] jj_2_rtns = new JJCalls[4];
    private boolean jj_rescan = false;
    private int jj_gc = 0;

    /**
     * Constructor with InputStream.
     */
    public OneCTJ(java.io.InputStream stream) {
        this(stream, null);
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.InputStream stream) {
        ReInit(stream, null);
    }

    /**
     * Constructor.
     */
    public OneCTJ(java.io.Reader stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1);
        token_source = new OneCTJTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 46; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.Reader stream) {
        jj_input_stream.ReInit(stream, 1, 1);
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 46; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    /**
     * Constructor with generated Token Manager.
     */
    public OneCTJ(OneCTJTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 46; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    /**
     * Reinitialise.
     */
    public void ReInit(OneCTJTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 46; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    private Token jj_consume_token(int kind) throws ParseException {
        Token oldToken;
        if ((oldToken = token).next != null) {
            token = token.next;
        }
        else {
            token = token.next = token_source.getNextToken();
        }
        jj_ntk = -1;
        if (token.kind == kind) {
            jj_gen++;
            if (++jj_gc > 100) {
                jj_gc = 0;
                for (int i = 0; i < jj_2_rtns.length; i++) {
                    JJCalls c = jj_2_rtns[i];
                    while (c != null) {
                        if (c.gen < jj_gen) {
                            c.first = null;
                        }
                        c = c.next;
                    }
                }
            }
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }

    @SuppressWarnings("serial")
    static private final class LookaheadSuccess extends java.lang.Error {
    }
    final private LookaheadSuccess jj_ls = new LookaheadSuccess();

    private boolean jj_scan_token(int kind) {
        if (jj_scanpos == jj_lastpos) {
            jj_la--;
            if (jj_scanpos.next == null) {
                jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
            }
            else {
                jj_lastpos = jj_scanpos = jj_scanpos.next;
            }
        }
        else {
            jj_scanpos = jj_scanpos.next;
        }
        if (jj_rescan) {
            int i = 0;
            Token tok = token;
            while (tok != null && tok != jj_scanpos) {
                i++;
                tok = tok.next;
            }
            if (tok != null) {
                jj_add_error_token(kind, i);
            }
        }
        if (jj_scanpos.kind != kind) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            throw jj_ls;
        }
        return false;
    }

    /**
     * Get the next Token.
     */
    final public Token getNextToken() {
        if (token.next != null) {
            token = token.next;
        }
        else {
            token = token.next = token_source.getNextToken();
        }
        jj_ntk = -1;
        jj_gen++;
        return token;
    }

    /**
     * Get the specific Token.
     */
    final public Token getToken(int index) {
        Token t = token;
        for (int i = 0; i < index; i++) {
            if (t.next != null) {
                t = t.next;
            }
            else {
                t = t.next = token_source.getNextToken();
            }
        }
        return t;
    }

    private int jj_ntk_f() {
        if ((jj_nt = token.next) == null) {
            return (jj_ntk = (token.next = token_source.getNextToken()).kind);
        }
        else {
            return (jj_ntk = jj_nt.kind);
        }
    }

    private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
    private int[] jj_expentry;
    private int jj_kind = -1;
    private int[] jj_lasttokens = new int[100];
    private int jj_endpos;

    private void jj_add_error_token(int kind, int pos) {
        if (pos >= 100) {
            return;
        }

        if (pos == jj_endpos + 1) {
            jj_lasttokens[jj_endpos++] = kind;
        }
        else if (jj_endpos != 0) {
            jj_expentry = new int[jj_endpos];

            for (int i = 0; i < jj_endpos; i++) {
                jj_expentry[i] = jj_lasttokens[i];
            }

            for (int[] oldentry : jj_expentries) {
                if (oldentry.length == jj_expentry.length) {
                    boolean isMatched = true;

                    for (int i = 0; i < jj_expentry.length; i++) {
                        if (oldentry[i] != jj_expentry[i]) {
                            isMatched = false;
                            break;
                        }

                    }
                    if (isMatched) {
                        jj_expentries.add(jj_expentry);
                        break;
                    }
                }
            }

            if (pos != 0) {
                jj_lasttokens[(jj_endpos = pos) - 1] = kind;
            }
        }
    }

    /**
     * Generate ParseException.
     */
    public ParseException generateParseException() {
        jj_expentries.clear();
        boolean[] la1tokens = new boolean[70];
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 46; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                    if ((jj_la1_1[i] & (1 << j)) != 0) {
                        la1tokens[32 + j] = true;
                    }
                    if ((jj_la1_2[i] & (1 << j)) != 0) {
                        la1tokens[64 + j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 70; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.add(jj_expentry);
            }
        }
        jj_endpos = 0;
        jj_rescan_token();
        jj_add_error_token(0, 0);
        int[][] exptokseq = new int[jj_expentries.size()][];
        for (int i = 0; i < jj_expentries.size(); i++) {
            exptokseq[i] = jj_expentries.get(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

    /**
     * Enable tracing.
     */
    final public void enable_tracing() {
    }

    /**
     * Disable tracing.
     */
    final public void disable_tracing() {
    }

    private void jj_rescan_token() {
        jj_rescan = true;
        for (int i = 0; i < 4; i++) {
            try {
                JJCalls p = jj_2_rtns[i];

                do {
                    if (p.gen > jj_gen) {
                        jj_la = p.arg;
                        jj_lastpos = jj_scanpos = p.first;
                        switch (i) {
                            case 0:
                                jj_3_1();
                                break;
                            case 1:
                                jj_3_2();
                                break;
                            case 2:
                                jj_3_3();
                                break;
                            case 3:
                                jj_3_4();
                                break;
                        }
                    }
                    p = p.next;
                }
                while (p != null);

            }
            catch (LookaheadSuccess ls) {
            }
        }
        jj_rescan = false;
    }

    private void jj_save(int index, int xla) {
        JJCalls p = jj_2_rtns[index];
        while (p.gen > jj_gen) {
            if (p.next == null) {
                p = p.next = new JJCalls();
                break;
            }
            p = p.next;
        }

        p.gen = jj_gen + xla - jj_la;
        p.first = token;
        p.arg = xla;
    }

    static final class JJCalls {

        int gen;
        Token first;
        int arg;
        JJCalls next;
    }

}
