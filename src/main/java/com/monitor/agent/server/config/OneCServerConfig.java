package com.monitor.agent.server.config;

/*
 * Copyright 2022 Aleksei Andreev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import com.fasterxml.jackson.annotation.*;
import com.monitor.enterprise.OneCCredentials;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({ "dumps path", "clutersOrEmpty" })
public class OneCServerConfig {
    
    private String address;
    @JsonProperty("port")
    private int port;
    @JsonProperty("ras port")
    private int rasPort;
    @JsonProperty("ras address")
    private String rasAddress;                    // адрес сервера ras, подключенного к этому центральному серверу 
    private boolean central;
    @JsonProperty("logcfg path")
    private String logCfgPath;                    // путь к файлу logcfg.xml
    private List<FilesConfig> files;              // набор каталогов с технологическим журналом, фильтры данных
    private List<OneCCredentials> administrators; // администраторы центрального сервера (если central == true)
    private List<OneCClusterConfig> clusters;     // кластеры, для которых данный сервер является центральным
    
    private String id;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        if (rasAddress == null) {
            rasAddress = address;
        }
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

    public boolean isCentral() {
        return central;
    }

    public void setCentral(boolean central) {
        this.central = central;
    }

    public List<OneCCredentials> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(List<OneCCredentials> administrators) {
        this.administrators = administrators;
    }

    public List<OneCClusterConfig> getClusters() {
        return clusters;
    }

    public List<OneCClusterConfig> getClustersOrEmpty() {
        if (clusters == null) {
            return new ArrayList<>();
        }
        else {
            return clusters;
        }
    }

    public void setClusters(List<OneCClusterConfig> clusters) {
        this.clusters = clusters;
        if (this.clusters != null) {
            for (OneCClusterConfig cluster : this.clusters) {
                cluster.setServer(this); // устанавливаем центральный сервер-владелец кластера
            }
        }
    }

    public String getLogCfgPath() {
        return logCfgPath;
    }

    public void setLogCfgPath(String logCfgPath) {
        this.logCfgPath = logCfgPath;
    }

    public List<FilesConfig> getFiles() {
        return files;
    }

    public void setFiles(List<FilesConfig> files) {
        this.files = files;
    }
    
    public OneCClusterConfig getCluster(String clusterName) {
        if (clusters == null) {
            return null;
        }
        for (OneCClusterConfig cluster : clusters) {
            if (cluster.getName().equalsIgnoreCase(clusterName)) {
                return cluster;
            }
        }
        return null;
    }

    public String getId() {
        return (id == null) ? (address + ":" + port) : id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
}
