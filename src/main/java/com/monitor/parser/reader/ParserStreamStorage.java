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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.parser.LogRecord;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Алексей
 */
public class ParserStreamStorage implements ParserRecordsStorage {
    
    private static final byte[] COMMA = ",\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SPACE = " ".getBytes(StandardCharsets.UTF_8);
    
    private final OutputStream stream;
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean first;
    private int size;
    
    private long streamTouchMoment;
    private long streamTouchTimeout;

    public ParserStreamStorage(OutputStream stream, long streamTouchTimeout) {
        this.stream = stream;
        this.first = true;
        this.size = 0;
        this.streamTouchMoment = System.currentTimeMillis();
        this.streamTouchTimeout = streamTouchTimeout;
    }

    @Override
    public void put(LogRecord record) throws IOException {
        if (first) {
            first = false;
        }
        else {
            stream.write(COMMA);
        }
        stream.write(mapper.writeValueAsBytes(record));
        size++;
    }

    @Override
    public void knock() throws Exception {
        if (System.currentTimeMillis() - streamTouchMoment < streamTouchTimeout) {
            return;
        }
        streamTouchMoment = System.currentTimeMillis();
        stream.write(SPACE);
    }
    
    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
    }

    @Override
    public void close() {
    }

    @Override
    public List<byte[]> getAll() {
        return new ArrayList<>();
    }
    
}
