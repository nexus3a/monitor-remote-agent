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
 *
 */

import com.monitor.agent.server.filter.Filter;
import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class FileModificationListener implements FileAlterationListener {

    private final PredefinedFields fields;
    private final FileWatcher watcher;
    private final Filter filter;
    private final String encoding;

    public FileModificationListener(FileWatcher watcher, PredefinedFields fields, Filter filter, String encoding) {
        this.watcher = watcher;
        this.fields = fields;
        this.filter = filter;
        this.encoding = encoding;
    }

    @Override
    public void onDirectoryChange(File file) {
        // Not implemented
    }

    @Override
    public void onDirectoryCreate(File file) {
        // Not implemented
    }

    @Override
    public void onDirectoryDelete(File file) {
        // Not implemented
    }

    @Override
    public void onFileChange(File file) {
        watcher.onFileChange(file, fields, filter, encoding);
    }

    @Override
    public void onFileCreate(File file) {
        watcher.onFileCreate(file, fields, filter, encoding);
    }

    @Override
    public void onFileDelete(File file) {
        watcher.onFileDelete(file);
    }

    @Override
    public void onStart(FileAlterationObserver file) {
        // Not implemented
    }

    @Override
    public void onStop(FileAlterationObserver file) {
        // Not implemented
    }

}
