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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.builder.ToStringBuilder;

public class FilterValue extends Filter {
    
    private static Pattern PROPERTY_PATTERN = Pattern.compile("((?<dot>\\.)?(?<fld>[^\\s\\.\\[\\]]+))|((?<lb>\\[)(?<idx>[^\\s\\.\\[\\]]*)(?<rb>\\]))");
    
    private String property = null;
    private Pattern pattern = null;
    @JsonIgnore
    private double doubleValue = 0.0f;
    @JsonIgnore
    private long longValue = 0;
    @JsonIgnore
    private String strValue = "";
    private boolean negate = false;
    private Operation operation = Operation.S_LIKE;
    @JsonProperty("if-not-exists")
    private boolean ifPropertyNotExists = false;
    @JsonIgnore
    private PropertyPart[] properties = null;
    
    private enum Operation {
        EQUALS,
        LESSER,
        LESSER_EQUALS,
        GREATER,
        GREATER_EQUALS,
        S_LIKE,
        S_EQUALS,
        S_GREATER,
        S_GREATER_EQUALS,
        S_LESSER,
        S_LESSER_EQUALS,
        N_EQUALS,
        N_GREATER,
        N_GREATER_EQUALS,
        N_LESSER,
        N_LESSER_EQUALS,
        D_EQUALS,
        D_GREATER,
        D_GREATER_EQUALS,
        D_LESSER,
        D_LESSER_EQUALS,
        L_EQUALS,
        L_GREATER,
        L_GREATER_EQUALS,
        L_LESSER,
        L_LESSER_EQUALS;
        
        static Operation fromString(String s) {
            switch (s) {
                case "like" :
                    return Operation.S_LIKE;
                case "=" :
                    return Operation.EQUALS;
                case ">" :
                    return Operation.GREATER;
                case ">=" :
                    return Operation.GREATER_EQUALS;
                case "<" :
                    return Operation.LESSER;
                case "<=" :
                    return Operation.LESSER_EQUALS;
                case "s=" :
                    return Operation.S_EQUALS;
                case "s>" :
                    return Operation.S_GREATER;
                case "s>=" :
                    return Operation.S_GREATER_EQUALS;
                case "s<" :
                    return Operation.S_LESSER;
                case "s<=" :
                    return Operation.S_LESSER_EQUALS;
                case "n=" :
                    return Operation.N_EQUALS;
                case "n>" :
                    return Operation.N_GREATER;
                case "n>=" :
                    return Operation.N_GREATER_EQUALS;
                case "n<" :
                    return Operation.N_LESSER;
                case "n<=" :
                    return Operation.N_LESSER_EQUALS;
                case "d=" :
                    return Operation.D_EQUALS;
                case "d>" :
                    return Operation.D_GREATER;
                case "d>=" :
                    return Operation.D_GREATER_EQUALS;
                case "d<" :
                    return Operation.D_LESSER;
                case "d<=" :
                    return Operation.D_LESSER_EQUALS;
                case "l=" :
                    return Operation.L_EQUALS;
                case "l>" :
                    return Operation.L_GREATER;
                case "l>=" :
                    return Operation.L_GREATER_EQUALS;
                case "l<" :
                    return Operation.L_LESSER;
                case "l<=" :
                    return Operation.L_LESSER_EQUALS;
            }
            return Operation.S_LIKE;
        }
        
        static String toString(Operation op) {
            switch (op) {
                case EQUALS :
                    return "=";
                case LESSER :
                    return "<";
                case LESSER_EQUALS :
                    return "<=";
                case GREATER :
                    return ">";
                case GREATER_EQUALS :
                    return ">=";
                case S_EQUALS :
                    return "s=";
                case S_LESSER :
                    return "s<";
                case S_LESSER_EQUALS :
                    return "s<=";
                case S_GREATER :
                    return "s>";
                case S_GREATER_EQUALS :
                    return "s>=";
                case N_EQUALS :
                    return "n=";
                case N_LESSER :
                    return "n<";
                case N_LESSER_EQUALS :
                    return "n<=";
                case N_GREATER :
                    return "n>";
                case N_GREATER_EQUALS :
                    return "n>=";
                case D_EQUALS :
                    return "d=";
                case D_LESSER :
                    return "d<";
                case D_LESSER_EQUALS :
                    return "d<=";
                case D_GREATER :
                    return "d>";
                case D_GREATER_EQUALS :
                    return "d>=";
                case L_EQUALS :
                    return "l=";
                case L_LESSER :
                    return "l<";
                case L_LESSER_EQUALS :
                    return "l<=";
                case L_GREATER :
                    return "l>";
                case L_GREATER_EQUALS :
                    return "l>=";
            }
            return "like";
        }
    }
    
