package com.monitor.enterprise;

/*
 * Copyright 2021 Aleksei Andreev
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

import com._1c.v8.ibis.admin.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OneCCluster {
    
    private UUID uuid;
    private String name;
    private OneCRASAgent agent;
    
    private Set<OneCCredentials> administrators;
    
    private final IAgentAdminConnection connection;

    public OneCCluster() {
        this(null, null, null);
    }

    /* package */ OneCCluster(UUID clusterId, String name, OneCRASAgent agent) {
        this.uuid = clusterId;
        this.name = name;
        this.agent = agent;
        this.connection = agent.getConnection();
        this.administrators = new HashSet<>();
        this.administrators.add(new OneCCredentials("", ""));
    }

    /**
     * Returns name of this cluster
     * @return name of cluster
     */
    public String getName() {
        return name;
    }

    /**
     * Returns uuid of this cluster
     * @return uuid of cluster
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Returns RAS agent, used for getting cluster info
     * @return RAS agent, used for getting cluster info
     */
    public OneCRASAgent getRASAgent() {
        return agent;
    }

    /**
     * Sets set of administartor credentials for this cluster
     * @param administartors set of cluster administartor credentials
     */
    public void setAdministrators(Set<OneCCredentials> administartors) {
        this.administrators = administartors;
    }
    
    /**
     * Performs cluster authentication
     * @param userName cluster administrator name
     * @param password cluster administrator password
     * @throws java.lang.Exception
     */
    public void authenticate(String userName, String password) throws Exception {
        assertConnectionIsNotNull();
        try {
            connection.authenticate(uuid, userName, password);
            administrators.add(new OneCCredentials(userName, password));
        }
        catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * Performs cluster authentication using set of 1c credentials, set before
     * @throws java.lang.Exception
     */
    public void authenticate() throws Exception {
        assertConnectionIsNotNull();
        Exception lastEx = null;
        for (OneCCredentials credentials : administrators) {
            try {
                connection.authenticate(uuid, credentials.getLogin(), credentials.getPassword());
                lastEx = null;
                break;
            }
            catch (Exception ex) {
                lastEx = ex;
            }
        }
        if (lastEx != null) {
            throw lastEx;
        }
    }

    /**
     * Gets the list of cluster working servers
     * @return list of cluster working servers
     */
    public List<OneCServer> getServers() {
        assertConnectionIsNotNull();
        List<IWorkingServerInfo> workingServersInfos = connection.getWorkingServers(uuid);
        List<OneCServer> servers = new ArrayList<>(workingServersInfos.size());
        for (IWorkingServerInfo workingServersInfo : workingServersInfos) {
            OneCServer server = new OneCServer(workingServersInfo.getWorkingServerId(), 
                    workingServersInfo.getName(), this);
            servers.add(server);
        }
        return servers; 
    }

    /**
     * Gets the cluster working server with given name
     * @param name name of working server
     * @return working server with given name
     */
    public OneCServer getServer(String name) {
        assertConnectionIsNotNull();
        List<IWorkingServerInfo> workingServersInfos = connection.getWorkingServers(uuid);
        for (IWorkingServerInfo workingServersInfo : workingServersInfos) {
            if (workingServersInfo.getName().equals(name)) {
                return new OneCServer(workingServersInfo.getWorkingServerId(), workingServersInfo.getName(), this);
            }
        }
        return null;
    }

    /**
     * Gets the list of cluster infobases
     * @return list of cluster infobases
     */
    public List<OneCInfoBase> getInfoBases() {
        assertConnectionIsNotNull();
        List<IInfoBaseInfoShort> baseInfoShorts = connection.getInfoBasesShort(uuid);
        List<OneCInfoBase> infoBases = new ArrayList<>(baseInfoShorts.size());
        for (IInfoBaseInfoShort baseInfoShort : baseInfoShorts) {
            OneCInfoBase infoBase = new OneCInfoBase(baseInfoShort.getInfoBaseId(), baseInfoShort.getName(), this);
            infoBases.add(infoBase);
        }
        return infoBases; 
    }

    /**
     * Gets the infobase with given name
     * @param name name of infobase
     * @return infobase with given name
     */
    public OneCInfoBase getInfoBase(String name) {
        assertConnectionIsNotNull();
        List<IInfoBaseInfoShort> baseInfoShorts = connection.getInfoBasesShort(uuid);
        for (IInfoBaseInfoShort baseInfoShort : baseInfoShorts) {
            if (baseInfoShort.getName().equals(name)) {
                return new OneCInfoBase(baseInfoShort.getInfoBaseId(), baseInfoShort.getName(), this);
            }
        }
        return null; 
    }

    /**
     * Gets the list of cluster sessions
     * @return list of cluster sessions
     */
    public List<ISessionInfo> getSessions() {
        assertConnectionIsNotNull();
        List<ISessionInfo> sessions = connection.getSessions(uuid);
        return sessions; 
    }

    /**
     * Gets the list of cluster sessions
     * @param infoBase
     * @return list of cluster sessions
     */
    public List<ISessionInfo> getInfoBaseSessions(OneCInfoBase infoBase) {
        assertConnectionIsNotNull();
        List<ISessionInfo> sessions = connection.getInfoBaseSessions(uuid, infoBase.uuid);
        return sessions; 
    }

    /**
     * Gets the list of cluster connections
     * @return list of cluster connections
     */
    public List<IInfoBaseConnectionShort> getConnections() {
        assertConnectionIsNotNull();
        List<IInfoBaseConnectionShort> connectionShorts = connection.getConnectionsShort(uuid);
        return connectionShorts; 
    }

    /**
     * Gets the list of cluster working processes
     * @return list of cluster working processes
     */
    public List<IWorkingProcessInfo> getProcesses() {
        assertConnectionIsNotNull();
        List<IWorkingProcessInfo> processes = connection.getWorkingProcesses(uuid);
        return processes; 
    }

    /**
     * Checks connection for not null value; if it is null, rises exception
     */
    private void assertConnectionIsNotNull() {
        if (connection == null) {
            throw new IllegalStateException("The connection is not established.");
        }
    }

}
