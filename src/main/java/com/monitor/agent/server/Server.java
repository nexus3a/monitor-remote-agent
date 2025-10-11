package com.monitor.agent.server;

/*
 * Copyright 2015 Didier Fetter
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
 * Changes by Aleksei Andreev:
 * - almost all code except option processing was rewriten
 *
*/

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.JsonMappingException;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

import com.monitor.agent.server.config.ConfigurationManager;
import com.monitor.agent.server.config.FilesConfig;
import com.monitor.agent.server.config.OneCServerConfig;
import com.monitor.agent.server.handler.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final String AGENT_VERSION = "2.9.3";
    public  static final String SERVER_JKS_NAME = "monitor-remote-agent.jks";
    public  static final String SERVER_JKS_PASSWORD = "monitor";

    private RouterNanoHTTPD httpd;
    private ThreadPoolExecutor executor;
    private int starterPort = 0;
    private int port = 8085;
    private int spoolSize = 1024;
    private String config;
    private ConfigurationManager configManager;
    private int signatureLength = 4096;
    private boolean tailSelected = false;
    private String sincedbFile = TextDB.SINCEDB_EXT;
    private String stopRoute = "/shutdown";
    private boolean stopServer = false;
    private boolean secure = false;

    private final ServerLog serverLog;
    
    private final Semaphore pauseLock = new Semaphore(1);
    private final HashMap<Section, FileWatcher> watchers = new HashMap<>();
    
    private final Lock logRecordsLock = new ReentrantLock();
    private final Lock sessionInfoLock = new ReentrantLock();
    
    public static boolean isCaseInsensitiveFileSystem() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }
    
    public static String version() {
        return AGENT_VERSION;
    }

    @SuppressWarnings({"UseSpecificCatch", "CallToPrintStackTrace"})
    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.setup(args);
            if (server.stopServer) {
                server.sendStop();
                return;
            }
            server.initializeFileWatchers();
            server.startServer();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }
    }
    
    public Server() {
        this.serverLog = new ServerLog();
    }
    
    private void startServer() throws IOException, Exception {
        new File(TextDB.SINCEDB_CAT).mkdir();
        
        httpd = new RouterNanoHTTPD(port);
        
        httpd.addRoute("/", RootHandler.class, this);
        httpd.addRoute("/ping", PingHandler.class);
        httpd.addRoute("/version", VersionHandler.class);
        httpd.addRoute("/pause", PauseServerHandler.class, this);
        httpd.addRoute("/continue", ContinueServerHandler.class, this);
        httpd.addRoute("/config", ConfigHandler.class, this);
        httpd.addRoute("/accessibility", AccessibilityHandler.class, this);
        httpd.addRoute("/watchmap", WatchMapHandler.class, this);
        httpd.addRoute("/logrecords", LogRecordsHandler.class, this);
        httpd.addRoute("/ack", AckHandler.class, this);
        httpd.addRoute("/sessionsinfo", OneCSessionsInfoHandler.class, this);
        httpd.addRoute("/tjlogconfig", TJLogConfigHandler.class, this);
        httpd.addRoute("/osprocinfo", OSProcessInfoHandler.class, this);
        httpd.addRoute("/dumpsinfo", DumpsInfoHandler.class, this);
        httpd.addRoute("/srvinfo", SrvInfoHandler.class, this);
        httpd.addRoute("/settoken", SetTokenHandler.class, this);
        httpd.addRoute(stopRoute, StopServerHandler.class, this);
        httpd.setNotFoundHandler(NotFoundHandler.class);
        
        Class execQueryClass = Class.forName("com.monitor.agent.server.handler.ExecQueryHandler");
        if (execQueryClass != null) {
            httpd.addRoute("/execquery", execQueryClass, this);
        }

        logger.info("server start");
        secure = Files.exists(Paths.get(SERVER_JKS_NAME));
        if (secure) {
            httpd.makeSecure(getSslServerSocketFactory("monitor-remote-agent.jks", "monitor"), null);
        }
        httpd.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        logger.info("server started");
    }
    
    public void stopServer() throws IOException {
        if (httpd == null || !httpd.isAlive()) {
            logger.info("server stopped");
            return;
        }
        // останавливаем приложение (System.exit) только если запустили сервер
        // автономно, не из стартера - иначе остановится и сам стартер;
        // для этого проверяем порт стартера
        //
        try {
            logger.info("server stop");
            executor.shutdownNow();
            httpd.stop();
            logger.info("server stopped");
            if (starterPort == 0) {
                logger.info("exit code == 0");
                System.exit(0);
            }
        }
        catch (Throwable ex) {
            if (starterPort == 0) {
                logger.info("stop server exception, exit code == 4");
                System.exit(4);
            }
        }
    }
    
    public void pauseServer() throws InterruptedException {
        pauseLock.acquire();
    }
    
    public void continueServer() {
        pauseLock.release();
    }
    
    public void waitForUnpause() {
        try {
            pauseServer();
        }
        catch (InterruptedException ex) {
        }
        finally {
            continueServer();
        }
    }
    
    public boolean isPaused() {
        return pauseLock.availablePermits() == 0;
    }
    
    public Lock getLogRecordsLock() {
        return logRecordsLock;
    }

    public Lock getSessionInfoLock() {
        return sessionInfoLock;
    }
    
    private void sendStop() throws MalformedURLException, ProtocolException, IOException {
        String url = String.format("http://localhost:%s%s", port, stopRoute);
        HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
        httpClient.setRequestMethod("GET");
        httpClient.setRequestProperty("User-Agent", "MonitorAgent");
        httpClient.getResponseCode();
        httpClient.disconnect();
    }
    
    private void setup(String[] args) {
        try {
            parseOptions(args);
            serverLog.setup();
        }
        catch (IOException e) {
            System.err.println("Log setup exception: " + e.getLocalizedMessage());
            System.exit(1);
        }
        catch (ParseException e) {
            System.exit(1);
        }
        catch (NumberFormatException e) {
            System.exit(2);
        }
    }
    
    @SuppressWarnings("static-access")
    private void parseOptions(String[] args) throws ParseException, NumberFormatException {
        System.out.println(Arrays.toString(args));
        Option helpOption = new Option("help", "print this message");
        Option quietOption = new Option("quiet", "operate in quiet mode - only emit errors to log");
        Option debugOption = new Option("debug", "operate in debug mode");
        Option traceOption = new Option("trace", "operate in trace mode");
        Option debugWatcherOption = new Option("debugwatcher", "operate watcher in debug mode");
    //  Option secureOption = new Option("secure", "operate over https protocol");
        Option tailOption = new Option("tail", "read new files from the end");
        Option stopServerOption = new Option("stop", "signal to stop server, see also -stoproute");

        Option portOption = Option.builder("port").desc("server port")
                .hasArg()
                .argName("port number")
                .build();
        Option spoolSizeOption = Option.builder("spoolsize").desc("event count spool threshold - forces network flush")
                .hasArg()
                .argName("number of events")
                .build();
        Option configOption = Option.builder("config").desc("path to log-shipper configuration file")
                .hasArg() // .required()
                .argName("config file")
                .build();
        Option signatureLengthOption = Option.builder("signaturelength").desc("maximum length of file signature")
                .hasArg()
                .argName("signature length")
                .build();
        Option logfileOption = Option.builder("logfile").desc("logfile name")
                .hasArg()
                .argName("logfile name")
                .build();
        Option logfileSizeOption = Option.builder("logfilesize").desc("logfile size (default 10M)")
                .hasArg()
                .argName("logfile size")
                .build();
        Option logfileNumberOption = Option.builder("logfilenumber").desc("number of logfiles (default 5)")
                .hasArg()
                .argName("number of logfiles")
                .build();
        Option sincedbOption = Option.builder("sincedb").desc("sincedb file name")
                .hasArg()
                .argName("sincedb file")
                .build();
        Option stopRouteOption = Option.builder("stoproute").desc("route for server stop (defailt /shutdown)")
                .hasArg()
                .argName("stop server route")
                .build();

        Options options = new Options();
        options.addOption(helpOption)
                .addOption(spoolSizeOption)
                .addOption(quietOption)
                .addOption(debugOption)
                .addOption(debugWatcherOption)
    //          .addOption(secureOption)
                .addOption(traceOption)
                .addOption(tailOption)
                .addOption(portOption)
                .addOption(signatureLengthOption)
                .addOption(configOption)
                .addOption(logfileOption)
                .addOption(logfileNumberOption)
                .addOption(logfileSizeOption)
                .addOption(sincedbOption)
                .addOption(stopServerOption)
                .addOption(stopRouteOption);

        // повторим создание коллекции опций, применимых только к серверу,
        // без лишних "внешних" опций, чтобы можно было вывести подсказку
        // только по необходимым опциям
        //
        Options helpOptions = new Options();
        helpOptions.addOption(helpOption)
                .addOption(spoolSizeOption)
                .addOption(quietOption)
                .addOption(debugOption)
                .addOption(debugWatcherOption)
    //          .addOption(secureOption)
                .addOption(traceOption)
                .addOption(tailOption)
                .addOption(portOption)
                .addOption(signatureLengthOption)
                .addOption(configOption)
                .addOption(logfileOption)
                .addOption(logfileNumberOption)
                .addOption(logfileSizeOption)
                .addOption(sincedbOption)
                .addOption(stopServerOption)
                .addOption(stopRouteOption);

        // дальше - "внешние" опции только для стартера агента, но они должны 
        // здесь распознаваться, чтобы не было ошибки парсинга командной строки
        //
        Option starterPortOption = Option.builder("starterport").desc("starter port")
                .hasArg()
                .argName("starter port number")
                .build();
        Option updateRouteOption = Option.builder("updateroute").desc("route for server update (defailt /update)")
                .hasArg()
                .argName("update server route")
                .build();
        options.addOption(starterPortOption);
        options.addOption(updateRouteOption);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmdLine = parser.parse(options, args, true);
            if (cmdLine.hasOption(stopServerOption)) {
                stopServer = true;
            }
            if (!stopServer) {
                if (!cmdLine.hasOption(configOption)) {
                    throw new ParseException("Missing required option: " + configOption.getOpt());
                }
                config = cmdLine.getOptionValue(configOption);
            }
            if (cmdLine.hasOption(spoolSizeOption)) {
                spoolSize = Integer.parseInt(cmdLine.getOptionValue(spoolSizeOption));
            }
            if (cmdLine.hasOption(portOption)) {
                port = Integer.parseInt(cmdLine.getOptionValue(portOption));
            }
            if (cmdLine.hasOption(starterPortOption)) {
                starterPort = Integer.parseInt(cmdLine.getOptionValue(starterPortOption));
            }
            if (cmdLine.hasOption(signatureLengthOption)) {
                signatureLength = Integer.parseInt(cmdLine.getOptionValue(signatureLengthOption));
            }
            if (cmdLine.hasOption(quietOption)) {
                serverLog.setLevel(Level.ERROR);
            }
            if (cmdLine.hasOption(debugOption)) {
                serverLog.setLevel(Level.DEBUG);
            }
            if (cmdLine.hasOption(traceOption)) {
                serverLog.setLevel(Level.TRACE);
            }
            if (cmdLine.hasOption(debugWatcherOption)) {
                serverLog.setDebugWatchers(true);
            }
            if (cmdLine.hasOption(tailOption)) {
                tailSelected = true;
            }
            if (cmdLine.hasOption(logfileOption)) {
                serverLog.setLogfile(cmdLine.getOptionValue(logfileOption));
            }
            if (cmdLine.hasOption(logfileSizeOption)) {
                serverLog.setLogfileSize(cmdLine.getOptionValue(logfileSizeOption));
            }
            if (cmdLine.hasOption(logfileNumberOption)) {
                serverLog.setLogfileNumber(Integer.parseInt(cmdLine.getOptionValue(logfileNumberOption)));
            }
            if (cmdLine.hasOption(sincedbOption)) {
                sincedbFile = cmdLine.getOptionValue(sincedbOption);
            }
            if (cmdLine.hasOption(stopRouteOption)) {
                stopRoute = cmdLine.getOptionValue(stopRouteOption);
            }
    //      if (cmdLine.hasOption(secureOption)) {
    //          secure = true;
    //      }
        }
        catch (ParseException e) {
            System.err.println("General options exception: " + e.getLocalizedMessage());
            printHelp(helpOptions);
            throw e;
        }
        catch (NumberFormatException e) {
            System.err.println("Value must be an integer");
            printHelp(helpOptions);
            throw e;
        }
    }

    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("monitor-agent", options);
    }
    
    private void createFileWatchers(List<FilesConfig> configs) 
            throws JsonMappingException, UnsupportedEncodingException, IOException {
        
        if (configs == null) {
            return;
        }
        for (FilesConfig files : configs) {
            
            Section section = Section.byName(files.getSection());
            
            FileWatcher watcher = watchers.get(section);
            if (watcher == null) {
                watcher = new FileWatcher();
                watcher.setMaxSignatureLength(signatureLength);
                watcher.setTail(tailSelected);
                watcher.setSincedb(TextDB.SINCEDB_CAT + "/" + (section.getName().isEmpty() ? "" : ".") 
                        + section.getName() + sincedbFile);
                watcher.setSection(section);
                watchers.put(section, watcher);
            }

            for (String path : files.getPaths()) {
                watcher.addFilesToWatch(
                        path, 
                        PredefinedFields.fromMap(files.getFields()), 
                        files.getDeadTimeInSeconds() * 1000, 
                        files.getFilter(),
                        files.getEncoding());
            }
        }
    }
    
    public synchronized void initializeFileWatchers() throws JsonMappingException, UnsupportedEncodingException, IOException {
        for (FileWatcher watcher : watchers.values()) {
            watcher.close();
        }
        watchers.clear();

        configManager = new ConfigurationManager(config);
        configManager.readConfiguration();

        // настройки логов из корня конфигурации
        createFileWatchers(configManager.getConfig().getFiles());
        
        for (OneCServerConfig oneCServer : configManager.getConfig().getOneCServers()) {
            // настройки логов из узлов настроек серверов 1С
            createFileWatchers(oneCServer.getFiles());
        }

        for (FileWatcher watcher : watchers.values()) {
            watcher.initialize();
        }
    }
    
    public synchronized Collection<FileWatcher> getWatchers() {
        // возвращаем копию watchers.values() из-за того, что есть вероятность
        // перезаписи конфигурации агента в процессе передачи данных посредством
        // watchers текущей конфигурации; после перезаписи конфигурации
        // коллекция watchers будет переинициализирована, но нельзя прерывать
        // отправку данных предыдущими watcher'ами
        //
        ArrayList<FileWatcher> result = new ArrayList<>();
        for (FileWatcher watcher : watchers.values()) {
            result.add(watcher);
        }
        return result;
    }

    public synchronized Collection<FileWatcher> getWatchers(Section section) {
        if (section == null) {
            return getWatchers();
        }
        // здесь так же, как и в getWatchers(), возвращается новая коллекция с
        // watcher'ами - потому как по-другому нельзя, ибо используется фильтр,
        // но ещё и в целях возможности записывать новую конфигурацию агента в
        // процессе передачи данных по старой конфигурации (см. комментарий к
        // getWatchers())
        //
        ArrayList<FileWatcher> result = new ArrayList<>();
        FileWatcher watcher = watchers.get(section);
        if (watcher != null) {
            result.add(watcher);
        }
        return result;
    }

    public int getSpoolSize() {
        return spoolSize;
    }

    public String getConfig() {
        return config;
    }
    
    public ConfigurationManager getConfigManager() {
        return configManager;
    }
    
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }
    
    private SSLServerSocketFactory getSslServerSocketFactory (String keystoreFileName, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(Files.newInputStream(Paths.get(keystoreFileName)), password.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());
        return NanoHTTPD.makeSSLSocketFactory(keyStore, kmf.getKeyManagers());
    }
    
}
