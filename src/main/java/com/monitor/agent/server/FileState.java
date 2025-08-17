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
 * - removed "multiline"
 * - added "encoding"
 * - added "logFormat"
 * - added "newPointer"
 *
 */

import com.monitor.agent.server.filter.Filter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FileState {

    @JsonIgnore
    private File file;
    private String directory;
    private String fileName;
    @JsonIgnore
    private long lastModified;
    @JsonIgnore
    private long size;
    @JsonIgnore
    private boolean deleted = false;
    private long signature;
    private int signatureLength;
    @JsonIgnore
    private boolean changed = false;
    @JsonIgnore
    private RandomAccessFile raFile;
    private long pointer = 0;
    private long newPointer = 0; // указатель на позицию после чтения записей лога ДО подтверждения получения клиентом; ПОСЛЕ подтверждения нужно pointer = newPointer
    @JsonIgnore
    private FileState oldFileState;
    @JsonIgnore
    private PredefinedFields fields;
    @JsonIgnore
    private Filter filter;
    @JsonIgnore
    private boolean matchedToNewFile = false;
    @JsonProperty("log format")
    private LogFormat logFormat;
    private String encoding;
    
    public FileState() {
    }

    public FileState(File file) throws IOException {
        this.file = file;
        directory = file.getCanonicalFile().getParent();
        fileName = file.getName();
        raFile = null;
        lastModified = file.lastModified();
        size = file.length();
        logFormat = null;
        encoding = "UTF-8";
    }

    private void setFileFromDirectoryAndName() throws FileNotFoundException, IOException {
        file = new File(directory + File.separator + fileName);
        if (file.exists()) {
            directory = file.getCanonicalFile().getParent();
            raFile = null;
            lastModified = file.lastModified();
            size = file.length();
        }
        else {
            deleted = true;
        }
    }

    public File getFile() {
        return file;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) throws FileNotFoundException, IOException {
        this.directory = directory;
        if (fileName != null && directory != null) {
            setFileFromDirectoryAndName();
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) throws FileNotFoundException, IOException {
        this.fileName = fileName;
        if (fileName != null && directory != null) {
            setFileFromDirectoryAndName();
        }
    }

    public boolean isDeleted() {
        return deleted || file == null || !file.exists();
    }

    public void setDeleted() {
        deleted = true;
    }

    public boolean hasChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public long getSignature() {
        return signature;
    }

    public void setSignature(long signature) {
        this.signature = signature;
    }

    @JsonIgnore
    public RandomAccessFile getRandomAccessFile() {
        return file.exists() ? raFile : null;
    }

    @JsonIgnore
    public RandomAccessFile getOpenedRandomAccessFile() {
        if (!isRandomAccessFileOpened()) {
            try {
                // reopen random access file if it closed
                raFile = new RandomAccessFile(file, "r");
            }
            catch (FileNotFoundException ex) {
                raFile = null; // was nothing
            }
        }
        return raFile;
    }

    public long getPointer() {
        return pointer;
    }

    public void setPointer(long pointer) {
        this.pointer = pointer;
    }

    public long getNewPointer() {
        return newPointer;
    }

    public void setNewPointer(long newPointer) {
        this.newPointer = newPointer;
    }
    
    public int getSignatureLength() {
        return signatureLength;
    }

    public void setSignatureLength(int signatureLength) {
        this.signatureLength = signatureLength;
    }

    public FileState getOldFileState() {
        return oldFileState;
    }

    public void setOldFileState(FileState oldFileState) {
        this.oldFileState = oldFileState;
        oldFileState.setMatchedToNewFile(true);
    }
    
    @JsonIgnore
    public boolean isRandomAccessFileOpened() {
        if (raFile == null) {
            return false;
        }
        try {
            raFile.getFilePointer(); // check for random access file readability
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
    
    public final boolean closeRandomAccessFile() {
        try {
            if (isRandomAccessFileOpened()) {
                getRandomAccessFile().close();
            }
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public void deleteOldFileState() {
        if (oldFileState.closeRandomAccessFile()) {
            oldFileState = null;
        }
    }

    public PredefinedFields getFields() {
        return fields;
    }

    public void setFields(PredefinedFields fields) {
        this.fields = fields;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public LogFormat getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(LogFormat logFormat) {
        this.logFormat = logFormat;
    }

    public void setLogFormat(String kind) {
        if ("one_c_tech_log".equalsIgnoreCase(kind)) {
            this.logFormat = LogFormat.ONE_C_TECH_LOG;
        }
        else if ("one_c_reg_log".equalsIgnoreCase(kind)) {
            this.logFormat = LogFormat.ONE_C_REG_LOG;
        }
        else if ("perfomance_monitor".equalsIgnoreCase(kind)) {
            this.logFormat = LogFormat.PERFOMANCE_MONITOR;
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isMatchedToNewFile() {
        return matchedToNewFile;
    }

    public void setMatchedToNewFile(boolean matchedToNewFile) {
        this.matchedToNewFile = matchedToNewFile;
    }
    
    public long length() {
        return file == null ? 0 : file.length();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("fileName", fileName).
                append("directory", directory).
                append("pointer", pointer).
                append("newPointer", newPointer).
                append("signature", signature).
                append("signatureLength", signatureLength).
                append("encoding", encoding).
                toString();
    }

}
