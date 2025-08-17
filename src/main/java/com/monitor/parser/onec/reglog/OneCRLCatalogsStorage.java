/*
 * Copyright 2025 Алексей.
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
package com.monitor.parser.onec.reglog;

import com.monitor.parser.reader.*;
import com.monitor.parser.LogRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Алексей
 */
public class OneCRLCatalogsStorage implements ParserRecordsStorage {
    
    private int size;

    public HashMap<Object, OneCRLCatalogRecord> users = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> computers = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> applications = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> events = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> metadata = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> servers = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> mainPorts = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> additionalPorts = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> dataAreas = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> collection10 = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> collection11 = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> collection12 = new HashMap<>();
    public HashMap<Object, OneCRLCatalogRecord> collection13 = new HashMap<>();
    
    public OneCRLCatalogsStorage() {
        this.size = 0;
    }

    @Override
    public void put(LogRecord record) throws Exception {
        this.size++;
        
        String catalog = (String) record.get(OneCRLDescriptorsParser.CATALOG_PROP_NAME);
        String value = (String) record.get(OneCRLDescriptorsParser.DATA_VALUE_PROP_NAME);
        String description = (String) record.get(OneCRLDescriptorsParser.DATA_DESCRIPTION_PROP_NAME);
        String index = (String) record.get(OneCRLDescriptorsParser.DATA_INDEX_PROP_NAME);
        
        if (null != catalog) switch (catalog) {
            case "1":
                users.put(index, new OneCRLCatalogRecord(index, value, description));
                break;
            case "2":
                computers.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "3":
                applications.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "4":
                events.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "5":
                metadata.put(index, new OneCRLCatalogRecord(index, value, description));
                break;
            case "6":
                servers.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "7":
                mainPorts.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "8":
                additionalPorts.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "9":
                dataAreas.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "10":
                collection10.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "11":
                collection11.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "12":
                collection12.put(index, new OneCRLCatalogRecord(index, value));
                break;
            case "13":
                collection13.put(index, new OneCRLCatalogRecord(index, value));
                break;
            default:
                break;
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
        
        users.clear();
        computers.clear();
        applications.clear();
        events.clear();
        metadata.clear();
        servers.clear();
        mainPorts.clear();
        additionalPorts.clear();
    }
    
    @Override
    public void close() {
    }

    @Override
    public List<byte[]> getAll() {
        return new ArrayList<>();
    }
    
}
