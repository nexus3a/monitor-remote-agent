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
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.util.Collection;
import java.util.Map;

public class AckHandler extends DefaultResponder {

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

            // получаем секцию, в пределах которой нужно подтввердить получение данных
            //
            String sectionName = (String) parameters.get("section", null);
            Section section = Section.byName(sectionName);
            Object sectionLock = section == null ? server : section;

            synchronized (sectionLock) {
                
                Collection<FileWatcher> watchers = server.getWatchers(Section.byName(sectionName));

                // подтверждаем факт успешного приёма предыдущей порции данных:
                // переустанавливаем указатели файлов в рабочее положение
                //
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
        }
        catch (Exception ex) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    ex.getLocalizedMessage());
        }
        
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                NanoHTTPD.MIME_PLAINTEXT,
                "OK");
    }

    @Override
    public NanoHTTPD.Response post(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

}
