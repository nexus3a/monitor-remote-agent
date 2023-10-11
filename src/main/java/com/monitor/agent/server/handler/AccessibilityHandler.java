package com.monitor.agent.server.handler;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.agent.server.DirectoryWatcher;
import com.monitor.agent.server.Server;
import com.monitor.agent.server.config.ConfigurationManager;
import com.monitor.agent.server.config.FilesConfig;
import com.monitor.agent.server.config.OneCServerConfig;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;

public class AccessibilityHandler extends DefaultResponder {
    
    private static final String TEST_FILE_NAME = ".monitor-remote-agent-test";
    private static final String AGENT_WORKDIR_MASK = "\\$\\{agent-path\\}";
    private static final String AGENT_CONFIG_MASK = "\\$\\{agent-config\\}";
    
    private static class PathCheckResult {
        String wildcard;
        String path;
        boolean unexpected = false;
        boolean exists = false;
        boolean created = false;
        boolean read = false;
        boolean write = false;
        String exception = "";
        String testfile = "";

        public String getWildcard() {
            return wildcard;
        }

        public String getPath() {
            return path;
        }

        public boolean isUnexpected() {
            return unexpected;
        }

        public boolean isExists() {
            return exists;
        }

        public boolean isCreated() {
            return created;
        }

        public boolean isRead() {
            return read;
        }

        public boolean isWrite() {
            return write;
        }

        public String getException() {
            return exception;
        }

        public String getTestfile() {
            return testfile;
        }
        
    }
    
    private static class SectionTests {
        String section;
        List<List<PathCheckResult>> tests = new ArrayList<>();

        public String getSection() {
            return section;
        }

        public List<List<PathCheckResult>> getTests() {
            return tests;
        }
    }
    
    private static class OneCServerPathsTests {
        @JsonProperty("address")
        String server1c;
        List<SectionTests> sections = new ArrayList<>();
        @JsonProperty("logcfg path test")
        PathCheckResult logCfgPathTest;

        public String getServer1c() {
            return server1c;
        }

        public List<SectionTests> getSections() {
            return sections;
        }

        public PathCheckResult getLogCfgPathTest() {
            return logCfgPathTest;
        }
    }
    
    private static class ConfigTests {
        List<List<PathCheckResult>> tests = new ArrayList<>();
        @JsonProperty("1c servers")
        List<OneCServerPathsTests> servers = new ArrayList<>();
        @JsonProperty("agent path test")
        PathCheckResult agentPathTest;

        public List<List<PathCheckResult>> getTests() {
            return tests;
        }

        public List<OneCServerPathsTests> getServers() {
            return servers;
        }

        public PathCheckResult getAgentPathTest() {
            return agentPathTest;
        }
    }
    
    private static String getTestFileName(File directory, String testFileWildcard) {
        if (testFileWildcard == null 
                || testFileWildcard.isEmpty() 
                || "*.*".equals(testFileWildcard)) {
            String fileName = TEST_FILE_NAME;
            for (int suffix = 0; suffix < 1000; suffix++) {
                File file = new File(directory, fileName);
                if (!file.exists()) {
                    break;
                }
                fileName = TEST_FILE_NAME + "-" + String.valueOf(suffix);
            }
            return fileName;
        }
        if (testFileWildcard.contains("*") || testFileWildcard.contains("?")) {
            String fileName = null;
            for (int suffix = 0; suffix < 1000; suffix++) {
                fileName = testFileWildcard.replaceAll("\\*", String.valueOf(suffix)).replaceAll("\\?", "_");
                File file = new File(directory, fileName);
                if (!file.exists()) {
                    break;
                }
            }
            return fileName;
        }
        return testFileWildcard;
    }
    
    private static List<PathCheckResult> checkWildcard(String filesToWatch) {

        List<PathCheckResult> results = new ArrayList<>();

        String directoryWildcard = FilenameUtils.getFullPath(filesToWatch);
        String testFileName = FilenameUtils.getName(filesToWatch);

        DirectoryWatcher watcher = new DirectoryWatcher();
        Set<File> directories = watcher.addWildCardDirectories(directoryWildcard, null, null, null, "");
        if (directories == null) {
            List<String> wildCardParts = watcher.getWildCardParts(directoryWildcard);
            if (!wildCardParts.isEmpty()) {
                directories = new HashSet<>(1);
                directories.add(new File(wildCardParts.get(0)));
            }
        }
        if (directories != null) {
            for (File directory : directories) {
                PathCheckResult result = checkPath(directory.getPath(), testFileName);
                result.wildcard = FilenameUtils.separatorsToSystem(filesToWatch);
                results.add(result);
            }
        }
        return results;
    }

