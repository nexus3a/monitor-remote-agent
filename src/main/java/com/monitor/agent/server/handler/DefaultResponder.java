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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.agent.server.Server;
import com.monitor.agent.server.config.Configuration;
import com.monitor.agent.server.config.ConfigurationManager;
import com.monitor.util.StringUtil;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.router.RouterNanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResponder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DefaultResponder implements UriResponder {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultResponder.class);

    private Status status;
    private String message;
    private RequestParameters parameters;

    public DefaultResponder() {
        status = Status.METHOD_NOT_ALLOWED;
        message = null;
        parameters = null;
    }

    public DefaultResponder(Status status) {
        this.status = status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setException(Throwable exception) {
        this.message = exception.getMessage();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private Response statusJson() {
        HashMap<String, Object> model = new HashMap<>();
        model.put("status", status.getRequestStatus());
        model.put("description", status.getDescription());
        model.put("message", message == null ? "" : message);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return NanoHTTPD.newFixedLengthResponse(
                    status, 
                    "application/json", 
                    mapper.writeValueAsString(model));
        }
        catch (JsonProcessingException ex) {
            return NanoHTTPD.newFixedLengthResponse(
                    status, 
                    "application/json", 
                    String.format("{ \"status\":\"%s\", \"description\":\"%s\", \"message\":\"%s\" }", 
                            status.getRequestStatus(),
                            status.getDescription(),
                            "statusJson() error"));
        }
    }
    
    private synchronized void loadParameters(NanoHTTPD.IHTTPSession session) {
        if (parameters == null) {
            parameters = new RequestParameters(session);
        }
    }
    
    public synchronized RequestParameters getParameters() {
        return parameters;
    }
    
    private void logInfo(NanoHTTPD.IHTTPSession session) {
        String section;
        try {
            section = (String) parameters.get("section", "");
        }
        catch (IOException ex) {
            section = "";
        }
        String uri = session.getUri();
        logger.info("{} {}{}", 
                session.getMethod().name(),
                uri.isEmpty() ? "/" : uri,
                section.isEmpty() ? "" : "?section=" + section);
    }
    
    public boolean checkToken(RouterNanoHTTPD.UriResource uriResource) throws IOException {
        Server server = uriResource.initParameter(Server.class);
        if (server == null) {
            return true;
        }
        ConfigurationManager configManager = server.getConfigManager();
        Configuration config = configManager.getConfig();
        String existingToken = config.getToken(); // хранится в encoded-виде
        if (existingToken == null) {
            return true;
        }

        RequestParameters params = getParameters();
        String token = (String) params.get("token", null);
        token = (token == null ? null : StringUtil.strip(token));
        token = (token == null ? null : (token.isEmpty() ? null : token));

        return Configuration.decodeString(existingToken).equals(token);
    }
    
    public Response badTokenResponse() {
        return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.UNAUTHORIZED,
                NanoHTTPD.MIME_PLAINTEXT,
                "BAD_TOKEN");
    }

    @Override
    public Response get(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        loadParameters(session);
        logInfo(session);
        return statusJson();
    }

    @Override
    public Response put(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        loadParameters(session);
        logInfo(session);
        return statusJson();
    }

    @Override
    public Response post(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        loadParameters(session);
        logInfo(session);
        return statusJson();
    }

    @Override
    public Response delete(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        loadParameters(session);
        logInfo(session);
        return statusJson();
    }

    @Override
    public Response other(
            String method,
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        loadParameters(session);
        logInfo(session);
        return statusJson();
    }

}
