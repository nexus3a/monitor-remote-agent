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

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.util.Map;

public class RootHandler extends DefaultResponder {

    @Override
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource, 
            Map<String, String> urlParams, 
            NanoHTTPD.IHTTPSession session) {
        
        super.get(uriResource, urlParams, session);

        try {
        
            if (!checkToken(uriResource)) {
                return badTokenResponse();
            }

            String message = 
                    "monitor remote agent usage:\n"
                    + "(get)/ping : return \"OK\"\n"
                    + "(get)/version : return agent's version number\n"
                    + "(get)/config : show current server configuration\n"
                    + "(post)/config : write server configuration\n"
                    + "(get)/accessibility : test catalogs for read/write capabiblities\n"
                    + "(get)/watchmap : show current watched log files\n"
                    + "(get)/logrecords?[section=<section>]&[max=<max-num-records>]&[filter=<json-filter>]&[ack]&[global-lock=<true|false>]&[max-token-length=<max-symbols>]&[exclude-data=<field1,field2,...,fieldN>]&[draft=<true|false>]&[delay=<N-ms>] : return log records\n"
                    + "(get)/ack : signal to confirm access last read log records\n"
                    + "(get)/sessionsinfo : show cluster's sessions info\n"
                    + "(get)/tjlogconfig : show tech-journal config content\n"
                    + "(post)/tjlogconfig : write tech-journal config content\n"
                    + "(get)/execquery?query=<query-text>&connection=<jdbc-cnn-string>&user=<db-user-name>&pass=<db-user-password> : return query resultset\n"
                    + "(get)/osprocinfo : return 1c os processes ports and pids\n"
                    + "(get)/dumpsinfo : return dump files list\n"
                    + "(get)/srvinfo?[server=<server-name>]&[catalog=<srvinfo-catalog>] : return server info\n"
                    + "(get)/settoken?newtoken=<access-token> : setup access token\n"
                    ;

            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    NanoHTTPD.MIME_PLAINTEXT,
                    message);
            
        }
        catch (Exception ex) {
            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    ex.getLocalizedMessage());
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
