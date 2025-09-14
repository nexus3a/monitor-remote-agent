/*
 * Copyright 2025 Aleksei Andreev.
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
package com.monitor.util;

import java.util.HashSet;

/**
 *
 * @author Aleksei Andreev
 */
public class ArrayUtil {
    
    
    @SuppressWarnings("ManualArrayToCollectionCopy")
    public static String[] uniqueArray(String str) {
        if (!str.contains(",")) {
            String[] result = new String[1];
            result[0] = str;
            return result;
        }
        String[] splitted = str.replaceAll("['\"]", "").split(",");
        HashSet<String> set = new HashSet(splitted.length);
        for (String s : splitted) {
            set.add(s);
        }
        if (set.size() == splitted.length) {
            return splitted;
        }
        String[] result = new String[set.size()];
        set.toArray(result);
        return result;
    }
    
    
}
