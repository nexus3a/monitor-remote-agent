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
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public enum LogFormat {
    
    ONE_C_TECH_LOG,
    ONE_C_REG_LOG,
    PERFOMANCE_MONITOR, 
    UNKNOWN;
    
    private static final Pattern TL_PATTERN = Pattern.compile("\\d\\d:\\d\\d.\\d\\d\\d\\d(\\d\\d)?-(\\d)+,");
    private static final Pattern RL_PATTERN = Pattern.compile("1CV8LOG\\(ver 2\\.0\\)");
    private static final Pattern PM_PATTERN = Pattern.compile("(PDH-TSV )|(PDH-CSV )");
    private static final int MAX_SCAN_BYTES = 128;

    private static LogFormat determine(File file, Charset charset) {
        byte[] buf = new byte[MAX_SCAN_BYTES];
        int bytesRead = 0;
        long rafLength = 0;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            rafLength = raf.length();
            bytesRead = raf.read(buf, 0, (int) Math.min(rafLength, MAX_SCAN_BYTES));
        }
        catch (FileNotFoundException ex) {
            Logger.getLogger(LogFormat.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex) {
            Logger.getLogger(LogFormat.class.getName()).log(Level.SEVERE, null, ex);
        }
        String read = new String(buf, 0, bytesRead, charset);
        if (TL_PATTERN.matcher(read).find()) {
            return LogFormat.ONE_C_TECH_LOG;
        }
        else if (RL_PATTERN.matcher(read).find()) {
            return LogFormat.ONE_C_REG_LOG;
        }
        else if (PM_PATTERN.matcher(read).find()) {
            return LogFormat.PERFOMANCE_MONITOR;
        }
        else if (rafLength > MAX_SCAN_BYTES) {
            return LogFormat.UNKNOWN;
        }
        return null;
    }
    
    public static LogFormat determine(File file) {
        LogFormat format = determine(file, StandardCharsets.UTF_8);
        if (format == null) {
            format = determine(file, StandardCharsets.US_ASCII);
        }
        return format;
    }
    
}
