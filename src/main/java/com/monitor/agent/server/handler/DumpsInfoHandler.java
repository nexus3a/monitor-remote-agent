package com.monitor.agent.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.agent.server.Server;
import com.monitor.agent.server.config.ConfigurationManager;
import com.monitor.agent.server.config.FilesConfig;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.monitor.agent.server.TextDB;
import com.monitor.agent.server.config.OneCServerConfig;
import java.io.File;

public class DumpsInfoHandler extends DefaultResponder {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Date MIN_DATE = new GregorianCalendar(1900, 0, 1).getTime();
    
    private static final Logger log = LoggerFactory.getLogger(DumpsInfoHandler.class);

    private class DumpInfoDto {

        private String name;
        private String size;
        private String fullPath;
        private String modifiedDate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getFullPath() {
            return fullPath;
        }

        public void setFullPath(String fullPath) {
            this.fullPath = fullPath;
        }

        public String getModifiedDate() {
            return modifiedDate;
        }

        public void setModifiedDate(String modifiedDate) {
            this.modifiedDate = modifiedDate;
        }
    }

    @Override
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {

        super.get(uriResource, urlParams, session);
        
        Server server = uriResource.initParameter(Server.class);
        server.waitForUnpause(); // ожидание снятия сервера с паузы
        
        List<DumpInfoDto> collect = new ArrayList<>();
        String result;

        try {
        
            if (!checkToken(uriResource)) {
                return badTokenResponse();
            }

            RequestParameters parameters = getParameters();
            boolean ack = !"false".equalsIgnoreCase((String) parameters.get("ack", "false"));
            String sectionName = (String) parameters.get("section", (String) null);
            if (sectionName == null) {
                throw new IllegalArgumentException("Не указан параметр section в запросе");
            }

            ObjectMapper objectMapper = new ObjectMapper();

            ConfigurationManager configManager = server.getConfigManager();

            // соберём в одну коллекцию настройки путей (paths) из корня конфигурации
            // и из настроек путей серверов 1С
            //
            List<FilesConfig> filesConfigs = new ArrayList<>();
            filesConfigs.addAll(configManager.getConfig().getFiles());
            for (OneCServerConfig oneCServer : configManager.getConfig().getOneCServers()) {
                filesConfigs.addAll(oneCServer.getFiles());
            }

            for (FilesConfig files : filesConfigs) {
                if (!sectionName.equals(files.getSection())) {
                    continue;
                }

                for (String path : files.getPaths()) {
                    Path filePath = Paths.get(path);
                    
                    File check = filePath.toFile();
                    if (!check.exists() || !check.isDirectory()) {
                        continue;
                    }

                    List<Path> filesList;
                    try (Stream<Path> stream = Files.list(filePath);) {
                        filesList = stream.collect(Collectors.toList());
                    }
                    catch (Exception e) {
                        log.error("Exception while getting files list", e);
                        throw e;
                    }
                    
                    if (filesList.isEmpty()) {
                        continue;
                    }

                    String timeDb = TextDB.SINCEDB_CAT + "/." + sectionName + TextDB.DUMPS_TIME_EXT;
                    Path timeFile = Paths.get(timeDb);
                    if (!timeFile.toFile().exists()) {
                        Files.createFile(timeFile);
                        String time = String.valueOf(MIN_DATE.getTime());
                        Files.write(timeFile, time.getBytes());
                    }
                    String time;

                    Optional<String> first = Files.readAllLines(timeFile).stream().findFirst();
                    if (first.isPresent()) {
                        time = first.get();
                    }
                    else {
                        time = String.valueOf(MIN_DATE.getTime());
                        Files.write(timeFile, time.getBytes());
                    }
                    Date date = new Date(Long.parseLong(time));
                    Date maxFileDate = MIN_DATE;

                    for (Path file : filesList) {
                        if (Files.isDirectory(file)) {
                            continue;
                        }
                        FileTime lastModifiedTime = Files.getLastModifiedTime(file);
                        if (isAfter(lastModifiedTime, date)) {
                            collect.add(createDto(file));
                            if (isAfter(lastModifiedTime, maxFileDate)) {
                                maxFileDate = toDate(lastModifiedTime);
                            }
                        }
                    }

                    if (ack && maxFileDate.after(date)) {
                        try {
                            time = String.valueOf(maxFileDate.getTime());
                            Files.write(timeFile, time.getBytes());
                        }
                        catch (IOException e) {
                            log.error("Exception handling last modified time {}", timeFile, e);
                            throw e;
                        }
                    }

                }
            }
            
            result = objectMapper.writeValueAsString(collect);
        }
        catch (Exception ex) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    ex.getClass().getName() + ": " + ex.getMessage());
        }

        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                NanoHTTPD.MIME_PLAINTEXT,
                result);
    }

    private boolean isAfter(FileTime fileTime, java.util.Date date) {
        return Date.from(fileTime.toInstant()).after(date);
    }

    private Date toDate(FileTime fileTime) {
        return Date.from(fileTime.toInstant());
    }

    private DumpInfoDto createDto(Path path) throws IOException {
        try {
            DumpInfoDto dumpInfoDto = new DumpInfoDto();
            dumpInfoDto.setName(path.getFileName().toString());
            dumpInfoDto.setFullPath(path.toString());
            Date modified = toDate(Files.getLastModifiedTime(path));
            dumpInfoDto.setModifiedDate(DATE_FORMATTER.format(modified));
            dumpInfoDto.setSize(String.valueOf(Files.size(path)));
            return dumpInfoDto;
        }
        catch (IOException e) {
            log.error("Exception while getting filePath attributes", e);
            throw e;
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