    private static PathCheckResult checkPath(String directoryName, String testFileName) {
        
        PathCheckResult result = new PathCheckResult();
        result.path = FilenameUtils.separatorsToSystem(directoryName + "/" + testFileName);
        try {
            
            boolean testFileNameIsWild = testFileName.isEmpty() 
                    || (testFileName.contains("*") 
                    || testFileName.contains("?"));

            File directory = new File(directoryName);
            result.exists = directory.exists();

            if (!result.exists) {
                result.created = directory.mkdirs();
                if (!result.created) {
                    return result;
                }
            }

            result.unexpected = !directory.isDirectory();
            if (result.unexpected) {
                if (!testFileNameIsWild) {
                    // директория вовсе и не директория, а файл - значит, он не может содержать
                    // подчиненные файлы, в том числе и файл с именем testFileName
                    result.exists = false;
                }
                return result;
            }

            boolean deleteTest = false;

            result.testfile = getTestFileName(directory, testFileName);
            File test = new File(directory, result.testfile);
            try {
                // todo: SecurityManager
                // SecurityManager sm = System.getSecurityManager();
                // sm.checkPermission(new FilePermission("path-to-file", "read|write|delete"));
                deleteTest = test.createNewFile();
            }
            catch (IOException ex) {
                result.exception = ex.getMessage();
            }
            
            result.unexpected = !test.isFile();
            if (test.exists() && !result.unexpected) {
                result.read = test.canRead();
                result.write = test.canWrite();
            }

            if (deleteTest) {
                test.delete();
            }

            if (!result.exists && result.created) {
                directory.delete();
            }
            
            if (!testFileNameIsWild) {
                result.exists = !deleteTest;
                result.created = deleteTest;
            }
        }
        catch (Exception ex) {
            if (result.exception.isEmpty()) {
                result.exception = ex.getMessage();
            }
        }
        
        return result;
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {
        
        super.get(uriResource, urlParams, session);
        
        Server server = uriResource.initParameter(Server.class);
        server.waitForUnpause(); // ожидания снятия сервера с паузы

        try {
            
            RequestParameters parameters = getParameters();

            // получаем имя секции, в пределах которой нужно анализировать каталоги
            //
            String section = (String) parameters.get("section", null);

            // получаем имя сервера, в пределах которого нужно анализировать каталоги
            //
            String server1c = (String) parameters.get("1cserver", null);

            // получаем шаблон имени файлового ресурса (файла или каталога), который 
            // нужно проанализировать; шаблон, заканчивающийся на "/", считается каталогом, 
            // в противном случае - файлом; в шаблоне допустимы "*"
            //
            String wildcard = (String) parameters.get("file", null);
            if (wildcard != null && wildcard.isEmpty()) {
                throw new IllegalArgumentException("empty \"file\" parameter");
            }

            if (wildcard != null) {
                wildcard = wildcard.replaceAll(AGENT_WORKDIR_MASK, new File("").getAbsolutePath().replaceAll("\\\\", "/"));
                wildcard = wildcard.replaceAll(AGENT_CONFIG_MASK, new File(server.getConfig()).getAbsolutePath().replaceAll("\\\\", "/"));
                ObjectMapper mapper = new ObjectMapper();
                return NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json",
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(checkWildcard(wildcard)));
            }
            
            // если шаблон имени не задан в параметрах запроса, то проверяем все пути
            // во всех секциях настроек серверов 1С, из корня конфигурации агента,
            // а таже к каталогу агента
            //
            ConfigTests configTests = new ConfigTests();
            
            File agentConfig = new File(server.getConfig());
            configTests.agentPathTest = checkPath(agentConfig.getParent(), agentConfig.getName());
        
            ConfigurationManager configManager = new ConfigurationManager(server.getConfig());
            configManager.readConfiguration();

            // настройки логов из корня конфигурации
            List<FilesConfig> filesConfigs = configManager.getConfig().getFiles();
            if (filesConfigs != null) {
                for (FilesConfig files : filesConfigs) {
                    if (section == null || section.equalsIgnoreCase(files.getSection())) {
                        for (String path : files.getPaths()) {
                            configTests.tests.add(checkWildcard(path));
                        }
                    }
                }
            }

            List<OneCServerConfig> serversConfig = configManager.getConfig().getOneCServers();
            if (serversConfig != null) {
                for (OneCServerConfig oneCServer : serversConfig) {
                    // настройки логов из узлов настроек серверов 1С
                    if (server1c == null || server1c.equalsIgnoreCase(oneCServer.getAddress())) {
                        OneCServerPathsTests serverTests = new OneCServerPathsTests();
                        serverTests.server1c = oneCServer.getAddress();
                        filesConfigs = oneCServer.getFiles();
                        if (filesConfigs != null) {
                            for (FilesConfig files : filesConfigs) {
                                if (section == null || section.equalsIgnoreCase(files.getSection())) {
                                    SectionTests sectionTests = new SectionTests();
                                    sectionTests.section = files.getSection();
                                    for (String path : files.getPaths()) {
                                        sectionTests.tests.add(checkWildcard(path));
                                    }
                                    serverTests.sections.add(sectionTests);
                                }
                            }
                        }
                        // проверка возможности изменить logcfg
                        serverTests.logCfgPathTest = checkPath(oneCServer.getLogCfgPath(), "logcfg.xml");
                        serverTests.logCfgPathTest.wildcard = oneCServer.getLogCfgPath();

                        configTests.servers.add(serverTests);
                    }
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/json",
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configTests));
        
        }
        catch (Exception ex) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    NanoHTTPD.MIME_PLAINTEXT,
                    ex.getMessage());
        }
    }

    @Override
    public NanoHTTPD.Response post(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

}
