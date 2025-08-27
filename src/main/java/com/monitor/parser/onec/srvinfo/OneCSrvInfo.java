/*
 * Copyright 2025 Cube.
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
package com.monitor.parser.onec.srvinfo;

import com.monitor.agent.server.FileState;
import com.monitor.agent.server.filter.Filter;
import com.monitor.parser.LogParser;
import com.monitor.parser.LogRecord;
import com.monitor.parser.ParseException;
import com.monitor.parser.ParserParameters;
import com.monitor.parser.reader.ParserListStorage;
import com.monitor.parser.reader.ParserRecordsStorage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Cube
 */
public class OneCSrvInfo {
    
    private static OneCSrvInfoRecord readRecord(LogParser parser, File srvInfo, Filter filter) 
            throws IOException, ParseException {
        
        ParserRecordsStorage storage = new ParserListStorage();
        FileState state = new FileState(srvInfo);
        state.setPointer(0);
        
        parser.setRecordsStorage(storage);
        parser.parse(state, "UTF-8", state.getPointer(), 2 + 999999999, filter, new ParserParameters());
        
        // ожидается всего одна запись после разбора файла
        //
        List<LogRecord> records = storage.getAll();
        if (records.isEmpty()) {
            return new OneCSrvInfoRecord();
        }
        OneCSrvInfoRecord record = (OneCSrvInfoRecord) records.get(0);
        
        return record;
    }

    public static OneCSrvInfoRecord getInfo(String catalog, Filter filter) throws IOException, ParseException {
        
        File srvInfoCatalog = new File(catalog);
        if (!srvInfoCatalog.exists() || !srvInfoCatalog.isDirectory()) {
            throw new IOException("\"" + catalog + "\" is not a directory or doesn't exists");
        }

        File srvInfo = new File(srvInfoCatalog, "1cv8wsrv.lst");
        if (!srvInfo.exists() || !srvInfo.isFile()) {
            throw new IOException("can't find 1cv8wsrv.lst in \"" + catalog + "\" directory");
        }
        
        OneCSrvInfoRecord srvRecord = readRecord(new OneCSrvInfoParser(), srvInfo, filter);
        
        ArrayList<OneCSrvInfoRecord> clusters = (ArrayList<OneCSrvInfoRecord>) srvRecord.get("clusters");
        for (OneCSrvInfoRecord cluster : clusters) {
            
            String clusterCatalog = "reg_" + (String) cluster.get("port");
            
            File clusterInfoCatalog = new File(srvInfoCatalog, clusterCatalog);
            if (!clusterInfoCatalog.exists() || !clusterInfoCatalog.isDirectory()) {
                throw new IOException("\"" + catalog + "\" is not a directory or doesn't exists");
            }

            File clusterInfo = new File(clusterInfoCatalog, "1CV8Clst.lst");
            if (!clusterInfo.exists() || !clusterInfo.isFile()) {
                throw new IOException("can't find 1CV8Clst.lst in \"" + clusterCatalog + "\" directory");
            }

            OneCSrvInfoRecord clusterRecord = readRecord(new OneCClusterInfoParser(), clusterInfo, filter);
            clusterRecord.remove("cluster"); // дублирует информацию из srvRecord
            cluster.putAll(clusterRecord);
        }
        
        return srvRecord;
    
    }

}
