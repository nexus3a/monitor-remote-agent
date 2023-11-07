package com.monitor.parser;

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

import com.monitor.agent.server.filter.Filter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.agent.server.BufferedRandomAccessFileStream;
import com.monitor.agent.server.PredefinedFields;
import com.monitor.agent.server.FileState;
import com.monitor.parser.onec.OneCTJRecord;
import com.monitor.parser.onec.OneCTJ;
import com.monitor.parser.onec.TokenMgrError;
import com.monitor.parser.reader.ParserListStorage;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TJParser extends OneCTJ implements LogParser {
    
    private ParserRecordsStorage recordsStorage;
    private long readyBytesRead;
    private Throwable exception;
    private final ObjectMapper mapper = new ObjectMapper();
    private int maxCount;
    private PredefinedFields addFields;
    private Filter filter;
    private long filteredCount;
    private int delay;
    
    
    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) throws IOException, InterruptedException {
        ParserListStorage recordsStorage = new ParserListStorage();
        TJParser parser = new TJParser();
        parser.setRecordsStorage(recordsStorage);
        
        List<File> sources = filesFromDirectory("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\L70");

        long duration = 0;
        long TESTS_COUNT = 1; // 100;
        
        for (int i = 0; i < TESTS_COUNT; i++) {
            
            for (File file : sources) {
                FileState state = new FileState(file);
                state.setPointer(0);
                System.out.println("file " + file.getAbsolutePath());
                long m = new Date().getTime();
                try {
                    parser.parse(state, "UTF-8", state.getPointer(), 999999999 /* 1024 */, null, null);
                }
                catch (ParseException | TokenMgrError ex) {
                    System.out.println("!!! parse error: " + ex.getMessage());
                }
                duration = duration + (new Date().getTime() - m);
                System.out.println("found rows: " + recordsStorage.size());
                System.out.println(" file size: " + file.length());
                System.out.println("bytes read: " + parser.getBytesRead());
                if (parser.getBytesRead() != file.length()) {
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


    public TJParser() {
        recordsStorage = new ParserListStorage();
        filteredCount = 0L;
        readyBytesRead = 0L;
        exception = null;
        delay = 0;
    }
    
    
    @Override
    @SuppressWarnings("ConvertToTryWithResources")
    public void parse(FileState state, String encoding, long fromPosition, int maxRecords, Filter filter, ParserParameters parameters)
            throws IOException, ParseException {
        
        if (maxRecords <= 0) {
            return;
        }
        
        maxCount = maxRecords;
        addFields = state.getFields();
        this.filter = Filter.and(state.getFilter(), filter == null ? null : filter.copy());
        long pos = fromPosition;
        filteredCount = 0L;
        readyBytesRead = 0;
        delay = parameters.getDelay();
        exception = null;
        String fileName = state.getFile().getName();
        boolean isTJName = fileName.matches("\\d{8}.*\\.log");
        
        try (BufferedRandomAccessFileStream stream = new BufferedRandomAccessFileStream(state.getOpenedRandomAccessFile(), 4096)) {
            stream.seek(pos);
            parse(stream, encoding,
                    isTJName ? Integer.parseInt(fileName.substring(0, 2)) + 2000 : 1970,
                    isTJName ? Integer.parseInt(fileName.substring(2, 4)) : 0,
                    isTJName ? Integer.parseInt(fileName.substring(4, 6)) : 1,
                    isTJName ? Integer.parseInt(fileName.substring(6, 8)) : 0,
                    parameters);
            // в конце (finally) будет stream.close()
        }
        catch (ParseException | TokenMgrError ex) {
            // ошибкой будем считать любое исключение, возникшее при чтении лог-файла, за исключением
            // случая чтения записи лога в процессе её записи, когда она записана не полностью
            String message = ex.getMessage();
            if (message != null 
                    && !message.contains("Encountered: <EOF> after :")
                    && !message.contains("Encountered \"<EOF>\" at")) {
                String parserErrorLog = makeParserErrorsLogDir(parameters);
                File errorFragmentFile = new File(String.format("%s/%s.%s.%s.parse_error", 
                        parserErrorLog,
                        state.getFile().getName(),
                        fromPosition + readyBytesRead,
                        fromPosition + super.getBytesRead() + 256));
                copyFileFragment(state.getFile(), 
                        fromPosition + readyBytesRead,
                        fromPosition + super.getBytesRead() + 256,
                        errorFragmentFile);
                exception = ex;
                throw ex;
            }
        }
        catch (Exception ex) {
            exception = ex;
            throw ex;
        }
        
        if (pos + super.getBytesRead() >= state.getSize() - 1) {
            // close random access file if we are at the end of it
//          stream.close();
//          state.closeRandomAccessFile();
        }
    }

    
    @Override
    public boolean onLogRecord(OneCTJRecord logRecord) {
        try {
            if (filter == null || filter.accept(logRecord)) {
                filteredCount++;
                if (addFields != null) {
                    logRecord.putAll(addFields);
                }
                recordsStorage.put(mapper.writeValueAsBytes(logRecord));
            }
            if (delay > 0) {
                Thread.sleep(delay);
            }
            readyBytesRead = getReadyBytesRead();
        }
        catch (Exception ex) {
            Map<String, String> message = new HashMap<>(1);
            message.put("LOGSERIALIZEERROR", ex.getMessage());
            try {
                recordsStorage.put(mapper.writeValueAsBytes(message));
            }
            catch (Exception ex1) {}
        }
        return filteredCount < maxCount; // было [super.protected] recordsCount < maxCount
    }

    
    @Override
    public void setRecordsStorage(ParserRecordsStorage storage) {
        this.recordsStorage = storage;
    }

    
    public ParserRecordsStorage getRecordsStorage() {
        return recordsStorage;
    }


    @Override
    // Отличается от родительской функции тем, что возвращает не фактическое количество
    // прочитанных байтов, а количество байтов, прочитанных во всех записях лога, не
    // закончившихся ошибкой чтения данных, чтобы можно было начать следующее чтение
    // с позиции начала записи лога, в которой последний раз встретилась ошибка
    //
    public long getBytesRead() {
        return readyBytesRead;
    }


    @Override
    public Throwable getException() {
        return exception;
    }
    
    
}
