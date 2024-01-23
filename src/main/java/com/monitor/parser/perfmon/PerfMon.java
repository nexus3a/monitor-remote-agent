package com.monitor.parser.perfmon;

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

import com.monitor.parser.ParseException;
import com.monitor.parser.Token;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class PerfMon implements PerfMonConstants {

    private final PMLogRecord logRecord = new PMLogRecord();
    private long bytesRead;

    protected long recordsCount = 0L;
    private long perfomance = 0L;
    private int valueIndex = 0;
    private boolean headless = false;

    public static void main(String args[]) throws Throwable {
        final PerfMon parser = new PerfMon();
        parser.parse(new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\PerfMon\\Performance Counter.tsv"), false, "UTF-8");
        System.out.println("perfomance: " + parser.getPerfomance());
        System.out.println("records: " + parser.getRecordsCount());
    }

    public void parse(InputStream inputStream, boolean headless, String encoding) throws ParseException {

        ReInit(inputStream, encoding);

        logRecord.clear();
        bytesRead = 0;
        recordsCount = 0;
        perfomance = 0;
        this.headless = headless;

        long m1 = System.currentTimeMillis();

        onParseBegin();
        LogRecords();

        perfomance = System.currentTimeMillis() - m1;
        onParseEnd();
    }

    public void parse(File file, boolean headless, String encoding) throws FileNotFoundException, IOException, ParseException {
        FileInputStream inputStream = new FileInputStream(file);
        parse(inputStream, headless, encoding);
    }

    public final long getPerfomance() {
        return perfomance;
    }

    public final long getRecordsCount() {
        return recordsCount;
    }

    public boolean onLogRecord(PMLogRecord logRecord) {
        return true;
    }

    public void onParseBegin() {
    }

    public void onParseEnd() {
    }

    public PerfMon() {
        this((java.io.InputStream) null, "UTF-8");
    }

    public PerfMon(java.io.InputStream stream, String encoding) {
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
        token_source = new PerfMonTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
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
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
    }

    public long getBytesRead() {
        return bytesRead;
    }

    final public void LogRecords() throws ParseException {
        SequenceOfLogRecords();
    }

    final public void SequenceOfLogRecords() throws ParseException {
        boolean cont;
        label_1:
        while (true) {
            cont = LogRecord();
            bytesRead = token.bytesRead;
            if (!cont) {
                break;
            }
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case EOL:
                case BOM:
                case VALUE: {
                    ;
                    break;
                }
                default:
                    jj_la1[0] = jj_gen;
                    break label_1;
            }
        }
    }

    final public boolean LogRecord() throws ParseException {
        label_2:
        while (true) {
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case EOL:
                case BOM: {
                    ;
                    break;
                }
                default:
                    jj_la1[1] = jj_gen;
                    break label_2;
            }
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case BOM: {
                    jj_consume_token(BOM);
                    break;
                }
                case EOL: {
                    jj_consume_token(EOL);
                    break;
                }
                default:
                    jj_la1[2] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
            }
        }
        logRecord.clear();
        valueIndex = 0;
        Value();
        label_3:
        while (true) {
            jj_consume_token(SEPARATOR);
            Value();
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case SEPARATOR: {
                    ;
                    break;
                }
                default:
                    jj_la1[3] = jj_gen;
                    break label_3;
            }
        }
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case EOL: {
                jj_consume_token(EOL);
                break;
            }
            case 0: {
                jj_consume_token(0);
                break;
            }
            default:
                jj_la1[4] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        recordsCount++;
        return onLogRecord(logRecord);
     }

    final public void Value() throws ParseException {
        jj_consume_token(VALUE);
        String value = token.image.substring(1, token.image.length() - 1);
        if (valueIndex == 0 && (recordsCount > 0 || headless)) {
            // это время сбора счётчика, преобразовываем к формату 1С; 15/04/2022 15:53:12.136
            try {
                value = value.substring(6, 10) 
                        + value.substring(0, 2) 
                        + value.substring(3, 5) 
                        + value.substring(11, 13) 
                        + value.substring(14, 16) 
                        + value.substring(17, 19);
            }
            catch (Exception ex) {
            }
        }
        logRecord.put(String.valueOf(valueIndex++), value);
    }

    /**
     * Generated Token Manager.
     */
    public PerfMonTokenManager token_source;
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
    private int jj_gen;
    final private int[] jj_la1 = new int[5];
    static private int[] jj_la1_0;

    static {
        jj_la1_init_0();
    }

    private static void jj_la1_init_0() {
        jj_la1_0 = new int[]{0x16, 0x6, 0x6, 0x8, 0x3,};
    }

    /**
     * Constructor with InputStream.
     */
    public PerfMon(java.io.InputStream stream) {
        this(stream, null);
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.InputStream stream) {
        ReInit(stream, null);
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
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
    }

    /**
     * Constructor with generated Token Manager.
     */
    public PerfMon(PerfMonTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
    }

    /**
     * Reinitialise.
     */
    public void ReInit(PerfMonTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
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
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();

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

    /**
     * Generate ParseException.
     */
    public ParseException generateParseException() {
        jj_expentries.clear();
        boolean[] la1tokens = new boolean[5];
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 5; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.add(jj_expentry);
            }
        }
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

}
