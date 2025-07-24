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
import com.monitor.agent.server.config.*;
import com.monitor.enterprise.*;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OneCSessionsInfoHandler extends DefaultResponder {
    
    private static final Logger logger = LoggerFactory.getLogger(OneCSessionsInfoHandler.class);
    private static final String SECTION_LOCKED_MESSAGE = "SECTION_LOCKED";

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
    
    private void tryConnectAgent(OneCRASAgent agent, String address, int port) throws IllegalStateException {
        agent.connect(address, port, 1000);
        if (!agent.isConnected()) {
            throw new IllegalStateException(String.format(
                    "Не удалось установить соединение с RAS-сервером %s:%s", address, port));
        }
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
            RequestParameters parameters = getParameters();

            String spid = (String) parameters.get("spid", null);
            String infoBaseIds = (String) parameters.get("infoBase", null); // можно через запятую
            String clusterIds = (String) parameters.get("cluster", null);   // можно через запятую
            String serverId = (String) parameters.get("server", null);
            String sprops = (String) parameters.get("props", null);

            String[] props = null;
            if (sprops != null && !sprops.isEmpty()) {
                props = sprops.split("[,;:-]");
            }
            
            Lock lock = httpServer.getSessionInfoLock();
            if (lock.tryLock()) {
                
                try {
                    // перебираем все серверы, кластеры внутри серверов и информационные базы внутри кластеров;
                    // ищем совпадения по идентификаторам, переданным в качестве параметров (если не передали,
                    // то по всем из коллекции); для информационной базы получаем данные с помощью ras-агента
                    //
                    List<OneCServerInfo> serversInfo = new ArrayList<>();
                    for (OneCServerConfig serverConfig : config.getOneCServers()) {
                        if (serverConfig == null
                                || serverId != null && !serverId.equals(serverConfig.getId())
                                || serverConfig.getClusters() == null
                                || serverConfig.getRasAddress() == null
                                || serverConfig.getRasAddress().isEmpty()
                                || serverConfig.getRasPort() == 0) {
                            continue;
                        }
                        serverFound = true;

                        tryConnectAgent(agent, serverConfig.getRasAddress(), serverConfig.getRasPort());

                        List<OneCClusterInfo> clustersInfo = new ArrayList<>();

                        // создадим отдельную коллекцию настроек кластеров, содержащую только те кластера,
                        // которые нужно будет возвращать клиенту; в случае, когда не указывается отбор
                        // по кластеру, то мы должны вернуть информацию по всем кластерам, даже если они
                        // не укзанаы в настройках serverConfig
                        //
                        List<String> clusterNames;
                        if (clusterIds == null) {
                            clusterNames = agent.getClusters()
                                    .stream()
                                    .map(c -> c.getName())
                                    .collect(Collectors.toList());
                        }
                        else {
                            clusterNames = Arrays.asList(clusterIds.split(","))
                                    .stream()
                                    .map(c -> c.trim())
                                    .collect(Collectors.toList());
                        }

                        List<OneCClusterConfig> clustersConfigs = new ArrayList<>();
                        for (String clusterName : clusterNames) {
                            boolean configFound = false;
                            for (OneCClusterConfig clusterConfig : serverConfig.getClustersOrEmpty()) {
                                if (clusterName.equals(clusterConfig.getName())) {
                                    clustersConfigs.add(clusterConfig);
                                    configFound = true;
                                    break;
                                }
                            }
                            if (!configFound) {
                                clustersConfigs.add(new OneCClusterConfig(clusterName));
                            }
                        }

                        for (OneCClusterConfig clusterConfig : clustersConfigs) {
                            OneCCluster cluster = agent.getCluster(clusterConfig.getName());
                            if (cluster == null) {
                                throw new IllegalStateException(String.format(
                                        "Не найден кластер %s сервера %s:%s",
                                        clusterConfig.getName(),
                                        serverConfig.getAddress(),
                                        serverConfig.getPort()));
                            }
                            clusterFound = true;

                            // обязательная аутентификация кластера (если не указан администратор кластера,
                            // то всё равно надо аутентифицироваться с пустым логином/паролем)
                            //
                            cluster.setAdministrators(clusterConfig.getAdminVariants());
                            cluster.authenticate();

                            List<OneCInfoBaseInfo> infoBasesInfo = new ArrayList<>();

                            // создадим отдельную коллекцию настроек инфобаз, содержащую только те инфобазы,
                            // которые нужно будет возвращать клиенту; в случае, когда не указывается отбор
                            // по инфобазе, мы должны вернуть информацию по всем инфобазам кластера, даже
                            // если они не укзанаы в настройках clusterConfig
                            //
                            List<OneCInfoBaseConfig> infoBasesConfigs;
                            if (infoBaseIds == null) {
                                infoBasesConfigs = cluster.getInfoBases()
                                        .stream()
                                        .map(b -> new OneCInfoBaseConfig(b.getName()))
                                        .collect(Collectors.toList());
                            }
                            else {
                                infoBasesConfigs = new ArrayList<>();
                                for (String infoBaseId : infoBaseIds.split(",")) {
                                    String infoBaseIdTrim = infoBaseId.trim();
                                    boolean baseFound = false; // != infoBaseFound!!!
                                    for (OneCInfoBaseConfig infoBaseConfig : clusterConfig.getInfoBasesOrEmpty()) {
                                        if (infoBaseIdTrim.equals(infoBaseConfig.getId())) {
                                            infoBasesConfigs.add(infoBaseConfig);
                                            baseFound = true;
                                            break;
                                        }
                                    }
                                    if (!baseFound) {
                                        infoBasesConfigs.add(new OneCInfoBaseConfig(infoBaseIdTrim));
                                    }
                                }
                            }

                            for (OneCInfoBaseConfig infoBaseConfig : infoBasesConfigs) {
                                OneCInfoBaseInfo infoBaseInfo = new OneCInfoBaseInfo(infoBaseConfig);

                                try {
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
                                    infoBaseFound = true;

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
                    if (!clusterFound && clusterIds != null) {
                        throw new IllegalStateException(String.format(
                                "Не найден кластер %s",
                                clusterIds));
                    }
                    if (!infoBaseFound && infoBaseIds != null) {
                        throw new IllegalStateException(String.format(
                                "Не найдена информационная база %s",
                                infoBaseIds));
                    }

                    String json = mapper.writeValueAsString(serversInfo);
                    return NanoHTTPD.newFixedLengthResponse(
                            NanoHTTPD.Response.Status.OK,
                            "application/json",
                            json);
                
                }
                finally {
                    lock.unlock();
                }
            }
            else {
                // не удалось заблокировать секцию для получения данных -
                // выведем в поток служебную запись
                //
                return NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.TOO_MANY_REQUESTS,
                        NanoHTTPD.MIME_PLAINTEXT,
                        SECTION_LOCKED_MESSAGE);
            }
        }
        catch (Exception e) {
            logger.error("Exception while OneCSessionsInfo handling", e);
            try {
                if (agent.isConnected()) {
                    agent.disconnect();
                }
            }
            catch (Exception ex) {
                logger.error("Exception while disconnectiong RAS Agent", ex);
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