    private class PropertyPart {
        String name;
        int asInt;
        boolean forArray;
        boolean isAll;
        boolean isAny;

        public PropertyPart(String name, boolean forArray) {
            this.name = name;
            this.forArray = forArray;
            this.isAll = "*".equals(name);
            this.isAny = "?".equals(name);
            try {
                this.asInt = Integer.parseInt(name);
            }
            catch (NumberFormatException ex) {
                this.asInt = 0;
            }
        }
    }

    public FilterValue() {
    }

    public FilterValue(FilterValue filter) {
        if (filter != null) {
            this.property = filter.property;
            this.negate = filter.negate;
            this.doubleValue = filter.doubleValue;
            this.longValue = filter.longValue;
            this.strValue = filter.strValue;
            this.operation = filter.operation;
            this.pattern = filter.pattern;
            this.properties = filter.properties;
        }
    }

    @SuppressWarnings("ConvertToStringSwitch")
    public FilterValue(Map<String, Object> fields) throws UnsupportedEncodingException {
        String strPattern = "";
        for (String key : fields.keySet()) {
            if ("pattern".equals(key)) {
                strPattern = String.valueOf(fields.get(key));
            }
            else if ("operation".equals(key)) {
                operation = Operation.fromString(String.valueOf(fields.get(key)));
            }
            else if ("negate".equals(key)) {
                negate = Boolean.parseBoolean(String.valueOf(fields.get(key)));
            }
            else if ("property".equals(key)) {
                property = String.valueOf(fields.get(key));
                Matcher matcher = PROPERTY_PATTERN.matcher(property);
                ArrayList<PropertyPart> p = new ArrayList<>();
                while (matcher.find()) {
                    String group = matcher.group("fld");
                    if (group != null) {
                        p.add(new PropertyPart(group, false));
                    }
                    else {
                        group = matcher.group("idx");
                        if (group != null) {
                            p.add(new PropertyPart(group, true));
                        }
                    }
                }
                properties = new PropertyPart[p.size()];
                p.toArray(properties);
            }
            else if ("if-not-exists".equals(key)) {
                ifPropertyNotExists = Boolean.parseBoolean(String.valueOf(fields.get(key)));
            }
            else {
                throw new UnsupportedEncodingException(key + " not supported");
            }
        }
        
        strValue = strPattern;
        NumberFormatException dfe = null;
        NumberFormatException lfe = null;
        try {
            longValue = Long.parseLong(strPattern);
            doubleValue = longValue;
        }
        catch (NumberFormatException lex) {
            lfe = lex;
            try {
                doubleValue = Double.parseDouble(strPattern);
                longValue = Math.round(doubleValue);
            }
            catch (NumberFormatException dex) {
                dfe = dex;
            }
        }
        
        if (operation == Operation.S_LIKE) {
            pattern = Pattern.compile(strPattern.length() == 0 ? "^$" : strPattern);
        }
        else if (operation == Operation.EQUALS) {
            operation = (lfe == null) ? Operation.L_EQUALS : ((dfe == null) ? Operation.D_EQUALS : Operation.S_EQUALS);
        }
        else if (operation == Operation.LESSER) {
            operation = (lfe == null) ? Operation.L_LESSER : ((dfe == null) ? Operation.D_LESSER : Operation.S_LESSER);
        }
        else if (operation == Operation.LESSER_EQUALS) {
            operation = (lfe == null) ? Operation.L_LESSER_EQUALS : ((dfe == null) ? Operation.D_LESSER_EQUALS : Operation.S_LESSER_EQUALS);
        }
        else if (operation == Operation.GREATER) {
            operation = (lfe == null) ? Operation.L_GREATER : ((dfe == null) ? Operation.D_GREATER : Operation.S_GREATER);
        }
        else if (operation == Operation.GREATER_EQUALS) {
            operation = (lfe == null) ? Operation.L_GREATER_EQUALS : ((dfe == null) ? Operation.D_GREATER_EQUALS : Operation.S_GREATER_EQUALS);
        }
        else if (operation == Operation.N_EQUALS) {
            operation = (lfe == null) ? Operation.L_EQUALS : Operation.D_EQUALS;
        }
        else if (operation == Operation.N_LESSER) {
            operation = (lfe == null) ? Operation.L_LESSER : Operation.D_LESSER;
        }
        else if (operation == Operation.N_LESSER_EQUALS) {
            operation = (lfe == null) ? Operation.L_LESSER_EQUALS : Operation.D_LESSER_EQUALS;
        }
        else if (operation == Operation.N_GREATER) {
            operation = (lfe == null) ? Operation.L_GREATER : Operation.D_GREATER;
        }
        else if (operation == Operation.N_GREATER_EQUALS) {
            operation = (lfe == null) ? Operation.L_GREATER_EQUALS : Operation.D_GREATER_EQUALS;
        }
        else if (operation == Operation.D_EQUALS 
                || operation == Operation.D_LESSER
                || operation == Operation.D_LESSER_EQUALS
                || operation == Operation.D_GREATER
                || operation == Operation.D_GREATER_EQUALS) {
            if (dfe != null) {
                throw dfe;
            }
        }
        else if (operation == Operation.L_EQUALS 
                || operation == Operation.L_LESSER
                || operation == Operation.L_LESSER_EQUALS
                || operation == Operation.L_GREATER
                || operation == Operation.L_GREATER_EQUALS) {
            if (lfe != null) {
                throw lfe;
            }
        }
    }

