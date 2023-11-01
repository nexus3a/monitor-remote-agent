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
    
    private final OutputStream stream;
    private boolean first;
    private int size;

    public ParserStreamStorage(OutputStream stream) {
        this.stream = stream;
        this.first = true;
        this.size = 0;
    }

    @Override
    public void put(byte[] record) throws IOException {
        if (first) {
            first = false;
        }
        else {
            stream.write(COMMA);
        }
        stream.write(record);
        size++;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
    }

    @Override
    public List<byte[]> getAll() {
        return new ArrayList<>();
    }
    
}
