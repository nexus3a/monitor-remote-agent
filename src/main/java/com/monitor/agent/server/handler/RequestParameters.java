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
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestParameters {
    
    private final IHTTPSession session;
    private Map<String, Object> contentParameters;

    public RequestParameters(IHTTPSession session) {
        this.session = session;
        this.contentParameters = null;
    }
    
    public byte[] contentOf(IHTTPSession session) throws IOException {
        if (session.getMethod() == Method.GET) {
            return new byte[0];
        }
        int contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
        byte[] content = new byte[contentLength];
        InputStream input = session.getInputStream();
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            bytesRead += input.read(content, bytesRead, contentLength - bytesRead);
        }
        return content;
    }
    
    // получим параметры из тела запроса, если их передали именно в теле, а не в
    // параметрах запроса; ожидается, что параметры в теле представляют собой
    // json с именами свойств, равными именам параметров get-запроса
    //
    public Map<String, Object> contentParameters(IHTTPSession session) throws IOException {
        Map<String, Object> parameters = null;
        byte[] content = contentOf(session);
        if (content.length > 0) {
            ObjectMapper mapper = new ObjectMapper();
            parameters = mapper.readValue(content, Map.class);
        }
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        return parameters;
    }

    // возвращает значение параметра запроса: если это get-запрос, то возвращается
    // значение параметра из url; если это post-запрос, то считается, что параметры
    // преданы в виде json и возвращается значение соответствующего свойства json
    //
    public Object get(String key, Object defValue) throws IOException {
        Map<String, List<String>> sessionParameters = session.getParameters();
        if (sessionParameters.containsKey(key)) {
            return sessionParameters.get(key).get(0);
        }
        synchronized (this) {
            if (contentParameters == null) {
                contentParameters = contentParameters(session);
            }
        }
        return contentParameters.getOrDefault(key, defValue);
    }
    
}
