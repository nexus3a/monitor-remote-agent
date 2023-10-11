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

import com.monitor.agent.server.Server;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.io.IOException;
import java.util.Map;

public class StopServerHandler extends DefaultResponder {

    @Override
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {

        super.get(uriResource, urlParams, session);
        
        (new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1 * 1000L);
                    Server server = uriResource.initParameter(Server.class);
                    server.stopServer();
                }
                catch (IOException | InterruptedException ex) {
                }
            }
        }).start();

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
