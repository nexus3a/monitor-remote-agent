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
import com.monitor.agent.server.FileState;
import com.monitor.agent.server.FileWatcher;
import com.monitor.agent.server.ParserFileReader;
import com.monitor.agent.server.Registrar;
import com.monitor.agent.server.Section;
import com.monitor.agent.server.Server;
import com.monitor.agent.server.filter.Filter;
import com.monitor.agent.server.piped.ParserPipedOutputStream;
import com.monitor.agent.server.piped.ParserPipedStream;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LogRecordsHandler extends DefaultResponder {

    private static final int PARSE_EXEC_TIMEOUT = 10 * 60 * 1000; // 10 минут

    @Override
    @SuppressWarnings({"Convert2Lambda", "UseSpecificCatch"})
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {

        super.get(uriResource, urlParams, session);

        Server server = uriResource.initParameter(Server.class);
        server.waitForUnpause(); // ожидания снятия сервера с паузы

        ParserPipedStream pipe;

        try {

            pipe = new ParserPipedStream(PARSE_EXEC_TIMEOUT);

            (new Thread() {
                @Override
                @SuppressWarnings("UseSpecificCatch")
                public void run() {
                    ParserFileReader reader = null;
                    ParserPipedOutputStream output = pipe.getOutput();

                    try {

                        RequestParameters parameters = getParameters();

                        // получаем признак "чернового" чтения - данные читаются с самого начала,
                        // позиция завершения чтения не запоминается
                        //
                        boolean draft = !"false".equalsIgnoreCase((String) parameters.get("draft", "false"));

                        // получаем максимальное количество записей лога, которые нужно
                        // вернуть клиенту; если не указано - берём из глобальных настроек
                        //
                        int maxRecords = Integer.parseInt((String) parameters.get("max", String.valueOf(server.getSpoolSize())));

                        // получаем глобальный фильтр значений записей лог-файлов; он будет
                        // комбинироваться с фильтрами, указанными в конфигурационном файле
                        // сервера
                        //
                        Filter filter = null;
                        Object filterValue = parameters.get("filter", null);
                        if (filterValue != null) {
                            if (filterValue instanceof String) {
                                filter = Filter.fromJson((String) filterValue);
                            }
                            else if (Map.class.isAssignableFrom(filterValue.getClass())) {
                                filter = Filter.fromMap((Map<String, Object>) filterValue);
                            }
                        }

                        // получаем признак чтения полных данных по блокировкам для лог-файлов
                        // технологического журнала: если не нужны подробные данные по блокировкам,
                        // то можно сократить объём передаваемых клиенту данных
                        //
                        String strExcludes = (String) parameters.get("exclude-data", "");
                        HashSet<String> excludes = new HashSet<>();
                        excludes.addAll(Arrays.asList(strExcludes.replaceAll(" ", "").split(",")));

                        // создаём структуру с дополнительными данными, которую можно
                        // передать парсеру
                        //
                        HashMap<String, Object> parserParams = new HashMap<>();
                        parserParams.put("exclude-data", excludes);

                        // получаем секцию, в пределах которой нужно получать данные из лог-файлов
                        //
                        String sectionName = (String) parameters.get("section", (String) null);
                        Section section = Section.byName(sectionName);
                        Object sectionLock = section == null ? server : section;

                        synchronized (sectionLock) {

                            Collection<FileWatcher> watchers = server.getWatchers(section);

                            // подтверждаем факт успешного приёма предыдущей порции данных:
                            // переустанавливаем указатели файлов в рабочее положение
                            //
                            if (!"false".equalsIgnoreCase((String) parameters.get("ack", "false"))) {
                                for (FileWatcher watcher : watchers) {
                                    synchronized (watcher) {
                                        Collection<FileState> states = watcher.getWatched();
                                        for (FileState state : states) {
                                            state.setPointer(state.getNewPointer());
                                        }
                                        Registrar.writeStateToJson(watcher.getSincedbFile(), states);
                                    }
                                }
                            }

                            // читаем новые записи
                            //
                            reader = new ParserFileReader(maxRecords, filter, draft, parserParams);
                            for (FileWatcher watcher : watchers) {
                                synchronized (watcher) {
                                    watcher.checkFiles();
                                    int recordsRead = watcher.readFiles(reader);
                                    if (recordsRead >= maxRecords) {
                                        break;
                                    }
                                }
                            }
                        }

                        List<byte[]> records = reader.getRecords();
                        output.write("[\n".getBytes(StandardCharsets.UTF_8));
                        for (byte[] record : records) {
                            output.write(record);
                            output.write(",\n".getBytes(StandardCharsets.UTF_8));
                        }
                        output.write("]".getBytes(StandardCharsets.UTF_8));
                    }
                    catch (Exception ex) {
                        try {
                            output.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
                        }
                        catch (IOException iex) {
                        }
                    }
                    try {
                        if (reader != null) {
                            reader.done();
                        }
                    }
                    catch (Throwable ex) {
                    }
                    try {
                        pipe.close(); // закрывается при закрытии output, а output закрывает chunkedResponse
                    }
                    catch (IOException ex) {
                    }
                    System.gc();
                }
            }).start();

        }
        catch (Exception ex) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    ex.getMessage());
        }

        Response response = NanoHTTPD.newChunkedResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                pipe.getInput());
        response.setGzipEncoding(true);
        return response;
    }

    @Override
    public NanoHTTPD.Response post(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

}
