package com.monitor.agent.server;

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
 * - added "directory watcher"
 * - almost all the rest of code was rewriten
 *
*/

import com.monitor.agent.server.filter.Filter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.Logger;

public class FileWatcher {

    private static final Logger logger = Logger.getLogger(FileWatcher.class);
    public static final int ONE_DAY = 24 * 3600 * 1000;
    
    private final List<FileAlterationObserver> observerList = new ArrayList<>();
    private final Map<File, FileState> curWatchMap = new HashMap<>();
    private final Map<File, FileState> newWatchMap = new HashMap<>();
    private FileState[] savedStates;
    private int maxSignatureLength;
    private boolean tail = false;
    private String sincedbFile = null;
    private final DirectoryWatcher directoryWatcher;
    private Section section;

    public FileWatcher() {
        directoryWatcher = new DirectoryWatcher(this);
    }
    
    public void initialize() throws IOException {
        logger.debug("Initializing FileWatcher");
        if (savedStates != null) {
            // коллекция файлов, которые будут отслеживаться, полученная по конфигруационному файлу
            Collection<File> newWatched = new HashSet<>();
            for (FileState state : newWatchMap.values()) {
                newWatched.add(state.getFile());
            }
            // коллекция файлов, которая была загружена из sinceDB с состояниями отслеживаемых файлов
            for (FileState state : savedStates) {
                if (newWatched.contains(state.getFile())) { // оставляем только актуальные записи
                    logger.info("    Loading file state: " + state.getFile() + ": pointer = " + state.getPointer());
                    curWatchMap.put(state.getFile(), state); // обновляем состояние [указателей] в файлах
                }
                else {
                    logger.info("    Dropping file state: " + state.getFile() + " - not matches any wildcard");
                }
            }
        }
        processModifications();
        if (tail) {
            for (FileState state : curWatchMap.values()) {
                if (state.getPointer() == 0) {
                    state.setPointer(state.getSize());
                }
            }
        }
        printWatchMap();
    }
    
    public void checkFiles() throws Exception {
        logger.trace("Checking files");
        logger.trace("==============");
        directoryWatcher.checkAndNotify();
        for (FileAlterationObserver observer : observerList) {
            observer.checkAndNotify();
        }
        processModifications();
        printWatchMap();
    }
    
    private boolean sameState(FileState newState, FileState oldState) throws IOException {
        if (logger.isTraceEnabled() && !newState.getFile().equals(oldState.getFile())) {
            logger.trace("Compare to " + oldState.getFile());
        }
        if (oldState.getSize() > newState.getSize()) {
            logger.trace("File is shorter : file can't be the same");
            return false;
        }
        if (oldState.getSignatureLength() == newState.getSignatureLength()) {
            if (oldState.getSignature() == newState.getSignature()) {
                logger.trace("Checking result : same signature size and value - file is the same");
                return true;
            }
            logger.trace("Signature is different : file can't be the same");
            return false;
        }
        if (oldState.getSignatureLength() < newState.getSignatureLength()) {
            long signature = FileSigner.computeSignature(newState, oldState.getSignatureLength());
            if (signature == oldState.getSignature()) {
                logger.trace("Checking result : signature is larger, but file has same "
                        + "previous signature - file is the same");
                return true;
            }
            logger.trace("Signature is different : file can't be the same");
            return false;
        }
        // oldState.getSignatureLength() > newState.getSignatureLength()
        logger.trace("Signature is shorter : file can't be the same");
        return false;
    }

