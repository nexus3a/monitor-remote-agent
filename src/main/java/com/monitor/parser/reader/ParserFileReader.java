package com.monitor.parser.reader;

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

import com.monitor.agent.server.FileState;
import com.monitor.agent.server.LogFormat;
import com.monitor.agent.server.filter.Filter;
import com.monitor.parser.perfmon.PMParser;
import com.monitor.parser.LogParser;
import com.monitor.parser.ParserParameters;
import com.monitor.parser.onecf.FastTJParser;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserFileReader {

    private static final Logger logger = LoggerFactory.getLogger(ParserFileReader.class);

    protected int spoolSize = 0;
    private final ParserRecordsStorage records;
    private Map<FileState, Long> pointerMap;
    private int recordsCount;
    private final HashMap<LogFormat, LogParser> parsers;
    private final Filter filter;
    private final boolean draft;
    private final ParserParameters parserParameters;

    public ParserFileReader(int spoolSize, Filter filter, boolean draft, ParserParameters parserParameters, 
            ParserRecordsStorage storage) {
        this.spoolSize = spoolSize;
        this.records = storage;
        this.parsers = new HashMap<>(2);
        this.filter = filter;
        this.draft = draft;
        this.parserParameters = parserParameters;

        LogParser parser = new FastTJParser(); // new TJParser();
        parser.setRecordsStorage(records);
        parsers.put(LogFormat.ONE_C_TECH_JOURNAL, parser);

        parser = new PMParser();
        parser.setRecordsStorage(records);
        parsers.put(LogFormat.PERFOMANCE_MONITOR, parser);

        recordsCount = 0;
    }

    public int readFiles(Collection<FileState> fileList) {
        logger.trace("Reading {} file(s)", fileList.size());
        pointerMap = new HashMap<>();
        for (FileState state : fileList) {
            recordsCount += readFile(state, spoolSize - recordsCount);
            if (recordsCount == spoolSize) {
                break;
            }
        }
        if (!draft) {
            for (Entry<FileState, Long> entry : pointerMap.entrySet()) {
                entry.getKey().setNewPointer(entry.getValue().longValue());
            }
            
//          for (FileState state : fileList) {
//              state.setNewPointer(pointerMap.get(state.getFile()));
//          }
        }
//      pointerMap.clear();
        pointerMap = null;
        return recordsCount;
    }

    private int readFile(FileState state, int spaceLeftInSpool) {
        File file = state.getFile();
        long pointer = state.getPointer();
        int recordsRead = 0;
        if (spaceLeftInSpool <= 0) {
        }
        else if (state.isDeleted()) { // Don't try to read this file
            logger.trace("File : {} has been deleted", file);
        }
        else if (state.length() == 0) {
            logger.trace("File : {} is empty", file);
        }
        else if (!draft && pointer >= state.length()) {
            logger.trace("File : {} pointer at EOF", file);
        }
        else {
            logger.trace("File : {}", file);
            logger.trace("  Pointer before reading : {}", pointer);
            logger.trace("  Space left in spool : {}", spaceLeftInSpool);
            int eventListSizeBefore = records.size();
            pointer = readLines(state, spaceLeftInSpool);
            recordsRead = records.size() - eventListSizeBefore;
            logger.trace("  Pointer after reading : {}", pointer);
            logger.trace("  Records read : {}", recordsRead);
        }
        pointerMap.put(state, pointer);
        return recordsRead; // Return number of events read
    }

    private long readLines(FileState state, int spaceLeftInSpool) {
        long pos = state.getPointer();
        long bytesRead = 0;
        LogFormat logFormat = state.getLogFormat();
        if (logFormat != null && logFormat != LogFormat.UNKNOWN) {
            LogParser parser = parsers.get(state.getLogFormat());
            try {
                parser.parse(
                        state, 
                        state.getEncoding(), 
                        (draft ? 0 : state.getPointer()), 
                        spaceLeftInSpool, 
                        filter, 
                        parserParameters);
            }
            catch (Throwable ex) {
                ex.printStackTrace(System.err);
                logger.trace("  Error during reading file : {}", ex.getMessage());
            }
            bytesRead = parser.getFilePos();
        }
        return pos + bytesRead;
    }

    public List<byte[]> getRecords() {
        return records.getAll();
    }
    
    public void done() {
        records.clear();
        parsers.clear();
    }

}
