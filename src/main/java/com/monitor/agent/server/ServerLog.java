package com.monitor.agent.server;

import ch.qos.logback.classic.Level;
import static ch.qos.logback.classic.Level.TRACE;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import java.io.IOException;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import com.monitor.agent.server.handler.DefaultResponder;
import java.util.Iterator;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Алексей
 */
public class ServerLog {
    
    private Level level = Level.INFO;
    private boolean debugWatchers = false;
    private String logfileSize = "10MB";
    private int logfileNumber =  5;
    private String logfile = "logs/process/process.log";
    
    
    public ServerLog() {
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setDebugWatchers(boolean debugWatchers) {
        this.debugWatchers = debugWatchers;
    }

    public void setLogfileSize(String logfileSize) {
        this.logfileSize = logfileSize;
    }

    public void setLogfileNumber(int logfileNumber) {
        this.logfileNumber = logfileNumber;
    }

    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }

    private ConsoleAppender findConsoleAppender(Logger logger) {
        ConsoleAppender appender = null;
        Iterator<Appender<ILoggingEvent>> appenders = logger.iteratorForAppenders();
        while (appenders.hasNext()) {
            Object element = appenders.next();
            if (element instanceof ConsoleAppender) {
                appender = (ConsoleAppender) element;
                break;
            }
        }
        return appender;
    }

    private RollingFileAppender findRollingFileAppender(String logFileName, Logger logger) {
        RollingFileAppender appender = null;
        String normFileName = logFileName.replaceAll("\\\\", "/");
        Iterator<Appender<ILoggingEvent>> appenders = logger.iteratorForAppenders();
        while (appenders.hasNext()) {
            Object element = appenders.next();
            if (element instanceof RollingFileAppender 
                    && ((RollingFileAppender) element).getFile().replaceAll("\\\\", "/").equalsIgnoreCase(normFileName)) {
                appender = (RollingFileAppender) element;
                break;
            }
        }
        return appender;
    }

    private ConsoleAppender createConsoleAppender(Encoder encoder) 
            throws IOException {
        ConsoleAppender appender = new ConsoleAppender();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.setEncoder(encoder);
        appender.setName("console");
        appender.start();
        return appender;
    }

    private Appender createRollingFileAppender(String logFileName, Encoder encoder, String maxSize, int maxIndex) 
            throws IOException {
        RollingFileAppender appender = new RollingFileAppender();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.setFile(logFileName);
        appender.setName("rollingFile");
        appender.setAppend(true);
        appender.setEncoder(encoder);
        
        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        rollingPolicy.setParent(appender);
        rollingPolicy.setFileNamePattern(logFileName + ".%i");
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(maxIndex);
        rollingPolicy.start();
        appender.setRollingPolicy(rollingPolicy);
        
        SizeBasedTriggeringPolicy triggeringPolicy = new SizeBasedTriggeringPolicy();
        triggeringPolicy.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        triggeringPolicy.setMaxFileSize(FileSize.valueOf(maxSize));
        triggeringPolicy.start();
        appender.setTriggeringPolicy(triggeringPolicy);
        
        appender.start();

        return appender;
    }

    public void setup() throws IOException {
        Appender appender;
        
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        encoder.setPattern("%d %p %c{1} - %m%n");
        encoder.start();
        
        Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(level);

        // логирование по умолчанию
        //
        if (logfile == null) {
            appender = findConsoleAppender(rootLogger);
            if (appender == null) {
                appender = createConsoleAppender(encoder);
                rootLogger.detachAndStopAllAppenders();
                rootLogger.addAppender(appender);
            }
        }
        else {
            appender = findRollingFileAppender(logfile, rootLogger);
            if (appender == null) {
                appender = createRollingFileAppender(logfile, encoder, logfileSize, logfileNumber);
                rootLogger.detachAndStopAllAppenders();
                rootLogger.addAppender(appender);
            }
        }
        
        // логирование FileWatcher
        //
        Logger l = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(FileWatcher.class);
        l.setLevel(Level.OFF);
        if (debugWatchers) {
            l.setAdditive(false);
            l.setLevel(TRACE);
            l.addAppender(appender);
        }
        
        // логирование DirectoryWatcher (настройка точно такая же, как у FileWatcher выше)
        //
        l = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DirectoryWatcher.class);
        l.setLevel(Level.OFF);
        if (debugWatchers) {
            l.setAdditive(false);
            l.setLevel(TRACE);
            l.addAppender(appender);
        }
        
        // включение логирования запросов к Агенту
        //
        l = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DefaultResponder.class);
        String accessLogfile = "logs/access/access.log";
        appender = createRollingFileAppender(accessLogfile, encoder, logfileSize, logfileNumber);
        l.setAdditive(false);
        l.setLevel(TRACE);
        l.addAppender(appender);
    }

}
