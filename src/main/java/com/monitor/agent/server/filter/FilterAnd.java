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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class FilterAnd extends Filter {
    
    @JsonProperty("and")
    private final ArrayList<Filter> filters;

    public FilterAnd() {
        filters = new ArrayList<>();
    }

    public FilterAnd add(Filter filter) {
        filters.add(filter);
        return this;
    }

    @Override
    public boolean accept(Map<String, Object> record) {
        for (Filter filter : filters) {
            if (!filter.accept(record)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
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
        final FilterAnd other = (FilterAnd) obj;
        if (source != null && source == other.source 
                || source != null && source == other 
                || other.source != null && other.source == this) {
            return true;
        }
        return Objects.equals(filters, other.filters);
    }
    
}
