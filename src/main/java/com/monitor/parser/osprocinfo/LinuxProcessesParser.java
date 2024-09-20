package com.monitor.parser.osprocinfo;

/*
 * Copyright 2024 Sergei Silchenko
 * Copyright 2024 Aleksei Andreev
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
import com.monitor.parser.ConsoleParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxProcessesParser implements ConsoleParser {
    
    private final static Pattern PORT_PATTERN = Pattern.compile("-port\\s+(\\d+)");
    private final static Pattern REGPORT_PATTERN = Pattern.compile("-regport\\s+(\\d+)");
    
    private final List<ProcessInfo> processes = new ArrayList<>();

    public static class ProcessInfo {

        private final String name;
        private String parentPid;
        private final String pid;
        private final String port;
        private final String regPort;
        private final String commandLine;
        private final List<ProcessInfo> children = new ArrayList<>();

        ProcessInfo(String name, String parentPid, String pid, String port, String regPort, String commandLine) {
            this.name = name;
            this.parentPid = parentPid;
            this.pid = pid;
            this.port = port;
            this.regPort = regPort;
            this.commandLine = commandLine;
        }

        public void setParentPid(String parentPid) {
            this.parentPid = parentPid;
        }

        public String getName() {
            return name;
        }

        public String getParentPid() {
            return parentPid;
        }

        public String getPid() {
            return pid;
        }

        public String getPort() {
            return port;
        }

        public String getRegPort() {
            return regPort;
        }

        public String getCommandLine() {
            return commandLine;
        }

        public List<ProcessInfo> getChildren() {
            return children;
        }
    }

    @Override
    public List<ProcessInfo> parse() throws IOException, InterruptedException {
        getRawData();
        findParentPids();
        buildProcessesTree();

        return processes;
    }

    private void getRawData() throws IOException, InterruptedException {
        Process psProcess = Runtime.getRuntime().exec("ps -ef");
        
        try (BufferedReader psReader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
            String psRecord;
            
            while ((psRecord = psReader.readLine()) != null) {
                if (!psRecord.contains("ragent") && !psRecord.contains("rmngr") && !psRecord.contains("rphost")) {
                    continue;
                }
                
                String[] parts = psRecord.trim().split("\\s+");
                for (int i = 8; i < parts.length - 1; i++) {
                    parts[7] += " " + parts[i];
                }
                if (parts.length > 1) {
                    String pid = parts[1];
                    String parentPid = parts[2];
                    String port = "";
                    String regPort = "";
                    String commandLine = parts[7];

                    if (psRecord.contains("ragent")) {
                        Matcher matcher = PORT_PATTERN.matcher(commandLine);
                        if (matcher.find()) {
                            port = matcher.group(1);
                        }

                        processes.add(new ProcessInfo("ragent", null, pid, port, null, commandLine));
                    }
                    else if (psRecord.contains("rmngr")) {
                        Matcher matcher = PORT_PATTERN.matcher(commandLine);
                        if (matcher.find()) {
                            port = matcher.group(1);
                        }

                        processes.add(new ProcessInfo("rmngr", parentPid, pid, port, null, commandLine));
                    }
                    else if (psRecord.contains("rphost")) {
                        Matcher matcher = REGPORT_PATTERN.matcher(commandLine);
                        if (matcher.find()) {
                            regPort = matcher.group(1);
                        }

                        processes.add(new ProcessInfo("rphost", null, pid, null, regPort, commandLine));
                    }
                }
            }
        }
        psProcess.waitFor();
    }

    private void findParentPids() {
        for (ProcessInfo process : processes) {
            if (process.getName().equals("rphost")) {
                for (ProcessInfo bufProcess : processes) {
                    if (bufProcess.getName().equals("rmngr")) {
                        if (process.getRegPort().equals(bufProcess.getPort())) {
                            process.setParentPid(bufProcess.getPid());
                        }
                    }
                }
            }
        }
    }

    private void buildProcessesTree() {
        Map<String, ProcessInfo> processMap = new HashMap<>();
        for (ProcessInfo process : processes) {
            processMap.put(process.getPid(), process);
        }

        List<ProcessInfo> rootProcesses = new ArrayList<>();
        for (ProcessInfo process : processes) {
            if (process.getParentPid() == null || !processMap.containsKey(process.getParentPid())) {
                rootProcesses.add(process);
            }
            else {
                ProcessInfo parent = processMap.get(process.getParentPid());
                parent.getChildren().add(process);
            }
        }

        processes.clear();
        processes.addAll(rootProcesses);
    }
}
