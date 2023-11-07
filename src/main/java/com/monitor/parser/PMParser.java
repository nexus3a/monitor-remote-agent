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
import com.monitor.parser.perfmon.PerfMon;
import com.monitor.parser.reader.ParserListStorage;
import com.monitor.parser.reader.ParserRecordsStorage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PMParser extends PerfMon implements LogParser {
    
    private ParserRecordsStorage recordsStorage;
    private long recordsBytesRead;
    private Throwable exception;
    private final ObjectMapper mapper = new ObjectMapper();
    private int maxCount;
    private PredefinedFields addFields;
    private Filter filter;
    private long filteredCount;
    private boolean firstInFile;
    private int delay;
    
    
    public static void main(String[] args) throws IOException, ParseException {
        ParserListStorage recordsStorage = new ParserListStorage();
        PMParser parser = new PMParser();
        parser.setRecordsStorage(recordsStorage);

        File file = new File("d:\\java\\projects\\monitor-remote-agent\\src\\test\\logs\\PerfMon\\Performance Counter.tsv");
        FileState state = new FileState(file);
        state.setPointer(0);
        parser.parse(state, "UTF-8", state.getPointer(), 5, null, null); // 5 == maxRecords
        long pos = parser.getBytesRead();
        System.out.println("found " + recordsStorage.size() + " record(s)");
        System.out.println("bytes read: " + pos);
    }
    
    
    public PMParser() {
        recordsStorage = new ParserListStorage();
        filteredCount = 0L;
        firstInFile = true;
        recordsBytesRead = 0L;
        exception = null;
        delay = 0;
    }
    
    
    @Override
    public void parse(FileState state, String encoding, long fromPosition, int maxRecords, Filter filter, ParserParameters parameters)
            throws IOException, ParseException {

        addFields = state.getFields();

        filteredCount = 0L;
        firstInFile = true;
        recordsBytesRead = fromPosition;
        delay = parameters.getDelay();
        exception = null;
        
        try (BufferedRandomAccessFileStream stream = new BufferedRandomAccessFileStream(state.getOpenedRandomAccessFile(), 4096)) {
            if (fromPosition > 0) {
                // если читаем файл не с самого начала, то в результирующий набор
                // добавим запись из перовй строки лога - с заголовками записей
                this.filter = null;
                this.maxCount = 0;
                stream.seek(0);
                parse(stream, false, encoding);
            }
            // парсим и добавляем значащие записи
            if (fromPosition == 0 || maxRecords > 0) {
                this.filter = Filter.and(state.getFilter(), filter == null ? null : filter.copy());
                this.maxCount = maxRecords;
                stream.seek(fromPosition);
                parse(stream, fromPosition > 0, encoding);
            }
            // в конце (finally) будет stream.close()
        }
        catch (Exception ex) {
            exception = ex;
            throw ex;
        }
        
        if (fromPosition + super.getBytesRead() >= state.getSize() - 1) {
            // close random access file if we are at the end of it
//          stream.close();
//          state.closeRandomAccessFile();
        }
    }    
    

    @Override
    public boolean onLogRecord(Map<String, Object> logRecord) {
        try {
            // первая строка лога PerfMon всегда содержит заголовки данных,
            // поэтому её фильтровать не нужно (firstInFile)
            //
            if (firstInFile) {
                logRecord.put("header", "true");
                firstInFile = false;
            }
            else if (filter == null || filter.accept(logRecord)) {
                filteredCount++;
            }
            else {
                return true; // не записываем в коллекцию записей, читаем дальше
            }
            if (addFields != null) {
                logRecord.putAll(addFields);
            }
            recordsStorage.put(mapper.writeValueAsBytes(logRecord));
            if (delay > 0) {
                Thread.sleep(delay);
            }
            recordsBytesRead = super.getBytesRead();
        }
        catch (Exception ex) {
            Map<String, String> message = new HashMap<>(1);
            message.put("LOGSERIALIZEERROR", ex.getMessage());
            try {
                recordsStorage.put(mapper.writeValueAsBytes(message));
            }
            catch (Exception ex1) {}
        }
        return filteredCount < maxCount;
    }

    
    @Override
    public void setRecordsStorage(ParserRecordsStorage recordsStorage) {
        this.recordsStorage = recordsStorage;
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
        return recordsBytesRead;
    }


    @Override
    public Throwable getException() {
        return exception;
    }
    
    
}
