package com.monitor.agent.server;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Контейнер набора предопределённых полей и их значений, которые будут добавлены
 * к каждой возвращаемой клиенту записи.
 * По функционалу повторяет HashMap (однако, считаем, что после заполнения преопределённых
 * значений эта карта становится неизменяемой - но это программно не контролируется!),
 * особенность этого класса - возможность быстрого сравнения *клонирванных* объектов -
 * чтобы не сравнивать поэлементно, а, зная оригинал, сравнивать только одну ссылку;
 * эта особенность используется в классе DirectoryWatcher.WatchKey при поиске по ключу
 */
public class PredefinedFields extends HashMap<String, String> {
    
    @JsonIgnore
    private PredefinedFields source = null;
    
    public static PredefinedFields fromMap(Map<String, String> m) {
        return m == null ? null : new PredefinedFields(m);
    }

    public PredefinedFields() {
    }

    public PredefinedFields(Map<String, String> m) {
        super(m);
    }

    public PredefinedFields copy() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            PredefinedFields clone = mapper.readValue(mapper.writeValueAsString(this), PredefinedFields.class);
            clone.source = source == null ? this : source;
            return clone;
        }
        catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int hashCode() {
        return source == null ? super.hashCode() : source.hashCode();
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
        final PredefinedFields other = (PredefinedFields) obj;
        if (source != null && source == other.source
                || source != null && source == other 
                || other.source != null && other.source == this) {
            return true;
        }
        return super.equals(other);
    }
    
}