    public String getPattern() {
        return strValue;
    }

    public boolean isNegate() {
        return negate;
    }

    public String getProperty() {
        return property;
    }
    
    private boolean acceptOne(Object value, int propertyIndex) {
        boolean accepted = false;
        if (propertyIndex < properties.length - 1) {
            if (value == null) {
                return ifPropertyNotExists; // ожидалось, что здесь будет массив или объект
            }
            else if (value instanceof Map) {
                accepted = accept((Map<String, Object>) value, propertyIndex + 1);
            }
            else if (value instanceof List) {
                accepted = accept((List<Object>) value, propertyIndex + 1);
            }
            else if (value.getClass().isArray()) {
                accepted = accept((Object[]) value, propertyIndex + 1);
            }
            else {
                return ifPropertyNotExists; // ожидалось, что здесь будет массив или объект
            }
        }
        else {
            String val = String.valueOf(value);
            switch (operation) {
                case S_LIKE: {
                    accepted = pattern.matcher(val).find();
                    break;
                }
                case D_EQUALS: {
                    accepted = Double.parseDouble(val) == doubleValue;
                    break;
                }
                case D_LESSER: {
                    accepted = Double.parseDouble(val) < doubleValue;
                    break;
                }
                case D_LESSER_EQUALS: {
                    accepted = Double.parseDouble(val) <= doubleValue;
                    break;
                }
                case D_GREATER: {
                    accepted = Double.parseDouble(val) > doubleValue;
                    break;
                }
                case D_GREATER_EQUALS: {
                    accepted = Double.parseDouble(val) >= doubleValue;
                    break;
                }
                case L_EQUALS: {
                    accepted = Long.parseLong(val) == longValue;
                    break;
                }
                case L_LESSER: {
                    accepted = Long.parseLong(val) < longValue;
                    break;
                }
                case L_LESSER_EQUALS: {
                    accepted = Long.parseLong(val) <= longValue;
                    break;
                }
                case L_GREATER: {
                    accepted = Long.parseLong(val) > longValue;
                    break;
                }
                case L_GREATER_EQUALS: {
                    accepted = Long.parseLong(val) >= longValue;
                    break;
                }
                case S_EQUALS: {
                    accepted = strValue.equals(val);
                    break;
                }
                case S_LESSER: {
                    accepted = strValue.compareTo(val) > 0;
                    break;
                }
                case S_LESSER_EQUALS: {
                    accepted = strValue.compareTo(val) >= 0;
                    break;
                }
                case S_GREATER: {
                    accepted = strValue.compareTo(val) < 0;
                    break;
                }
                case S_GREATER_EQUALS: {
                    accepted = strValue.compareTo(val) <= 0;
                    break;
                }
            }
            if (negate) {
                accepted = !accepted;
            }
        }
        return accepted;
    }

    private boolean acceptAll(Collection values, int propertyIndex) {
        for (Object value : values) {
            if (!acceptOne(value, propertyIndex)) {
                return false;
            }
        }
        return values.isEmpty() ? ifPropertyNotExists : true;
    }

