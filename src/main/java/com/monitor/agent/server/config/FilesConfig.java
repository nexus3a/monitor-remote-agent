package com.monitor.agent.server.config;

/*
 * Copyright 2015 Didier Fetter
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
 * Changes by Aleksei Andreev:
 * - removed "multiline"
 * - added "encoding"
 * - added "d" analyse in "getDeadTimeInSeconds"
 *
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.monitor.agent.server.filter.Filter;
import java.io.UnsupportedEncodingException;

public class FilesConfig {

    private String section = "common";
    private List<String> paths;
    private Map<String, String> fields;
    @JsonProperty("dead time")
    private String deadTime = "24h";
    private Filter filter;
    private String encoding = "UTF-8";

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
    
    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public String getDeadTime() {
        return deadTime;
    }

    @JsonIgnore
    public long getDeadTimeInSeconds() {
        long deadTimeInSeconds = 0;
        String remaining = deadTime;

        if (deadTime.contains("d")) {
            String[] splitByDay = deadTime.split("d", 2);
            if (splitByDay.length > 1) {
                remaining = splitByDay[1];
            }
            deadTimeInSeconds += Integer.parseInt(splitByDay[0]) * 24 * 3600;
        }
        if (deadTime.contains("h")) {
            String[] splitByHour = deadTime.split("h", 2);
            if (splitByHour.length > 1) {
                remaining = splitByHour[1];
            }
            deadTimeInSeconds += Integer.parseInt(splitByHour[0]) * 3600;
        }
        if (remaining.contains("m")) {
            String[] splitByMinute = remaining.split("m", 2);
            if (splitByMinute.length > 1) {
                remaining = splitByMinute[1];
            }
            deadTimeInSeconds += Integer.parseInt(splitByMinute[0]) * 60;
        }
        if (remaining.contains("s")) {
            String[] splitBySecond = remaining.split("s", 2);
            deadTimeInSeconds += Integer.parseInt(splitBySecond[0]);
        }
        return deadTimeInSeconds;
    }

    public void setDeadTime(String deadTime) {
        this.deadTime = deadTime;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Object> filterMap) throws UnsupportedEncodingException {
        this.filter = Filter.fromMap(filterMap);
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("paths", paths).
                append("fields", fields).
                append("dead time", deadTime).
                append("filter", filter).
                append("encoding", encoding).
                toString();
    }
}
