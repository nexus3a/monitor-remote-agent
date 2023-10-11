package com.monitor.agent.server.config;

/*
 * Copyright 2015 Didier Fetter
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
 * Changes by Aleksei Andreev:
 * - removed "network"
 * - added "oneCServers"
 *
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

public class Configuration {

    private List<FilesConfig> files;
    @JsonProperty("1c servers")
    private List<OneCServerConfig> oneCServers;

    public List<FilesConfig> getFiles() {
        return files;
    }

    public void setFiles(List<FilesConfig> files) {
        this.files = files;
    }

    public List<OneCServerConfig> getOneCServers() {
        return oneCServers;
    }

    public void setOneCServers(List<OneCServerConfig> oneCServers) {
        this.oneCServers = oneCServers;
    }
    
    public OneCInfoBaseConfig getInfoBaseById(String id) {
        for (OneCServerConfig server : oneCServers) {
            for (OneCClusterConfig cluster : server.getClusters()) {
                for (OneCInfoBaseConfig infoBase : cluster.getInfoBases()) {
                    if (id.equals(infoBase.getId())) {
                        return infoBase;
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).
            //  append("ras agents", rasAgents).
                append("1c servers", oneCServers).
                append("files", files).
                toString();
    }

}
