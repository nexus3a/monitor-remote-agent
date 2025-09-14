package com.monitor.agent.server.handler;

/*
 * Copyright 2025 Aleksei Andreev
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
import com.monitor.agent.server.config.Configuration;
import com.monitor.agent.server.config.ConfigurationManager;
import com.monitor.util.StringUtil;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.util.Map;

public class SetTokenHandler extends DefaultResponder {

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

            if (!checkToken(uriResource)) {
                return badTokenResponse();
            }

            // если не указали параметр newtoken или он пустой, то
            // сбрасываем токен Агента
            //
            RequestParameters parameters = getParameters();
            String newToken = (String) parameters.get("newtoken", null);
            newToken = (newToken == null ? null : StringUtil.strip(newToken));
            newToken = (newToken == null ? null : (newToken.isEmpty() ? null : newToken));
            String encodedToken = (newToken == null ? null : Configuration.encodeString(newToken));
            
            ConfigurationManager configManager = server.getConfigManager();
            Configuration config = configManager.getConfig();
            String existingToken = config.getToken();

            if (existingToken != null && !existingToken.equals(encodedToken) 
                    || encodedToken != null && !encodedToken.equals(existingToken)) {
                config.setToken(encodedToken);
                configManager.setConfig(config).writeConfiguration();
            }

            return NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        NanoHTTPD.MIME_PLAINTEXT,
                        "OK");

        }
        catch (Exception ex) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
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
