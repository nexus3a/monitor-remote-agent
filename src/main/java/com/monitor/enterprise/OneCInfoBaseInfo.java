package com.monitor.enterprise;

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

import com.monitor.agent.server.config.OneCInfoBaseConfig;
import java.util.List;
import java.util.Map;

public class OneCInfoBaseInfo {
    
    private String name;
    private List<Map<String, Object>> sessions;
    private StackTraceElement[] exception;

    public OneCInfoBaseInfo() {
    }
    
    public OneCInfoBaseInfo(OneCInfoBaseConfig config) {
        name = config.getName();
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Map<String, Object>> getSessions() {
        return sessions;
    }

    public void setSessions(List<Map<String, Object>> sessions) {
        this.sessions = sessions;
    }

    public StackTraceElement[] getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception.getStackTrace();
    }
    
}
