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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties({ "infoBasesOrEmpty" })
public class OneCClusterConfig {
    
    private String name;
    private String address; // адрес главного менеджера кластера
    private int port; // порт главного менеджера кластера (1541)
    
    private String id;
    
    private List<OneCServerConfig> servers;
    private Set<OneCCredentials> administrators;
    @JsonProperty("bases")
    private List<OneCInfoBaseConfig> infoBases;
    
    @JsonIgnore
    private OneCServerConfig server; // центральный сервер кластера

    public OneCClusterConfig() {
        port = 1541;
        administrators = new HashSet<>(1);
        administrators.add(new OneCCredentials());
    }

    public OneCClusterConfig(String name) {
        this();
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<OneCServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<OneCServerConfig> servers) {
        this.servers = servers;
    }

    @JsonIgnore
    public Set<OneCCredentials> getAdminVariants() {
        // предполагаем, что пароли администраторов кластера могут хранится 
        // в зашифрованном виде (хотя это не обязательно) - в таком случае 
        // добавим ко множеству администраторов кластера новых администраторов,
        // у которых пароли будут декодированными версиями паролей, хранящихся
        // в настройке администраторов - один из паролей должен будет подойти
        // при аутентификации
        //
        Set<OneCCredentials> result = new HashSet<>();
        result.addAll(administrators);
        for (OneCCredentials admin : administrators) {
            OneCCredentials decodedAdmin = new OneCCredentials();
            decodedAdmin.setLogin(admin.getLogin());
            try {
                decodedAdmin.setPassword(Configuration.decodeString(admin.getPassword()));
                result.add(decodedAdmin);
            }
            catch (Exception e) {
                // пропускаем такой пароль - он точно не шифровался ранее
            }
        }
        return result;
    }

    public Set<OneCCredentials> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(Set<OneCCredentials> administrators) {
        this.administrators = administrators;
        // в множестве администраторов кластера всегда должна быть запись
        // с пустым логином/паролем для случая, когда пользователь не задал
        // явных администраторов кластера - в таком случае аутентификация
        // в кластере будет призводиться с пустым логином/паролем; вообще
        // без аутентификации обращаться к ресурсам кластера нельзя
        //
        if (this.administrators == null) {
            this.administrators = new HashSet<>(1);
        }
        this.administrators.add(new OneCCredentials());
    }
    
    public List<OneCInfoBaseConfig> getInfoBases() {
        return infoBases;
    }

    public List<OneCInfoBaseConfig> getInfoBasesOrEmpty() {
        if (infoBases == null) {
            return new ArrayList<>();
        }
        else {
            return infoBases;
        }
    }

    public void setInfoBases(List<OneCInfoBaseConfig> infoBases) {
        this.infoBases = infoBases;
        if (this.infoBases != null) {
            for (OneCInfoBaseConfig infoBase : this.infoBases) {
                infoBase.setCluster(this); // устанавливаем кластер-владельца базы
            }
        }
    }

    public OneCServerConfig getServer() {
        return server;
    }

    public void setServer(OneCServerConfig server) {
        this.server = server;
    }

    public String getId() {
        return (id == null) ? name : id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
