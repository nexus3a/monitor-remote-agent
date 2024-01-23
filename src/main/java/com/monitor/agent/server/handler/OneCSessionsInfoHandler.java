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

import com._1c.v8.ibis.admin.ISessionInfo;
import com._1c.v8.ibis.admin.client.AgentAdminConnectorFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.agent.server.Server;
import com.monitor.agent.server.config.Configuration;
import com.monitor.agent.server.config.OneCClusterConfig;
import com.monitor.agent.server.config.OneCInfoBaseConfig;
import com.monitor.agent.server.config.OneCServerConfig;
import com.monitor.enterprise.OneCCluster;
import com.monitor.enterprise.OneCClusterInfo;
import com.monitor.enterprise.OneCInfoBase;
import com.monitor.enterprise.OneCInfoBaseInfo;
import com.monitor.enterprise.OneCRASAgent;
import com.monitor.enterprise.OneCServerInfo;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OneCSessionsInfoHandler extends DefaultResponder {
    
    private Map<String, Object> toMap(ISessionInfo info, String[] props, ObjectMapper mapper) throws JsonProcessingException {
        Map<String, Object> asMap = mapper.readValue(mapper.writeValueAsString(info), HashMap.class);
        if (props == null || props.length == 0) {
            return asMap;
        }
        Map<String, Object> result = new HashMap<>();
        for (String prop : props) {
            result.put(prop, asMap.get(prop));
        }
        return result;
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {
        
        super.get(uriResource, urlParams, session);
        
        Server httpServer = uriResource.initParameter(Server.class);
        httpServer.waitForUnpause(); // ожидания снятия сервера с паузы

        Map<String, List<String>> parameters = session.getParameters();

        String spid = null;
        if (parameters.containsKey("spid")) {
            spid = parameters.get("spid").get(0);
        }

        String infoBaseId = null;
        if (parameters.containsKey("infoBase")) {
            infoBaseId = parameters.get("infoBase").get(0);
        }

        String clusterId = null;
        if (parameters.containsKey("cluster")) {
            clusterId = parameters.get("cluster").get(0);
        }

        String serverId = null;
        if (parameters.containsKey("server")) {
            serverId = parameters.get("server").get(0);
        }

        String[] props = null;
        if (parameters.containsKey("props")) {
            String sprops = parameters.get("props").get(0);
            if (!sprops.isEmpty()) {
                props = sprops.split("[,;:-]");
            }
        }

        Configuration config = httpServer.getConfigManager().getConfig();
        ObjectMapper mapper = new ObjectMapper();

        // подключаемся api из библиотеки 1С к ras-серверу для получения данных из кластера 1С;
        // адрес и порт ras-сервера указаны в конфигурации центрального сервера, который мы уже получили
        //
        AgentAdminConnectorFactory rasAgentConnFactory =  new AgentAdminConnectorFactory();
        OneCRASAgent agent = new OneCRASAgent(rasAgentConnFactory);
        
        boolean serverFound = false;
        boolean clusterFound = false;
        boolean infoBaseFound = false;
        
        try {
            // перебираем все серверы, кластеры внутри серверов и информационные базы внутри кластеров;
            // ищем совпадения по идентификаторам, переданным в качестве параметров (если не передали,
            // то по всем из коллекции); для информационной базы получаем данные с помощью ras-агента
            //
            List<OneCServerInfo> serversInfo = new ArrayList<>();
            for (OneCServerConfig serverConfig : config.getOneCServers()) {
                if (serverConfig == null
                        || serverConfig.getClusters() == null
                        || serverId != null && !serverId.equals(serverConfig.getId())) {
                    continue;
                }
                serverFound = true;
                boolean connected = false;
                List<OneCClusterInfo> clustersInfo = new ArrayList<>();
                for (OneCClusterConfig clusterConfig : serverConfig.getClusters()) {
                    if (clusterId != null && !clusterId.equals(clusterConfig.getId())) {
                        continue;
                    }
                    clusterFound = true;
                    List<OneCInfoBaseInfo> infoBasesInfo = new ArrayList<>();
                    for (OneCInfoBaseConfig infoBaseConfig : clusterConfig.getInfoBases()) {
                        if (infoBaseId != null && !infoBaseId.equals(infoBaseConfig.getId())) {
                            continue;
                        }
                        infoBaseFound = true;

                        if (!connected) {
                            agent.connect(serverConfig.getRasAddress(), serverConfig.getRasPort(), 1000);
                            if (!agent.isConnected()) {
                                throw new IllegalStateException(String.format(
                                        "Не удалось установить соединение с RAS-сервером %s:%s",
                                        serverConfig.getRasAddress(),
                                        serverConfig.getRasPort()));
                            }
                            connected = true;
                        }

                        OneCInfoBaseInfo infoBaseInfo = new OneCInfoBaseInfo(infoBaseConfig);

                        OneCCluster cluster = agent.getCluster(clusterConfig.getName());
                        if (cluster == null) {
                            throw new IllegalStateException(String.format(
                                    "Не найден кластер %s сервера %s:%s",
                                    clusterConfig.getName(),
                                    serverConfig.getAddress(),
                                    serverConfig.getPort()));
                        }
                        cluster.setAdministrators(clusterConfig.getAdministrators());
                        try {
                            // обязательная аутентификация кластера (если не указан администратор кластера,
                            // то всё равно надо аутентифицироваться с пустым логином/паролем)
                            cluster.authenticate();

                            // получение основных данных - информация по каждой сессии текущего кластера
                            OneCInfoBase infoBase = cluster.getInfoBase(infoBaseConfig.getName());
                            if (infoBase == null) {
                                throw new IllegalStateException(String.format(
                                        "Не найдена информационная база %s в кластере %s сервера %s:%s",
                                        infoBaseConfig.getName(),
                                        cluster.getName(),
                                        serverConfig.getAddress(),
                                        serverConfig.getPort()));
                            }
                            List<ISessionInfo> sessions = cluster.getInfoBaseSessions(infoBase);

                            List<Map<String, Object>> fsessions = new ArrayList<>();
                            if (spid != null) {
                                // возвращаем данные только сеанса с указанным соединением с БД
                                for (ISessionInfo sessionInfo : sessions) {
                                    if (spid.equals(sessionInfo.getDbProcInfo())) {
                                        fsessions.add(toMap(sessionInfo, props, mapper));
                                    } 
                                }
                            }
                            else {
                                // возвращаем данные всех сеансов текущей базы данных
                                for (ISessionInfo sessionInfo : sessions) {
                                    fsessions.add(toMap(sessionInfo, props, mapper));
                                }
                            }

                            infoBaseInfo.setSessions(fsessions);
                        }
                        catch (Exception ex) {
                            // не останавливаемся по ошибке на одной базе - можно получить данные других
                            infoBaseInfo.setException(ex);
                        }

                        List<Map<String, Object>> sessions = infoBaseInfo.getSessions();
                        if (sessions != null && !sessions.isEmpty() || infoBaseInfo.getException() != null) {
                            infoBasesInfo.add(infoBaseInfo);
                        }
                    }

                    if (!infoBasesInfo.isEmpty()) {
                        OneCClusterInfo clusterInfo = new OneCClusterInfo(clusterConfig);
                        clusterInfo.setInfoBases(infoBasesInfo);
                        clustersInfo.add(clusterInfo);
                    }
                }

                if (!clustersInfo.isEmpty()) {
                    OneCServerInfo serverInfo = new OneCServerInfo(serverConfig);
                    serverInfo.setClusters(clustersInfo);
                    serversInfo.add(serverInfo);
                }

                if (agent.isConnected()) {
                    agent.disconnect();
                }
            }

            if (!serverFound && serverId != null) {
                throw new IllegalStateException(String.format(
                        "Не найден сервер %s",
                        serverId));
            }
            if (!clusterFound && clusterId != null) {
                throw new IllegalStateException(String.format(
                        "Не найден кластер %s",
                        clusterId));
            }
            if (!infoBaseFound && infoBaseId != null) {
                throw new IllegalStateException(String.format(
                        "Не найдена информационная база %s",
                        infoBaseId));
            }

            String json = mapper.writeValueAsString(serversInfo);
        //  String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(serversInfo);
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    NanoHTTPD.MIME_PLAINTEXT,
                    json);
        }
        catch (Exception e) {
            try {
                if (agent.isConnected()) {
                    agent.disconnect();
                }
            }
            catch (Exception ex) {
            }
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.INTERNAL_ERROR,
                    NanoHTTPD.MIME_PLAINTEXT,
                    e.getMessage());
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
