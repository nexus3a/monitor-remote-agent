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
package com.monitor.parser;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Алексей
 */
public class ParserParameters {
    
    private Set<String> excludeData;
    private int maxTokenLength;
    private String parserErrorLog;
    private int delay;

    public ParserParameters() {
        excludeData = new HashSet<>();
        maxTokenLength = 1024 * 32;
        parserErrorLog = "logs/parser-errors";
        delay = 0;
    }

    public Set<String> getExcludeData() {
        return excludeData;
    }

    public void setExcludeData(Set<String> excludeData) {
        this.excludeData = excludeData;
    }

    public int getMaxTokenLength() {
        return maxTokenLength;
    }

    public void setMaxTokenLength(int maxTokenLength) {
        this.maxTokenLength = maxTokenLength;
    }

    public String getParserErrorLog() {
        return parserErrorLog;
    }

    public void setParserErrorLog(String parserErrorLog) {
        this.parserErrorLog = parserErrorLog;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
    
}
