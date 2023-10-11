package com.monitor.agent.server;

/*
 * Copyright 2021 Aleksei Andreev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.monitor.agent.server.filter.Filter;
import java.io.File;
import java.util.Objects;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

class DirectoryChangeListener implements FileAlterationListener {
    
    private final DirectoryWatcher watcher;
    private final String wildCard;
    private final IOFileFilter fileFilter;
    private final PredefinedFields fields;
    private final Filter filter;
    private final String encoding;

    DirectoryChangeListener(DirectoryWatcher watcher, String wildCard, IOFileFilter fileFilter, PredefinedFields fields, Filter filter, String encoding) {
        this.watcher = watcher;
        this.wildCard = wildCard;
        this.fileFilter = fileFilter;
        this.fields = fields;
        this.filter = filter;
        this.encoding = encoding;
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
    }

    @Override
    public void onDirectoryCreate(File directory) {
        watcher.onDirectoryCreate(directory, this);
    }

    @Override
    public void onDirectoryChange(File directory) {
        watcher.onDirectoryChange(directory, this);
    }

    @Override
    public void onDirectoryDelete(File directory) {
        watcher.onDirectoryDelete(directory, this);
    }

    @Override
    public void onFileCreate(File file) {
    }

    @Override
    public void onFileChange(File file) {
    }

    @Override
    public void onFileDelete(File file) {
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
    }

    public IOFileFilter getFileFilter() {
        return fileFilter;
    }

    public PredefinedFields getFields() {
        return fields;
    }

    public Filter getFilter() {
        return filter;
    }

    public String getWildCard() {
        return wildCard;
    }

    public String getEncoding() {
        return encoding;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.wildCard);
        hash = 89 * hash + Objects.hashCode(this.fileFilter);
        hash = 89 * hash + Objects.hashCode(this.fields);
        hash = 89 * hash + Objects.hashCode(this.filter);
        hash = 89 * hash + Objects.hashCode(this.encoding);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DirectoryChangeListener other = (DirectoryChangeListener) obj;
        if (!Objects.equals(this.wildCard, other.wildCard)) {
            return false;
        }
        if (!Objects.equals(this.fileFilter, other.fileFilter)) {
            return false;
        }
        if (!Objects.equals(this.fields, other.fields)) {
            return false;
        }
        if (!Objects.equals(this.filter, other.filter)) {
            return false;
        }
        return encoding.equalsIgnoreCase(other.encoding);
    }
    
}
