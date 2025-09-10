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
package com.monitor.parser.onec.reglog;

import java.util.HashMap;

/**
 *
 * @author Cube
 */
public class OneCRLCatalogRecord extends HashMap<String, String> {
    
    private String index;
    private String value;
    private String presentation;

    public OneCRLCatalogRecord(String index, String value, String presentation) {
        put("index", index);
        put("value", value);
        put("presentation", presentation);
        this.index = index;
        this.value = value;
        this.presentation = presentation;
    }
    
    public OneCRLCatalogRecord(String index, String value) {
        this(index, value, value);
    }

    @Override
    public String toString() {
        return "{" + "index=" + index + ", value=" + value + ", presentation=" + presentation + '}';
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
        put("index", index);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        put("value", value);
    }

    public String getPresentation() {
        return presentation;
    }

    public void setPresentation(String presentation) {
        this.presentation = presentation;
        put("presentation", presentation);
    }
    
    
    
}
