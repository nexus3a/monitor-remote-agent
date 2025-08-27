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
import com.monitor.agent.server.config.OneCServerConfig;
import com.monitor.agent.server.filter.Filter;
import com.monitor.parser.onec.srvinfo.OneCSrvInfo;
import com.monitor.parser.onec.srvinfo.OneCSrvInfoRecord;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.util.List;
import java.util.Map;

public class SrvInfoHandler extends DefaultResponder {

    @Override
    @SuppressWarnings({"Convert2Lambda", "UseSpecificCatch", "CallToPrintStackTrace"})
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {

        super.get(uriResource, urlParams, session);

        Server server = uriResource.initParameter(Server.class);
        server.waitForUnpause(); // ожидания снятия сервера с паузы

        Response response;

        try {

            RequestParameters parameters = getParameters();

            String serverName = (String) parameters.get("server", null);
            String catalog = (String) parameters.get("catalog", null);
            
            if (catalog == null && serverName != null) {
                // если не передали каталог srvinfo, то получим этот каталог
                // из описания сервера serverName
                //
                List<OneCServerConfig> srvConfigs = server.getConfigManager().getConfig().getOneCServers();
                for (OneCServerConfig srvConfig : srvConfigs) {
                    if (serverName.equalsIgnoreCase(srvConfig.getAddress())) {
                        catalog = srvConfig.getSrvInfoPath();
                        break;
                    }
                }
            }

            if (catalog == null) {
                return NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "application/json",
                        "{}");
            }
            
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
            
            OneCSrvInfoRecord srvInfo = OneCSrvInfo.getInfo(catalog, filter);

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(srvInfo);
            response = NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/json",
                    json);
        
        }
        catch (Exception ex) {
            setStatus(Response.Status.INTERNAL_ERROR);
            setException(ex);
            response = super.get(uriResource, urlParams, session);
        }
        
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
