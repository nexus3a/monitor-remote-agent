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
import com._1c.v8.ibis.admin.IInfoBaseInfo;
import java.util.UUID;

public class OneCInfoBase {
    
    UUID uuid;
    private String name;
    private OneCCluster cluster;

    private IAgentAdminConnection connection;
    
    public OneCInfoBase() {
        this(null, null, null);
    }

    /* package */ OneCInfoBase(UUID infoBaseId, String name, OneCCluster cluster) {
        this.uuid = infoBaseId;
        this.name = name;
        this.cluster = cluster;
        this.connection = cluster.getRASAgent().getConnection();
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the full infobase description
     *
     * @return infobase full infobase description
     */
    public IInfoBaseInfo getInfo() {
        assertConnectionIsNotNull();
        return connection.getInfoBaseInfo(cluster.getUUID(), uuid);
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
