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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.agent.server.FileState;
import com.monitor.agent.server.FileWatcher;
import com.monitor.agent.server.Section;
import com.monitor.agent.server.Server;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WatchMapHandler extends DefaultResponder {

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {
        
        super.get(uriResource, urlParams, session);
        
        Server server = uriResource.initParameter(Server.class);
        server.waitForUnpause(); // ожидание снятия сервера с паузы

        try {

            if (!checkToken(uriResource)) {
                return badTokenResponse();
            }

            RequestParameters parameters = getParameters();

            // получаем имя секции, в пределах которой нужно получить данные об отслеживаемых файлах
            String sectionName = (String) parameters.get("section", null);
            Collection<FileWatcher> watchers = server.getWatchers(Section.byName(sectionName));

            // создаём коллекцию отслеживаемых файлов в разрезе секций
            HashMap<String, Collection<FileState>> model = new HashMap<>();
            for (FileWatcher watcher : watchers) {
                watcher.checkFiles();
                model.put(watcher.getSection().getName(), watcher.getWatched());
            }

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    NanoHTTPD.MIME_PLAINTEXT,
                    json);
        }
        catch (Exception ex) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    NanoHTTPD.MIME_PLAINTEXT,
                    ex.getLocalizedMessage());
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
