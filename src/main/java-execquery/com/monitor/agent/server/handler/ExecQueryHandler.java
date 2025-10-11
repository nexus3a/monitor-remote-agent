package com.monitor.agent.server.handler;

/*
 * Copyright 2023 Aleksei Andreev
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.agent.server.Server;
import com.monitor.agent.server.piped.ParserPipedOutputStream;
import com.monitor.agent.server.piped.ParserPipedStream;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import org.postgresql.util.PGobject;

public class ExecQueryHandler extends DefaultResponder {

    private static final int QUERY_EXEC_TIMEOUT = 10 * 60 * 1000; // 10 минут
    private static boolean MSSQL_AUTH_LIB_LOADED = false;

    private static boolean extractFromJar(String src, String dest) {
        File destFile = new File(dest);
        if (destFile.exists() && destFile.length() > 0) {
            return true;
        }
        try (
                JarFile jarFile = new JarFile("monitor-remote-agent.jar");
                InputStream in = jarFile.getInputStream(jarFile.getEntry(src));
                OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
            return true;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void appendToLibraryPath(String dir) {

        String path = System.getProperty("java.library.path");
        String lpath = path.toLowerCase();
        String ldir = dir.toLowerCase();
        if (lpath.equals(ldir) 
                || lpath.startsWith(ldir + ";") 
                || lpath.endsWith(";" + ldir) 
                || lpath.contains(";" + ldir + ";")) {
            return;
        }
        path = dir + ";" + path;
        System.setProperty("java.library.path", path);

        // https://stackoverflow.com/questions/23189776/load-native-library-from-class-path
        //
        try {
            final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }
    
    private static synchronized boolean loadMssqlAuthLibrary() {
        if (MSSQL_AUTH_LIB_LOADED) {
            return true;
        }
        boolean result = false;
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            final String JDBCA = 
                    System.getProperty("os.arch").toLowerCase().contains("64")
                    ? "mssql-jdbc_auth-12.4.1.x64"
                    : "mssql-jdbc_auth-12.4.1.x86";
            if (extractFromJar("mssql-jdbc-auth-lib/" + JDBCA + ".dll", "./" + JDBCA + ".dll")) {
                try { appendToLibraryPath("."); } catch (Exception | Error ex) {}
                try { System.loadLibrary(JDBCA); result = true; } catch (Exception | Error ex) {}
            }
        }
        MSSQL_AUTH_LIB_LOADED = result;
        return result;
    }
    
    @Override
    @SuppressWarnings({"Convert2Lambda", "UseSpecificCatch"})
    public NanoHTTPD.Response get(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {

        super.get(uriResource, urlParams, session);

        Server server = uriResource.initParameter(Server.class);
        server.waitForUnpause(); // ожидания снятия сервера с паузы

        ParserPipedStream pipe;

        try {

            if (!checkToken(uriResource)) {
                return badTokenResponse();
            }

            pipe = new ParserPipedStream(QUERY_EXEC_TIMEOUT);;

            (new Thread() {
                @Override
                public void run() {

                    ObjectMapper mapper = new ObjectMapper();

                    ParserPipedOutputStream output = pipe.getOutput();

                    ResultSet resultSet = null;
                    Connection conn = null;
                    Statement stmt = null;
                    Exception fatal = null;

                    try {
                        RequestParameters parameters = getParameters();

                        // получаем текст запроса
                        //
                        String queryString = (String) parameters.get("query", "select 1 where 1 = 0");

                        // получаем строку подключения к базе данных
                        // пример: jdbc:mysql://localhost:3306/myDb?user=user1&password=pass
                        // пример: jdbc:postgresql://localhost/myDb
                        // пример: jdbc:hsqldb:mem:myDb
                        // пример: jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;user=user1;password=pass;encrypt=false;
                        //
                        String connString = (String) parameters.get("connection", "");

                        // принудительная загрузка класса, по идее, в новых версиях драйверов не требуется,
                        // но для Java старых версий это не работает, поэтому принудительно загружаем класс
                        // с драйвером СУБД;
                        // для sqlserver дополнительно загружаем библиотеку для windows-авторизации
                        //
                        if (connString.contains("sqlserver")) {
                            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                            if (connString.toLowerCase().contains("integratedsecurity=true")) {
                                loadMssqlAuthLibrary();
                            }
                        }
                        else if (connString.contains("postgresql")) {
                            Class.forName("org.postgresql.Driver");
                        }

                        // получаем логин и пароль для подключения к базе данных
                        //
                        String user = (String) parameters.get("user", "");
                        String pass = (String) parameters.get("pass", "");

                        // выполняем запрос и отправляем результат клиенту;
                        //
                        conn = DriverManager.getConnection(connString, user, pass);
                        stmt = conn.createStatement();

                        // возвращаться будет массив массивов запесей: результат выполнения каждого запроса
                        // из пакета - это элемент внешнего массива ([результат_0, ..., результат_N]);
                        // каждый результат, в свою очередь, тоже массив объектов записей результата запроса
                        // ([{поле_0:значение_0, ...}, ..., {поле_M:значение_M, ...}]), или массив из одного 
                        // элемента, если это запрос на обновление данных или иной не-SELECT ([{affected:значение}])
                        
                        boolean isResultSet = stmt.execute(queryString);

                        output.write("[\n".getBytes(StandardCharsets.UTF_8));

                        boolean isFirstQuery = true;
                        while (true) {
                            if (isResultSet) {
                                if (!isFirstQuery) {
                                    output.write(",\n".getBytes(StandardCharsets.UTF_8));
                                }
                                resultSet = stmt.getResultSet();

                                ResultSetMetaData rsmd = resultSet.getMetaData();
                                int columnsCount = rsmd.getColumnCount();
                                String[] columnNames = new String[columnsCount];
                                for (int i = 1; i <= columnsCount; i++) {
                                    try {
                                        String columnName = rsmd.getColumnName(i);
                                        columnNames[i - 1] = (columnName.trim().length() == 0 ? "columnn_" + i : columnName);
                                    }
                                    catch (SQLException e) {
                                        columnNames[i - 1] = "unknown_" + i;
                                    }
                                }

                                output.write("[\n".getBytes(StandardCharsets.UTF_8));

                                boolean isFirstRecord = true;
                                while (resultSet.next()) {
                                    HashMap<String, Object> record = new HashMap<>();
                                    for (int i = 0; i < columnsCount; i++) {
                                        Object recValue = resultSet.getObject(i + 1);
                                        if (recValue instanceof PGobject) {
                                            record.put(columnNames[i], ((PGobject) recValue).getValue());
                                        }
                                        else {
                                            record.put(columnNames[i], recValue);
                                        }
                                    }
                                    if (isFirstRecord) {
                                        isFirstRecord = false;
                                    }
                                    else {
                                        output.write(",\n".getBytes(StandardCharsets.UTF_8));
                                    }
                                    output.write(mapper.writeValueAsBytes(record));
                                }

                                output.write("\n]".getBytes(StandardCharsets.UTF_8));

                                resultSet.close();
                            }
                            else {
                                long updCount = stmt.getUpdateCount();
                                if (updCount == -1) {
                                    break;
                                }
                                if (!isFirstQuery) {
                                    output.write(",\n".getBytes(StandardCharsets.UTF_8));
                                }
                                output.write(("[{\"affected\":" + updCount + "}]").getBytes(StandardCharsets.UTF_8));
                            }
                            isFirstQuery = false;
                            isResultSet = stmt.getMoreResults();
                        }

                        output.write("\n]".getBytes(StandardCharsets.UTF_8));
                    }
                    catch (Exception ex) {
                        fatal = ex;
                    }

                    try { if (resultSet != null) resultSet.close(); } catch (SQLException e) {}
                    try { if (stmt != null)      stmt.close(); }      catch (SQLException e) {}
                    try { if (conn != null)      conn.close(); }      catch (SQLException e) {}

                    if (fatal != null) {
                        System.err.print(new Date());
                        fatal.printStackTrace();
                        try { output.write(fatal.getMessage().getBytes(StandardCharsets.UTF_8)); } catch (Exception e) {}
                    }

                    try {
                        pipe.close(); // закрывается при закрытии output, а output закрывает chunkedResponse
                    }
                    catch (IOException ex) {
                    }
                    System.gc();
                }
            }).start();

        }
        catch (Exception ex) {

            return NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    ex.getMessage());
        }

        Response response = NanoHTTPD.newChunkedResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                pipe.getInput());
        response.setGzipEncoding(true);
        return response;
    }

    @Override
    public NanoHTTPD.Response post(
            RouterNanoHTTPD.UriResource uriResource,
            Map<String, String> urlParams,
            NanoHTTPD.IHTTPSession session) {
        return get(uriResource, urlParams, session);
    }

}
