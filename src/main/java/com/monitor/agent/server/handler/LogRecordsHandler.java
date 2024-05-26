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
import com.monitor.agent.server.Registrar;
import com.monitor.agent.server.Section;
import com.monitor.agent.server.Server;
import com.monitor.agent.server.filter.Filter;
import com.monitor.agent.server.piped.ParserPipedOutputStream;
import com.monitor.agent.server.piped.ParserPipedStream;
import com.monitor.parser.ParserParameters;
import com.monitor.parser.reader.ParserFileReader;
import com.monitor.parser.reader.ParserListStorage;
import com.monitor.parser.reader.ParserRecordsStorage;
import com.monitor.parser.reader.ParserStreamStorage;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class LogRecordsHandler extends DefaultResponder {

    private static final int PARSE_EXEC_TIMEOUT = 10 * 60 * 1000; // 10 минут
    private static final String SECTION_LOCKED_MESSAGE = "SECTION_LOCKED";

    @Override
    @SuppressWarnings({"Convert2Lambda", "UseSpecificCatch", "CallToPrintStackTrace"})
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {

        super.get(uriResource, urlParams, session);

        Server server = uriResource.initParameter(Server.class);
        server.waitForUnpause(); // ожидания снятия сервера с паузы

        final ParserPipedStream pipe;

        try {

            pipe = new ParserPipedStream(PARSE_EXEC_TIMEOUT);
            server.getExecutor().execute(() -> {
            
                ParserFileReader reader = null;
                ParserPipedOutputStream output = pipe.getOutput();

                try {

                    ParserRecordsStorage storage = new ParserStreamStorage(output, PARSE_EXEC_TIMEOUT / 2);
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

                    // получаем максимальную длину токена, которую агент будет возвращать клиенту;
                    // в случае, если в файле токен (значение параметра) будет более указанной длины
                    // в символах, то вернётся значение в виде "значение-параметра-(... ещё N симв.)";
                    // используем значение 0 для указания того, что ограничений на длину нет
                    //
                    int maxTokenLen = Integer.parseInt((String) parameters.get("max-token-length", "-1"));
                    maxTokenLen = (maxTokenLen == -1) ? ParserParameters.MAX_TOKEN_LENGTH : maxTokenLen;

                    // получаем задержку времени, которую можно использовать для уменьшения нагрузки
                    // на процессор при разборе файлов лога - чем больше задержка, тем меньше нагрузка,
                    // но и скорость разбора увеличивается; задержка = 0 - то же самое, что без задержки,
                    // задержка более 5мс опасна - производительность резко падает
                    //
                    int delay = Integer.parseInt((String) parameters.get("delay", "0"));

                    // создаём структуру с дополнительными данными, которую можно
                    // передать парсеру
                    //
                    ParserParameters parserParams = new ParserParameters();
                    parserParams.setExcludeData(excludes);
                    parserParams.setMaxTokenLength(maxTokenLen);
                    parserParams.setDelay(delay);

                    // получаем секцию, в пределах которой нужно получать данные из лог-файлов
                    //
                    String sectionName = (String) parameters.get("section", (String) null);
                    Section section = Section.byName(sectionName);

                    // агент блокирует параллельный доступ к одной и той же секции данных из разных запросов;
                    // если требуется установить глобальный запрет на параллельную обработку даже разных
                    // секций, то используется параметр "global-lock"
                    //
                    boolean globalLock = !"false".equalsIgnoreCase((String) parameters.get("global-lock", "false"));
                    Lock lock = (section == null || globalLock) ? server.getLock() : section.getLock();

                    if (lock.tryLock()) {
                        // удалось заблокировать секцию - читаем данные из неё
                        //
                        try {
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

                            // читаем новые записи и в случае использования хранилища ParserStreamStorage сразу
                            // выводим их в поток; в случае использования хранилища вида ParserListStorage вывод
                            // в поток будет в отдельном цикле, ниже
                            //
                            if (storage instanceof ParserStreamStorage) {
                                output.write("[\n".getBytes(StandardCharsets.UTF_8));
                            }
                            reader = new ParserFileReader(maxRecords, filter, draft, parserParams, storage);
                            for (FileWatcher watcher : watchers) {
                                synchronized (watcher) {
                                    watcher.checkFiles();
                                    int recordsRead = watcher.readFiles(reader);
                                    if (recordsRead >= maxRecords) {
                                        break;
                                    }
                                }
//                                  System.gc();
                            }
                            if (storage instanceof ParserStreamStorage) {
                                output.write("]".getBytes(StandardCharsets.UTF_8));
                            }
                        }
                        finally {
                            lock.unlock();
                        }

                        // здесь вывод в поток записей, если используется хранилище ParserListStorage:
                        // записи выводятся после накопления в хранилище, а не в процессе разбора
                        //
                        if (storage instanceof ParserListStorage && reader != null) {
                            List<byte[]> records = reader.getRecords();
                            final byte[] comma = ",\n".getBytes(StandardCharsets.UTF_8);
                            output.write("[\n".getBytes(StandardCharsets.UTF_8));
                            for (byte[] record : records) {
                                output.write(record);
                                output.write(comma);
                            }
                            output.write("]".getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    else {
                        // не удалось заблокировать секцию для получения данных -
                        // выведем в поток служебную запись
                        //
                        output.write(SECTION_LOCKED_MESSAGE.getBytes(StandardCharsets.UTF_8));
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    try {
                        output.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
                    }
                    catch (IOException iex) {
                    }
                }
                finally {
                    try {
                        if (reader != null) {
                            reader.done();
                        }
                    }
                    catch (Throwable ex) {
                    }
                    finally {
                        try {
                            pipe.getInput().close(); // закроет pipe, а тот, в свою очередь, закроет output
//                          pipe.close(); // закрывается при закрытии output, а output закрывает chunkedResponse
                        }
                        catch (IOException ex) {
                        }
                        finally {
                            System.gc();
                        }
                    }

                }

            });
            
            /*

            (new Thread() {
                @Override
                @SuppressWarnings("UseSpecificCatch")
                public void run() {
                    ParserFileReader reader = null;
                    ParserPipedOutputStream output = pipe.getOutput();

                    try {

                        ParserRecordsStorage storage = new ParserStreamStorage(output, PARSE_EXEC_TIMEOUT / 2);
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

                        // получаем максимальную длину токена, которую агент будет возвращать клиенту;
                        // в случае, если в файле токен (значение параметра) будет более указанной длины
                        // в символах, то вернётся значение в виде "значение-параметра-(... ещё N симв.)";
                        // используем значение 0 для указания того, что ограничений на длину нет
                        //
                        int maxTokenLen = Integer.parseInt((String) parameters.get("max-token-length", "-1"));
                        maxTokenLen = (maxTokenLen == -1) ? ParserParameters.MAX_TOKEN_LENGTH : maxTokenLen;

                        // получаем задержку времени, которую можно использовать для уменьшения нагрузки
                        // на процессор при разборе файлов лога - чем больше задержка, тем меньше нагрузка,
                        // но и скорость разбора увеличивается; задержка = 0 - то же самое, что без задержки,
                        // задержка более 5мс опасна - производительность резко падает
                        //
                        int delay = Integer.parseInt((String) parameters.get("delay", "0"));

                        // создаём структуру с дополнительными данными, которую можно
                        // передать парсеру
                        //
                        ParserParameters parserParams = new ParserParameters();
                        parserParams.setExcludeData(excludes);
                        parserParams.setMaxTokenLength(maxTokenLen);
                        parserParams.setDelay(delay);
                        
                        // получаем секцию, в пределах которой нужно получать данные из лог-файлов
                        //
                        String sectionName = (String) parameters.get("section", (String) null);
                        Section section = Section.byName(sectionName);
                        
                        // агент блокирует параллельный доступ к одной и той же секции данных из разных запросов;
                        // если требуется установить глобальный запрет на параллельную обработку даже разных
                        // секций, то используется параметр "global-lock"
                        //
                        boolean globalLock = !"false".equalsIgnoreCase((String) parameters.get("global-lock", "false"));
                        Lock lock = (section == null || globalLock) ? server.getLock() : section.getLock();

                        if (lock.tryLock()) {
                            // удалось заблокировать секцию - читаем данные из неё
                            //
                            try {
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

                                // читаем новые записи и в случае использования хранилища ParserStreamStorage сразу
                                // выводим их в поток; в случае использования хранилища вида ParserListStorage вывод
                                // в поток будет в отдельном цикле, ниже
                                //
                                if (storage instanceof ParserStreamStorage) {
                                    output.write("[\n".getBytes(StandardCharsets.UTF_8));
                                }
                                reader = new ParserFileReader(maxRecords, filter, draft, parserParams, storage);
                                for (FileWatcher watcher : watchers) {
                                    synchronized (watcher) {
                                        watcher.checkFiles();
                                        int recordsRead = watcher.readFiles(reader);
                                        if (recordsRead >= maxRecords) {
                                            break;
                                        }
                                    }
//                                  System.gc();
                                }
                                if (storage instanceof ParserStreamStorage) {
                                    output.write("]".getBytes(StandardCharsets.UTF_8));
                                }
                            }
                            finally {
                                lock.unlock();
                            }

                            // здесь вывод в поток записей, если используется хранилище ParserListStorage:
                            // записи выводятся после накопления в хранилище, а не в процессе разбора
                            //
                            if (storage instanceof ParserListStorage && reader != null) {
                                List<byte[]> records = reader.getRecords();
                                final byte[] comma = ",\n".getBytes(StandardCharsets.UTF_8);
                                output.write("[\n".getBytes(StandardCharsets.UTF_8));
                                for (byte[] record : records) {
                                    output.write(record);
                                    output.write(comma);
                                }
                                output.write("]".getBytes(StandardCharsets.UTF_8));
                            }
                        }
                        else {
                            // не удалось заблокировать секцию для получения данных -
                            // выведем в поток служебную запись
                            //
                            output.write(SECTION_LOCKED_MESSAGE.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
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
//                  System.gc();
                }
            }).start();
            */

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
