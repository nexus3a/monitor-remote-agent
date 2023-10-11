package com.monitor.agent.server;

/*
 * Copyright 2022 Aleksei Andreev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.util.HashMap;
import java.util.Objects;

public class Section {
    
    private static final HashMap<String, Section> sections = new HashMap<>();
    
    private final String name;
    
    public static Section byName(String name) {
        
        if (name == null) {
            return null;
        }
        
        synchronized (sections) {

            Section section = sections.get(name);
            if (section != null) {
                return section;
            }
            
            section = new Section(name);
            sections.put(name, section);
            
            return section;

        }
        
    }

    private Section(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.name);
        return hash;
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
        final Section other = (Section) obj;
        return Objects.equals(this.name, other.name);
    }
    
}
