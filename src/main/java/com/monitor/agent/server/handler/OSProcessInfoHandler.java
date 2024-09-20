package com.monitor.agent.server.handler;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.parser.ConsoleParser;
import com.monitor.parser.osprocinfo.EmptyProcessesParser;
import com.monitor.parser.osprocinfo.WindowsProcessesParser;
import com.monitor.parser.osprocinfo.LinuxProcessesParser;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

import fi.iki.elonen.router.RouterNanoHTTPD;
import java.io.IOException;
import java.util.Map;

public class OSProcessInfoHandler extends DefaultResponder {

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public Response get(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {

        super.get(uriResource, urlParams, session);
        
        ConsoleParser consoleParser;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("win")) {
            consoleParser = new WindowsProcessesParser();
        }
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            consoleParser = new LinuxProcessesParser();
        }
        else {
            consoleParser = new EmptyProcessesParser();
        }
        
        ObjectMapper mapper = new ObjectMapper();

        try {
            Object result = consoleParser.parse();
            return NanoHTTPD.newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        
        }
        catch (IOException | InterruptedException ex) {
            setStatus(Response.Status.INTERNAL_ERROR);
            setException(ex);
            return super.get(uriResource, urlParams, session);
        }
    }

    @Override
    public Response post(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

}
