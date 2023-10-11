package com.monitor.agent.server.filter;

/*
 * Copyright 2022 Aleksei Andreev
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

public class Filter {
    
    @JsonIgnore
    Filter source = null;
    
    public static Filter fromJson(String filterJson) throws UnsupportedEncodingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        Map<String, Object> filtersMap = mapper.readValue(filterJson, Map.class);
        return fromMap(filtersMap);
    }

    public static Filter fromMap(Map<String, Object> map) throws UnsupportedEncodingException, IllegalArgumentException {
        Filter result;
        
        if (map == null) {
            result = null;
        }
        else if (map.containsKey("pattern")) {
            result = new FilterValue(map);
        }
        else if (map.containsKey("and")) {
            FilterAnd filterAnd = new FilterAnd();
            ArrayList<Map<String, Object>> filterMaps = (ArrayList<Map<String, Object>>) map.get("and");
            for (Map<String, Object> filterMap : filterMaps) {
                Filter filter = Filter.fromMap(filterMap);
                filterAnd.add(filter);
            }
            result = filterAnd;
        }
        else if (map.containsKey("or")) {
            FilterOr filterOr = new FilterOr();
            ArrayList<Map<String, Object>> filterMaps = (ArrayList<Map<String, Object>>) map.get("or");
            for (Map<String, Object> filterMap : filterMaps) {
                Filter filter = Filter.fromMap(filterMap);
                filterOr.add(filter);
            }
            result = filterOr;
        }
        else if (map.containsKey("not")) {
            Filter filter = Filter.fromMap((Map<String, Object>) map.get("not"));
            result = new FilterNot(filter);
        }
        else {
            throw new IllegalArgumentException();
        }
        
        return result;
    }
    
    public static Filter and(Filter f1, Filter f2) {
        if (f1 != null && f2 != null) {
            return new FilterAnd().add(f1).add(f2);
        }
        if (f1 != null) {
            return f1;
        }
        return f2;
    }
    
    public boolean accept(Map<String, Object> record) {
        return false;
    }

    public Filter copy() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Filter clone = fromMap(mapper.readValue(mapper.writeValueAsString(this), Map.class));
            clone.source = source == null ? this : source;
            return clone;
        }
        catch (JsonProcessingException | UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int hashCode() {
        if (source != null) {
            return source.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Filter other = (Filter) obj;
        if (source != null && source == other.source 
                || source != null && source == other 
                || other.source != null && other.source == this) {
            return true;
        }
        return super.equals(other);
    }
    
}
