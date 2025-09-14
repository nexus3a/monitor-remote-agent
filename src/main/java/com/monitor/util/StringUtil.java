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

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Aleksei Andreev
 */
public class StringUtil {
    
    
    private static final Pattern UNPRINTABLE_PATTERN = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]");    
    private static final Charset UTF8 = Charset.forName("UTF-8");


    public static String right(String str, int count) {
        return str.substring(str.length() - count);
    }

    
    private static int indexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int left = 0;
        while (left < length) {
            char ch = (char) (value[left] & 0xff);
            if (ch != ' ' && ch != '\t' && !Character.isWhitespace(ch)) {
                break;
            }
            left++;
        }
        return left;
    }

    
    private static int lastIndexOfNonWhitespace(byte[] value) {
        int length = value.length;
        int right = length;
        while (0 < right) {
            char ch = (char) (value[right - 1] & 0xff);
            if (ch != ' ' && ch != '\t' && !Character.isWhitespace(ch)) {
                break;
            }
            right--;
        }
        return right;
    }

    
    public static String strip(String value) {
        byte[] btvalue = value.getBytes(UTF8);
        int left = indexOfNonWhitespace(btvalue);
        if (left == btvalue.length) {
            return "";
        }
        int right = lastIndexOfNonWhitespace(btvalue);
        return ((left > 0) || (right < btvalue.length)) ? new String(btvalue, left, right - left, UTF8) : value;
    }

    
    public static String lastLine(String str) {
        int markerPos = str.length();
        while (true) {
            while (--markerPos >= 0 && str.charAt(markerPos) == '\n') {
            }
            if (markerPos < 0) {
                return "";
            }
            int last = markerPos;
            markerPos = str.lastIndexOf('\n', last) + 1;
            String result = strip(str.substring(markerPos, last + 1));
            if (!result.isEmpty()) {
                return result;
            }
        }
    }

    
    public static String getRidOfUnprintables(String str) {
        Matcher matcher = UNPRINTABLE_PATTERN.matcher(str);
        return matcher.replaceAll("?");
    }

    
}
