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
import com.monitor.agent.server.Server;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import com.monitor.agent.server.config.Configuration;
import com.monitor.agent.server.config.ConfigurationManager;
import java.util.Map;

public class ConfigHandler extends DefaultResponder {

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {
        
        super.get(uriResource, urlParams, session);
        
        Response response;
        
        Server server = uriResource.initParameter(Server.class);
        ConfigurationManager configManager = server.getConfigManager();
        ObjectMapper mapper = new ObjectMapper();
        
        try {

            if (!checkToken(uriResource)) {
                return badTokenResponse();
            }

            RequestParameters parameters = getParameters();

            // определяем содержимое файла настройки агента; используется только если
            // необходимо записать файл настройки с переданными данными; если он не задан, то  
            // это значит, что мы хотим прочитать содержимое файла настройки агента
            //
            String contentJson = (String) parameters.get("config", null);
            boolean writeConfig = !(contentJson == null || contentJson.isEmpty());
            
            if (writeConfig) {
                Configuration config = configManager.getConfig();
                String token = config.getToken();
                config = mapper.readValue(contentJson, Configuration.class);
                config.setToken(token);   // устанавливаем в новой конфигурации текущий токен
                config.encodePasswords(); // перед записью шифруем пароли кластера
                configManager.setConfig(config).writeConfiguration();
                server.initializeFileWatchers();
                response = NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        NanoHTTPD.MIME_PLAINTEXT,
                        "OK");
            }
            else {
                Configuration config = configManager.getConfig();
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
                response = NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json",
                        json);
            }
        
        }
        catch (Exception ex) {
            setStatus(Response.Status.INTERNAL_ERROR);
            setException(ex);
            response = super.get(uriResource, urlParams, session);
        }
        
        return response;
        
    }

    @Override
    public Response post(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

}
