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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.monitor.agent.server.config.OneCServerConfig;
import java.util.List;

public class OneCServerInfo {
    
    private String address;
    private int port;
    @JsonProperty("ras address")
    private String rasAddress;
    @JsonProperty("ras port")
    private int rasPort;
    private List<OneCClusterInfo> clusters;

    public OneCServerInfo() {
    }
    
    public OneCServerInfo(OneCServerConfig config) {
        address = config.getAddress();
        port = config.getPort();
        rasAddress = config.getRasAddress();
        rasPort = config.getRasPort();
    }    

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRasAddress() {
        return rasAddress;
    }

    public void setRasAddress(String rasAddress) {
        this.rasAddress = rasAddress;
    }

    public int getRasPort() {
        return rasPort;
    }

    public void setRasPort(int rasPort) {
        this.rasPort = rasPort;
    }

    public List<OneCClusterInfo> getClusters() {
        return clusters;
    }

    public void setClusters(List<OneCClusterInfo> clusters) {
        this.clusters = clusters;
    }
    
}