    @SuppressWarnings("UseSpecificCatch")
    private void processModifications() throws IOException {
        
        for (File file : newWatchMap.keySet()) {

            FileState state = newWatchMap.get(file);
            if (logger.isTraceEnabled()) {
                logger.trace("Checking file : " + file.getCanonicalPath());
                logger.trace("-- Last modified : " + state.getLastModified());
                logger.trace("-- Size : " + state.getSize());
                logger.trace("-- Directory : " + state.getDirectory());
                logger.trace("-- Filename : " + state.getFileName());
                logger.trace("-- Pointer : " + state.getPointer());
                logger.trace("-- New pointer : " + state.getNewPointer());
            }

            // A -> A(time, size+) +
            // A -> A(time, size-)        rename?
            // A -> A(time)        +
            // A -> A(size+)       +
            // A -> A(size-)              rename?
            // del A
            // new A
            // A -> B (== del A + new B)  rename!
            // A <-> B                    rename!
                
            logger.trace("Determine if file has just been written to");
            { // for hiding curState variable
                FileState curState = curWatchMap.get(file);
                if (curState != null && sameState(state, curState)) {
                    // A -> A(time?, size+?); file is the same - no renaming
                    state.setOldFileState(curState);
                    continue;
                }
            }

            logger.trace("Determine if file has been renamed and/or written to");
            for (FileState curState : curWatchMap.values()) {
                if (!curState.isDeleted() 
                        || curState.isMatchedToNewFile() 
                        || file.equals(curState.getFile())
                        || !state.getDirectory().equals(curState.getDirectory())) {
                    continue;
                }
                if (sameState(state, curState)) {
                    // A -> B (== del A + new B); renaming of files
                    state.setOldFileState(curState);
                    break;
                }
            }
            if (state.getOldFileState() != null) {
                continue;
            }
            
            FileState curState = curWatchMap.get(file);
            if (curState != null) {
                for (FileState newState : newWatchMap.values()) {
                    FileState curNewState = curWatchMap.get(newState.getFile());
                    if (curNewState == null 
                            || curNewState.isMatchedToNewFile() 
                            || file.equals(curNewState.getFile())
                            || !curState.getDirectory().equals(curNewState.getDirectory())) {
                        continue;
                    }
                    if (sameState(newState, curState) && sameState(state, curNewState)) {
                        // A <-> B; renaming of files
                        state.setOldFileState(curNewState);
                        newState.setOldFileState(curState);
                        break;
                    }
                }
            }
        }

        for (FileState state : newWatchMap.values()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Refreshing file state: " + state.getFile());
            }
            FileState oldState = state.getOldFileState();
            if (oldState == null) {
                oldState = curWatchMap.get(state.getFile());
                if (oldState == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("File " + state.getFile() + " has been created; not retrieving pointer");
                    }
                }
            //  else if (oldState.isMatchedToNewFile()) {
            //      if (logger.isDebugEnabled()) {
            //          logger.debug("File " + state.getFile() + " has been truncated or created; not retrieving pointer");
            //      }
            //  }
                else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("File " + state.getFile() + " has been replaced and not renamed; not retrieving pointer");
                    }
                    oldState.closeRandomAccessFile(); 
            //      curWatchMap.remove(state.getFile());
                }
            }
            else {
                if (state.getFileName().equals(oldState.getFileName())) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("New data was added into the file " + state.getFile() + "; keep pointer");
                    }
                }
                else if (logger.isInfoEnabled() && (state.getFile().length() > 3 || oldState.getFile().length() > 3)) { // 3 == BOM.length
                    logger.info("File rename was detected: " + oldState.getFile() + " -> " + state.getFile() + "; keep pointer");
                }
                state.setPointer(oldState.getPointer());
                state.setNewPointer(oldState.getNewPointer());
                state.setLogFormat(oldState.getLogFormat());
                state.deleteOldFileState();
            }
        }

        logger.trace("Replacing old state");
        for (File file : newWatchMap.keySet()) {
            FileState state = newWatchMap.get(file);
            curWatchMap.put(file, state);
        }
        
        // checking log format - set new one if it isn't determined before
        for (File file : curWatchMap.keySet()) {
            FileState state = curWatchMap.get(file);
            if (!state.isDeleted() && state.getLogFormat() == null) {
                state.setLogFormat(LogFormat.determine(file));
            }
        }

        // Truncating changedWatchMap
        newWatchMap.clear();

        removeMarkedFilesFromWatchMap();
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void removeMarkedFilesFromWatchMap() throws IOException {
        logger.trace("Removing deleted files from watchMap");
        List<FileState> markedList = null;
        for (FileState state : curWatchMap.values()) {
            if (state.isDeleted()) {
                if (markedList == null) {
                    markedList = new ArrayList<>();
                }
                markedList.add(state);
            }
        }
        if (markedList != null) {
            for (FileState state : markedList) {
                state.closeRandomAccessFile();
                curWatchMap.remove(state.getFile());
                if (logger.isDebugEnabled() && !state.isMatchedToNewFile()) {
                    logger.debug("File " + state.getFile() + " removed from watchMap");
                }
            }
        }
    }
    
    public void addFilesToWatch(String fileToWatch, PredefinedFields fields, long deadTime, Filter filter, String encoding) {
        try {
            if (fileToWatch.contains("*")) {
                addWildCardFiles(fileToWatch, fields, deadTime, filter, encoding);
            }
            else {
                addSingleFile(fileToWatch, fields, deadTime, filter, encoding);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void addSingleFile(String fileToWatch, PredefinedFields fields, long deadTime, Filter filter, String encoding) throws Exception {
        logger.info("Watching file : " + new File(fileToWatch).getCanonicalPath());
        String directory = FilenameUtils.getFullPath(fileToWatch);
        String fileName = FilenameUtils.getName(fileToWatch);
        IOFileFilter fileFilter = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                FileFilterUtils.nameFileFilter(fileName),
                new LastModifiedFileFilter(deadTime));
        initializeWatchMap(new File(directory), fileFilter, fields, filter, encoding);
    }
    
    private void addWildCardFiles(String filesToWatch, PredefinedFields fields, long deadTime, Filter filter, String encoding) throws Exception {
        logger.info("Watching wildcard files : " + filesToWatch);
        String pathWildcard = FilenameUtils.getFullPath(filesToWatch);
        String nameWildcard = FilenameUtils.getName(filesToWatch);
        IOFileFilter fileFilter = FileFilterUtils.and(
                FileFilterUtils.fileFileFilter(),
                new WildcardFileFilter(nameWildcard, IOCase.SYSTEM),
                new LastModifiedFileFilter(deadTime));
        
        Set<File> directories = directoryWatcher.addWildCardDirectories(pathWildcard, fileFilter, fields, filter, encoding);
        if (directories != null) {
            for (File directory : directories) {
                logger.trace("Directory : " + directory.getCanonicalPath() + ", wildcard : " + nameWildcard);
                initializeWatchMap(directory, fileFilter, fields, filter, encoding);
            }
        }
    }
    
    private void initializeWatchMap(File directory, IOFileFilter fileFilter, PredefinedFields fields, Filter filter, String encoding) throws Exception {
        if (!directory.isDirectory() && directory.exists()) {
            logger.warn("Directory " + directory + " does not exist");
            return;
        }
        FileAlterationObserver observer = new FileAlterationObserver(directory, fileFilter);
        FileModificationListener listener = new FileModificationListener(this, fields, filter, encoding);
        observer.addListener(listener);
        observerList.add(observer);
        observer.initialize();
        if (directory.exists()) {
            for (File file : FileUtils.listFiles(directory, fileFilter, null)) {
                addFileToNewWatchMap(file, fields, filter, encoding);
            }
        }
    }
    
    @SuppressWarnings("UseSpecificCatch")
    private void addFileToNewWatchMap(File file, PredefinedFields fields, Filter filter, String encoding) {
        try {
            FileState state = new FileState(file);
            state.setFields(fields == null ? null : fields.copy());
            state.setFilter(filter == null ? null : filter.copy());
            state.setLogFormat(LogFormat.determine(file));
            state.setEncoding(encoding);
            int signatureLength = (int) Math.min(state.getSize(), maxSignatureLength);
            state.setSignatureLength(signatureLength);
            long signature = FileSigner.computeSignature(state, signatureLength);
            state.setSignature(signature);
            logger.trace("Setting signature of size : " + signatureLength + " on file : " + file + " : " + signature);
            newWatchMap.put(file, state); // will use in processModifications()
        }
        catch (Exception e) {
            logger.error("Caught IOException in addFileToWatchMap : "
                    + e.getMessage());
        }
    }
    
    public void onFileChange(File file, PredefinedFields fields, Filter filter, String encoding) {
        try {
            logger.debug("Change detected on file : " + file.getCanonicalPath());
            addFileToNewWatchMap(file, fields, filter, encoding);
        }
        catch (IOException e) {
            logger.error("Caught IOException in onFileChange : "
                    + e.getMessage());
        }
    }
    
    public void onFileCreate(File file, PredefinedFields fields, Filter filter, String encoding) {
        try {
            logger.debug("Create detected on file : " + file.getCanonicalPath());
            addFileToNewWatchMap(file, fields, filter, encoding);
        }
        catch (IOException e) {
            logger.error("Caught IOException in onFileCreate : "
                    + e.getMessage());
        }
    }
    
    public void onFileDelete(File file) {
        try {
            logger.debug("Delete detected on file : " + file.getCanonicalPath());
            FileState state = curWatchMap.get(file);
            if (state != null) {
                state.setDeleted();
            }
        }
        catch (IOException e) {
            logger.error("Caught IOException in onFileDelete: "
                    + e.getMessage());
        }
    }
    
    void onDirectoryCreate(File directory, IOFileFilter fileFilter, PredefinedFields fields, Filter filter, String encoding) {
        try {
            logger.trace("Directory created : " + directory.getCanonicalPath() + "; files will be watched");
            initializeWatchMap(directory, fileFilter, fields, filter, encoding);
        }
        catch (Exception e) {
            logger.error("Caught Exception in onDirectoryCreate: "
                    + e.getMessage());
        }
    }
    
    void onDirectoryChange(File directory, IOFileFilter fileFilter, PredefinedFields fields, Filter filter, String encoding) {
        try {
            logger.trace("Directory changed : " + directory.getCanonicalPath() + "; files will be watched");
            initializeWatchMap(directory, fileFilter, fields, filter, encoding);
        }
        catch (Exception e) {
            logger.error("Caught Exception in onDirectoryChange: "
                    + e.getMessage());
        }
    }
    
    void onDirectoryDelete(File directory, IOFileFilter fileFilter, PredefinedFields fields, Filter filter) {
        try {
            logger.trace("Directory deleted : " + directory.getCanonicalPath() + "; can't do anything");
        }
        catch (IOException e) {
            logger.error("Caught IOException in onDirectoryDelete: "
                    + e.getMessage());
        }
    }

    private void printWatchMap() throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("WatchMap contents :");
            for (File file : curWatchMap.keySet()) {
                FileState state = curWatchMap.get(file);
                logger.trace("\tFile : " + file.getCanonicalPath() + "; marked for deletion : " + state.isDeleted());
            }
        }
    }
    
    public void close() throws IOException {
        logger.debug("Closing all files");
        for (File file : curWatchMap.keySet()) {
            FileState state = curWatchMap.get(file);
            state.closeRandomAccessFile();
        }
    }
    
    public int getMaxSignatureLength() {
        return maxSignatureLength;
    }

    public void setMaxSignatureLength(int maxSignatureLength) {
        this.maxSignatureLength = maxSignatureLength;
    }

    public void setTail(boolean tail) {
        this.tail = tail;
    }
    
    public int readFiles(ParserFileReader reader) throws IOException {
        logger.trace("Reading files");
        logger.trace("==============");
        int numberOfLinesRead = reader.readFiles(curWatchMap.values());
        Registrar.writeStateToJson(sincedbFile, curWatchMap.values());
        return numberOfLinesRead;
    }
    
    @SuppressWarnings("UseSpecificCatch")
    public void setSincedb(String sincedbFile) {
        this.sincedbFile = sincedbFile;
        try {
            logger.debug("Loading saved states");
            savedStates = Registrar.readStateFromJson(sincedbFile);
        }
        catch (Exception e) {
            logger.warn("Could not load saved states : " + e.getMessage()); // was (..., e)
        }
    }
    
    public String getSincedbFile() {
        return sincedbFile;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }
    
    public Collection<FileState> getWatched() {
        return curWatchMap.values();
    }

}
