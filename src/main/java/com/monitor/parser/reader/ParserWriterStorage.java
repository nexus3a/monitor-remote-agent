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
package com.monitor.parser.reader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.parser.LogRecord;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Алексей
 */
public class ParserWriterStorage implements ParserRecordsStorage {
    
    private int size;
    private final Writer writer;
    private final ObjectMapper mapper;

    public ParserWriterStorage(Writer writer) {
        this.size = 0;
        this.writer = writer;
        
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mapper = new ObjectMapper(jsonFactory);
    }

    @Override
    public void put(LogRecord record) throws Exception {
        this.size++;
        writer.write(mapper.writeValueAsString(record));
    //  mapper.writeValue(writer, record);
        if (size % 100 == 0) {
            writer.flush();
        }
    }

    @Override
    public void knock() throws Exception {
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @Override
    public void clear() {
        this.size = 0;
    }
    
    @Override
    public void close() {
    }

    @Override
    public List<byte[]> getAll() {
        return new ArrayList<>();
    }
    
}
