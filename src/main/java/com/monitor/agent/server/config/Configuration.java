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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.monitor.enterprise.OneCCredentials;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(Include.NON_NULL)
public class Configuration {

    private List<FilesConfig> files = new ArrayList<>();
    @JsonProperty("1c servers")
    private List<OneCServerConfig> oneCServers = new ArrayList<>();
    @JsonProperty(required = false)
    private String token = null;
    
    public static final String encodeString(String string) {
        if (string.isEmpty()) {
            return string;
        }
        Double rnd = Math.floor(Math.random() * 255);
        String temp = string.substring(1) + string.substring(0, 1) + new String(new byte[] { rnd.byteValue() });
        temp = new String(Base64.getEncoder().encode(temp.getBytes()));
        while (temp.endsWith("=")) {
            temp = temp.substring(0, temp.length() - 1);
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < temp.length(); i++) {
            String sym = String.valueOf(temp.charAt(i));
            result.append(sym.toLowerCase().equals(sym) ? sym.toUpperCase() : sym.toLowerCase());
        }
        return result.toString();
    }

    public static final String decodeString(String string) {
        if (string.isEmpty()) {
            return string;
        }
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            String sym = String.valueOf(string.charAt(i));
            buff.append(sym.toLowerCase().equals(sym) ? sym.toUpperCase() : sym.toLowerCase());
        }
        String temp = new String(Base64.getDecoder().decode(buff.toString().getBytes()));
        return temp.substring(temp.length() - 2, temp.length() - 1) + temp.substring(0, temp.length() - 2);
    }

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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
    
    public void encodePasswords() {
        for (OneCServerConfig server : oneCServers) {
            for (OneCCredentials credentials : server.getAdministrators()) {
                String password = credentials.getPassword();
                credentials.setPassword(encodeString(password));
            }
            for (OneCClusterConfig cluster : server.getClusters()) {
                for (OneCCredentials credentials : cluster.getAdministrators()) {
                    String password = credentials.getPassword();
                    credentials.setPassword(encodeString(password));
                }
            }
        }
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
