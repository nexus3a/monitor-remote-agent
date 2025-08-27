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

import com.monitor.parser.LogRecord;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Алексей
 */
public class ParserListStorage implements ParserRecordsStorage {
    
    private final List<LogRecord> records;

    public ParserListStorage() {
        this.records = new ArrayList<>();
    }

    @Override
    public void put(LogRecord record) throws Exception {
        records.add(record);
    }

    @Override
    public void knock() throws Exception {
    }
    
    @Override
    public int size() {
        return records.size();
    }
    
    @Override
    public void clear() {
        records.clear();
    }
    
    @Override
    public void close() {
    }

    @Override
    public List<LogRecord> getAll() {
        return records;
    }
    
}