    private boolean acceptAny(Collection values, int propertyIndex) {
        for (Object value : values) {
            if (acceptOne(value, propertyIndex)) {
                return true;
            }
        }
        return values.isEmpty() ? ifPropertyNotExists : false;
    }

    private boolean acceptAll(Object[] values, int propertyIndex) {
        for (Object value : values) {
            if (!acceptOne(value, propertyIndex)) {
                return false;
            }
        }
        return values.length == 0 ? ifPropertyNotExists : true;
    }

    private boolean acceptAny(Object[] values, int propertyIndex) {
        for (Object value : values) {
            if (acceptOne(value, propertyIndex)) {
                return true;
            }
        }
        return values.length == 0 ? ifPropertyNotExists : false;
    }

    private boolean accept(Map<String, Object> record, int propertyIndex) {
        PropertyPart prop = properties[propertyIndex];
        if (prop.forArray) {
            return ifPropertyNotExists; // ожидалось, что здесь будет объект, а не массив
        }
        boolean result;
        if (prop.isAll) {
            // условие должно выполняться для всех свойств
            result = acceptAll(record.values(), propertyIndex);
        }
        else if (prop.isAny) {
            // условие должно выполняться хотя бы для одного свойства
            result = acceptAny(record.values(), propertyIndex);
        }
        else {
            // условие должно выполняться для одного указанного свойства
            Object value = record.get(prop.name);
            if (value == null || "".equals(value)) {
                return ifPropertyNotExists;
            }
            result = acceptOne(value, propertyIndex);
        }
        return result;
    }
    
    private boolean accept(List<Object> record, int propertyIndex) {
        PropertyPart prop = properties[propertyIndex];
        if (!prop.forArray) {
            return ifPropertyNotExists; // ожидалось, что здесь будет массив, а не объект
        }
        boolean result;
        if (prop.isAll) {
            // условие должно выполняться для всех элементов массива
            result = acceptAll(record, propertyIndex);
        }
        else if (prop.isAny) {
            // условие должно выполняться хотя бы для одного элемента массива
            result = acceptAny(record, propertyIndex);
        }
        else {
            // условие должно выполняться для одного указанного элемента массива
            if (prop.asInt >= record.size()) {
                return ifPropertyNotExists; // нет такого элемента массива
            }
            Object value = record.get(prop.asInt);
            if (value == null) {
                value = "";
            }
            result = acceptOne(value, propertyIndex);
        }
        return result;
    }
    
    private boolean accept(Object[] record, int propertyIndex) {
        PropertyPart prop = properties[propertyIndex];
        if (!prop.forArray) {
            return ifPropertyNotExists; // ожидалось, что здесь будет массив, а не объект
        }
        boolean result;
        if (prop.isAll) {
            // условие должно выполняться для всех элементов массива
            result = acceptAll(record, propertyIndex);
        }
        else if (prop.isAny) {
            // условие должно выполняться хотя бы для одного элемента массива
            result = acceptAny(record, propertyIndex);
        }
        else {
            // условие должно выполняться для одного указанного элемента массива
            if (prop.asInt >= record.length) {
                return ifPropertyNotExists; // нет такого элемента массива
            }
            Object value = record[prop.asInt];
            if (value == null) {
                value = "";
            }
            result = acceptOne(value, propertyIndex);
        }
        return result;
    }
    
    @Override
    public boolean accept(Map<String, Object> record) {
        return accept(record, 0);
    }
    
    public boolean accept(List<Object> record) {
        return accept(record, 0);
    }
    
    public boolean accept(Object[] record) {
        return accept(record, 0);
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("property", property).
                append("negate", negate).
                append("operation", getOperation()).
                append("pattern", pattern).
                append("if-not-exists", ifPropertyNotExists).
                toString();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
    public String getOperation() {
        return Operation.toString(operation);
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
        final FilterValue other = (FilterValue) obj;
        if (source != null && source == other.source 
                || source != null && source == other 
                || other.source != null && other.source == this) {
            return true;
        }
        if (negate != other.negate) {
            return false;
        }
        if (ifPropertyNotExists != other.ifPropertyNotExists) {
            return false;
        }
        if (!Objects.equals(property, other.property)) {
            return false;
        }
    //  assert pattern != null;
    //  assert other.pattern != null;
        return Objects.equals(pattern.pattern(), other.pattern.pattern());
    }
}
