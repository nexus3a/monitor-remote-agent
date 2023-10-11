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

import com._1c.v8.ibis.admin.AgentAdminException;
import com._1c.v8.ibis.admin.IAgentAdminConnection;
import com._1c.v8.ibis.admin.IClusterInfo;
import com._1c.v8.ibis.admin.client.AgentAdminConnectorFactory;
import com._1c.v8.ibis.admin.client.IAgentAdminConnector;
import com._1c.v8.ibis.admin.client.IAgentAdminConnectorFactory;
import java.util.ArrayList;
import java.util.List;

public class OneCRASAgent {
    
    private final IAgentAdminConnectorFactory factory;
    private IAgentAdminConnector connector;
    private IAgentAdminConnection connection;
    
    private String address;
    private int port;
    
    public OneCRASAgent(IAgentAdminConnectorFactory factory) {
        this.factory = factory;
    }
    
    public OneCRASAgent() {
        this(new AgentAdminConnectorFactory());
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    /**
     * Establishes connection with the administration server of 1C:Enterprise server cluster
     * @param address server address
     * @param port IP port
     * @param timeout connection timeout (in milliseconds)
     * @throws AgentAdminException in the case of errors.
     */
    public void connect(String address, int port, long timeout) throws AgentAdminException {
        this.address = address;
        this.port = port;

        if (connection != null) {
            throw new IllegalStateException("The connection is already established.");
        }

        connector = factory.createConnector(timeout);
        connection = connector.connect(address, port);
    }
    
    /**
     * Returns connection, associated with this RAS agent
     * @return connection, associated with RAS agent
     */
    IAgentAdminConnection getConnection() {
        return connection;
    }

    /**
     * Checks whether connection to the administration server is established
     * @return {@code true} if connected, {@code false} otherwise
     */
    public boolean isConnected() {
        return connection != null;
    }

    /**
     * Terminates connection to the administration server
     * @throws AgentAdminException in the case of errors.
     */
    public void disconnect() {
        assertConnectionIsNotNull();
        try {
            connector.shutdown();
        }
        finally {
            connection = null;
            connector = null;
        }
    }

    /**
     * Gets the list of clusters
     * @return list of clusters
     */
    public List<OneCCluster> getClusters() {
        assertConnectionIsNotNull();
        List<IClusterInfo> clusterInfos = connection.getClusters();
        List<OneCCluster> clusters = new ArrayList<>(clusterInfos.size());
        for (IClusterInfo clusterInfo : clusterInfos) {
            OneCCluster cluster = new OneCCluster(clusterInfo.getClusterId(), clusterInfo.getName(), this);
            clusters.add(cluster);
        }
        return clusters;
    }

    /**
     * Gets cluster with given name
     * @param name name of cluster
     * @return cluster with given name
     */
    public OneCCluster getCluster(String name) {
        assertConnectionIsNotNull();
        List<IClusterInfo> clusterInfos = connection.getClusters();
        for (IClusterInfo clusterInfo : clusterInfos) {
            if (clusterInfo.getName().equals(name)) {
                return new OneCCluster(clusterInfo.getClusterId(), clusterInfo.getName(), this);
            }
        }
        return null;
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
