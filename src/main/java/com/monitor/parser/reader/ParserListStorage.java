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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Алексей
 */
public class ParserListStorage implements ParserRecordsStorage {
    
    private final List<byte[]> records;

    public ParserListStorage() {
        this.records = new ArrayList<>();
    }

    @Override
    public void put(byte[] record) throws Exception {
        records.add(record);
    }
    
    public int size() {
        return records.size();
    }
    
    public void clear() {
        records.clear();
    }
    
    public List<byte[]> getAll() {
        return records;
    }
    
}
