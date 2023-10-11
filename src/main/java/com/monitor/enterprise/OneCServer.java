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

import com._1c.v8.ibis.admin.IAgentAdminConnection;
import java.util.Set;
import java.util.UUID;

public class OneCServer {
    
    private final UUID uuid;
    private final String name;
    private int port;
    private boolean central;
    private final OneCCluster cluster;
    private Set<OneCCredentials> administartors; // администраторы центрального сервера (если central == true)

    private final IAgentAdminConnection connection;

    /* package */ OneCServer(UUID serverId, String name, OneCCluster cluster) {
        this.uuid = serverId;
        this.name = name;
        this.cluster = cluster;
        this.connection = cluster.getRASAgent().getConnection();
    }

    public String getName() {
        return name;
    }
    
    /**
     * Sets set of administartor credentials for this *central* working server
     * @param administartors set of cluster administartor credentials
     */
    public void setAdministartors(Set<OneCCredentials> administartors) {
        this.administartors = administartors;
    }
    
    /**
     * Performs ras agent authentication
     * @param userName cluster administrator name
     * @param password cluster administrator password
     */
    public void authenticate(String userName, String password) {
        assertConnectionIsNotNull();
        try {
            connection.authenticateAgent(userName, password);
            administartors.add(new OneCCredentials(userName, password));
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
        for (OneCCredentials credentials : administartors) {
            try {
                connection.authenticateAgent(credentials.getLogin(), credentials.getPassword());
                lastEx = null;
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
     * Checks connection for not null value; if it is null, rises exception
     */
    private void assertConnectionIsNotNull() {
        if (connection == null) {
            throw new IllegalStateException("The connection is not established.");
        }
    }

}
