package com.monitor.agent.server;

/*
 * Copyright 2021 Aleksei Andreev
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
 */

import com.monitor.agent.server.filter.Filter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.Logger;

/**
 * Задача отслеживателя каталогов - во-первых, определить список каталогов, удовлетворяющих
 * условиям маски имён каталогов, например, "c:\program*\java\*\**\", где \*\ обозначает
 * ровно один каталог, а \**\ - ноль и более вложенных каталогов. Во-вторых, отследить
 * изменения внутри найденных каталогов - если внутри будут созданы, переименованы, удалены
 * каталоги, и они удовлетворяют условиям определённой ранее маски, то отслеживателю каталогов 
 * нужно будет сообщить об этом отслеживателю файлов, чтобы тот, в свою очередь, или начал 
 * отслеживать в этих каталогах файлы логов по маске уже имени файла (а не каталога), или 
 * перестал отслеживать такие файлы
 */
public class DirectoryWatcher {
    
    private static final Logger logger = Logger.getLogger(DirectoryWatcher.class);
    
    private final Map<DirectoryChangeListener, Set<File>> directories;
    private final Map<File, FileAlterationObserver> observersMap;
    private final FileWatcher fileWatcher;
    private final Map<WatchKey, DirectoryChangeListener> listeners;
    
    /**
     * Набор: маска каталога (полученная из маски полного имени файла логов), фильтр файлов
     * (тоже полученный из маски полного имени файла логов), комплект полей по умолчанию для
     * запией лог-файла, фильтр записей лог-файла - это поля, по которым создаётся множество
     * файлов, которые отслеживаются, разбираются и передаются на сервер сбора журналов; эти
     * параметры задаются в SinceDB-файле, и их нужно передавать в процедуры отслеживателя 
     * файлов, чтобы начать работу с файлами лога. Каждый такой набор связан с одним или 
     * несколькими каталогами (в зависимости от наличия * в маске каталога), и к каждому
     * каталогу привязывается слушатель изменений каталога - соответственно, каждый такой
     * набор можно связать со слушателем изменений каталога. Для использования указанного
     * набора в качестве ключа карты слушателей и создан данный класс (см. listeners)
     */
    public static class WatchKey {
        
        String wildCard;
        IOFileFilter fileFilter;
        PredefinedFields fields;
        Filter filter;
        String encoding;

        public WatchKey(String wildCard, IOFileFilter fileFilter, PredefinedFields fields, Filter filter, String encoding) {
            this.wildCard = wildCard;
            this.fileFilter = fileFilter;
            this.fields = fields;
            this.filter = filter;
            this.encoding = encoding;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + Objects.hashCode(this.wildCard);
            hash = 37 * hash + Objects.hashCode(this.fileFilter);
            hash = 37 * hash + Objects.hashCode(this.fields);
            hash = 37 * hash + Objects.hashCode(this.filter);
            hash = 37 * hash + Objects.hashCode(this.encoding);
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
            final WatchKey other = (WatchKey) obj;
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
            return this.encoding.equalsIgnoreCase(other.encoding);
        }

        @Override
        public String toString() {
            return String.format("ListenerKey[ %s, %s, %s, %s, %s ]", wildCard, fileFilter, fields, filter, encoding);
        }
    }

    
    public DirectoryWatcher() {
        this(null);
    }
    
    
    public DirectoryWatcher(FileWatcher fileWatcher) {
        directories = new HashMap<>();
        observersMap = new HashMap<>();
        listeners = new HashMap<>();
        /*
        deferred = new HashSet<>();
        */
        this.fileWatcher = fileWatcher;
    }
    
    
    /**
     * Проверяет - удовлетворяет ли имя каталога маске, приведённой к аналитическому
     * формату (массиву подмасок). Используется для принятия решения - подходит ли
     * указанный каталог для использования в отслеживателе файлов, связанном с текущим
     * отслеживателем каталогов, или нет
     * 
     * @param directory исследуемый каталог
     * @param scans маска каталога, разбитая на подмаски с фрагментами со * и без *
     * @return true, если имя каталога удовлетворяет маске
     * @throws IOException 
     */
    protected boolean matches(File directory, List<String> scans) throws IOException {
        String path = directory.getCanonicalPath();
        if (Server.isCaseInsensitiveFileSystem()) {
            path = path.toLowerCase();
        }
        String separator = File.separator;
        String wildCard = "";
        for (int i = 0; i < scans.size(); i++) {
            String scan = scans.get(i);
            if ("**".equals(scan)) {
                scan = "[\\\\\\S]+";
            }
            else {
                scan = scan.replaceAll("\\\\", "\\\\\\\\")
                        .replaceAll("\\*", "\\\\S+")
                        .replaceAll("\\?", "\\\\S");
            }
            wildCard += (i == 0 ? scan : ("\\".equals(separator) ? separator + separator : separator) + scan);
        }
        return path.matches(wildCard);
    }
    
    
    /**
     * Проверяет - удовлетворяет ли имя каталога маске. Используется для принятия 
     * решения - подходит ли указанный каталог для использования в отслеживателе файлов, 
     * связанном с текущим отслеживателем каталогов, или нет
     * 
     * @param directory исследуемый каталог
     * @param wildCard маска каталога строкой
     * @return true, если имя каталога удовлетворяет маске
     * @throws IOException 
     */
    protected boolean matches(File directory, String wildCard) throws IOException {
        return matches(directory, getScanParts(standardWildCard(wildCard)));
    }
    
    
    /**
     * Проверяет - может ли указанный каталог подходить маске, приведённой к аналитическому
     * формату, или нет. Например, если маска задана "c:\program*\java\*\*\bin\*\", то 
     * подкаталоги каталога "c:\program*\java\jre13" могут подходить по маске [если у него
     * будут соответствующие маске подкаталоги], а подкаталоги каталога 
     * "c:\program*\java\jre13\public\lib" никогда не смогут подойти по заданной маске
     * 
     * @param directory исследуемый каталог (потенциальный родитель других подкаталогов)
     * @param scans маска каталога, разбитая на подмаски с фрагментами со * и без *
     * @return true, если подкаталоги указанного каталога могут удовлетворить маске
     * @throws IOException 
     */
    protected boolean canMatch(File directory, List<String> scans) throws IOException {
        String path = directory.getCanonicalPath();
        if (Server.isCaseInsensitiveFileSystem()) {
            path = path.toLowerCase();
        }
        String separator = File.separator;
        String wildCard = "";
        for (int i = 0; i < scans.size(); i++) {
            String scan = scans.get(i);
            if ("**".equals(scan)) {
                return true;
            }
            scan = scan.replaceAll("\\\\", "\\\\\\\\")
                    .replaceAll("\\*", "\\\\S+")
                    .replaceAll("\\?", "\\\\S");
            wildCard += (i == 0 ? scan : ("\\".equals(separator) ? separator + separator : separator) + scan);
            if (path.matches(wildCard)) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * Проверяет - может ли указанный каталог подходить маске или нет. Например, если маска
     * задана "c:\program*\java\*\*\bin\*\", то  подкаталоги каталога 
     * "c:\program*\java\jre13" могут подходить по маске [если у него будут соответствующие
     * маске подкаталоги], а подкаталоги каталога "c:\program*\java\jre13\public\lib" 
     * никогда не смогут подойти по заданной маске
     * 
     * @param directory исследуемый каталог (потенциальный родитель других подкаталогов)
     * @param wildCard маска каталога строкой
     * @return true, если подкаталоги указанного каталога могут удовлетворить маске
     * @throws IOException 
     */
    protected boolean canMatch(File directory, String wildCard) throws IOException {
        return canMatch(directory, getScanParts(standardWildCard(wildCard)));
    }
    
    
    private FileAlterationObserver getCreateObserver(File directory, DirectoryChangeListener listener) {
        FileAlterationObserver observer = observersMap.get(directory);
        if (observer == null) {
            observer = new FileAlterationObserver(directory);
            observersMap.put(directory, observer);
        }
        Iterator<FileAlterationListener> iterator = observer.getListeners().iterator();
        while (iterator.hasNext()) {
            if (listener.equals(iterator.next())) {
                return observer;
            }
        }
        observer.addListener(listener);
        return observer;
    }
    
    
    private void deleteObserver(File directory, DirectoryChangeListener listener) {
        FileAlterationObserver observer = observersMap.get(directory);
        if (observer == null) {
            return;
        }
        observer.removeListener(listener);
        Iterator<FileAlterationListener> iterator = observer.getListeners().iterator();
        if (iterator.hasNext()) {
            return;
        }
        observersMap.remove(directory);
    }
    
    
    private Set<File> getCreateListenerDirectories(DirectoryChangeListener listener) {
        Set<File> result = directories.get(listener);
        if (result == null) {
            result = new HashSet<>();
            directories.put(listener, result);
        }
        return result;
    }
    
    
    /**
     * Разбивает маску имени каталога на подмаски, которые содержат фрагменты последовательности
     * каталогов без * и фрагменты с каталогми, в именах которых присутствует *. Такой набор
     * упрощает анализ иерархии каталогов на предмет подходимости имени каталога маске.
     * Пример.
     * маска "c:\programs\java\*\*\bin\*\" будет разбита на подмаски
     * [ "c:\programs\java", "*", "*", "bin", "*" ]
     * 
     * @param wildCard маска имени каталога строкой
     * @return список подстрок маски
     */
    private List<String> getScanParts(String wildCard) {
        String separator = File.separator;
        Pattern pattern = Pattern.compile(
                "(\\s%[^\\s%*]*\\*+[^\\s%*]*)"
                .replaceAll("s%", "\\".equals(separator) ? separator + separator : separator));
        Matcher matcher = pattern.matcher(wildCard);
        
        ArrayList<String> scans = new ArrayList<>();
        int pos = -1;
        while (matcher.find()) {
            if (pos < matcher.start()) {
                scans.add(wildCard.substring(pos + 1, matcher.start()));
            }
            pos = matcher.end();
            String scan = wildCard.substring(matcher.start() + 1, pos);
            if (!"**".equals(scan) || !scans.isEmpty() && !scans.get(scans.size() - 1).equals(scan)) {
                scans.add(scan);
            }
        }
        if (pos < wildCard.length() - 1) {
            scans.add(wildCard.substring(pos + 1));
        }
        
        return scans;
    }

    
    /**
     * Основная процедура класса. Осуществляет обход дерева каталогов и составляет коллекции
     * каталогов, подходящих по маске; назначает также слушателей изменений каталогов, чтобы
     * не упустить каталоги, удовлетворяющие маске, кторые могут быть созданы в будущем
     * 
     * @param root текущий рассматриваемый каталог
     * @param scans маска каталога, разбитая на подмаски с фрагментами со * и без *
     * @param level номер подмаски маски каталога
     * @param global признак распространения состояния подмаски на все подчинённые подкаталоги;
     * включатся при достижении специального вида подмаски "**", означающей "0 или более
     * подкаталогов"
     * @param listener слушатель изменений каталога, назначаемый просматриваемым каталогам,
     * чтобы отслеживать в них появление новых подкаталогов, которые могут удовлетворить
     * условиям маски
     */
    @SuppressWarnings("ManualArrayToCollectionCopy")
    private void walkDirectories(File root, List<String> scans, int level, boolean global, 
            DirectoryChangeListener listener) {
        if (global) {
            getCreateObserver(root, listener);
            walkDirectories(root, scans, level, false, listener);
            File[] childs = root.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
            if (childs == null) {
                return;
            }
            if (level >= scans.size() - 1) {
                Set<File> listenerDirectories = getCreateListenerDirectories(listener);
                listenerDirectories.add(root);
                for (File children : childs) {
                    listenerDirectories.add(children);
                }
            }
            for (File children : childs) {
                walkDirectories(children, scans, level, true, listener);
            }
            return;
        }
        if (level == 1) {
            Set<File> listenerDirectories = getCreateListenerDirectories(listener);
            listenerDirectories.add(root);
        }
        if (level >= scans.size()) {
            return;
        }
        String scan = scans.get(level);
        if ("**".equals(scan)) {
            walkDirectories(root, scans, level + 1, true, listener);
        }
        else if (scan.contains("*")) {
            getCreateObserver(root, listener);
            File[] childs = root.listFiles((FileFilter) FileFilterUtils.and(
                    FileFilterUtils.directoryFileFilter(),
                    new WildcardFileFilter(scan, IOCase.SYSTEM)));
            if (childs == null) {
                return;
            }
            if (level == scans.size() - 1) {
                Set<File> listenerDirectories = getCreateListenerDirectories(listener);
                for (File children : childs) {
                    listenerDirectories.add(children);
                }
            }
            else {
                for (File children : childs) {
                    walkDirectories(children, scans, level + 1, false, listener);
                }
            }
        }
        else {
            File children = new File(root, scan);
            if (children.exists() && children.isDirectory()) {
                if (level == scans.size() - 1) {
                    Set<File> listenerDirectories = getCreateListenerDirectories(listener);
                    listenerDirectories.add(children);
                }
                else {
                    walkDirectories(children, scans, level + 1, false, listener);
                }
            }
        }
    }
    
    
    /**
     * Приводит маску к "стандартному" виду - все символы строчные, все разделители
     * каталогов приведены к системному разделителю, в конце маски нет разделителя
     * каталогов, нет относительных путей
     * 
     * @param wildCard маска, как она задана поьзователем в SinceDB-файле
     * @return маска, приведённая к "стандартному" виду
     */
    private String standardWildCard(String wildCard) {
        return FilenameUtils.getFullPathNoEndSeparator(
                FilenameUtils.separatorsToSystem(
                FilenameUtils.normalize(Server.isCaseInsensitiveFileSystem() ? wildCard.toLowerCase() : wildCard)));
    }
    
    
    private DirectoryChangeListener getCreateListener(WatchKey key) {
        DirectoryChangeListener listener = listeners.get(key);
        if (listener == null) {
            listener = new DirectoryChangeListener(this, key.wildCard, key.fileFilter, key.fields, key.filter, key.encoding);
            listeners.put(key, listener);
        }
        return listener;
    }
    
    
    public List<String> getWildCardParts(String wildCard) {
        String stdWildCard = standardWildCard(wildCard);
        return getScanParts(stdWildCard);
    }
    
    
    public Set<File> addWildCardDirectories(String wildCard, IOFileFilter fileFilter, PredefinedFields fields, Filter filter, String encoding) {
        String stdWildCard = standardWildCard(wildCard);
        List<String> scans = getScanParts(stdWildCard);
        if (scans.isEmpty()) {
            return null;
        }
        File root = new File(scans.get(0));
        if (root.isDirectory() || !root.exists()) {
            WatchKey key = new WatchKey(stdWildCard, fileFilter, fields, filter, encoding);
            DirectoryChangeListener listener = getCreateListener(key);
            walkDirectories(root, scans, 1, false, listener);
            return directories.get(listener);
        }
        return null;
    }
    
    
    void onDirectoryCreate(File directory, DirectoryChangeListener listener) {
        try {
            logger.debug("Create detected on directory : " + directory.getCanonicalPath());
            List<String> scans = getScanParts(listener.getWildCard());
            if (canMatch(directory, scans)) {
                getCreateObserver(directory, listener);
                if (matches(directory, scans)) {
                    Set<File> listenerDirectories = getCreateListenerDirectories(listener);
                    if (!listenerDirectories.contains(directory)) {
                        listenerDirectories.add(directory);
                        if (fileWatcher != null) {
                            fileWatcher.onDirectoryCreate(directory, 
                                    listener.getFileFilter(), 
                                    listener.getFields(), 
                                    listener.getFilter(),
                                    listener.getEncoding());
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            logger.error("Caught IOException in onDirectoryCreate : " + e.getMessage());
        }
    }

    
    void onDirectoryChange(File directory, DirectoryChangeListener listener) {
        try {
            logger.debug("Change detected on directory : " + directory.getCanonicalPath());
            List<String> scans = getScanParts(listener.getWildCard());
            if (canMatch(directory, scans)) {
                getCreateObserver(directory, listener);
                if (matches(directory, scans)) {
                    Set<File> listenerDirectories = getCreateListenerDirectories(listener);
                    if (!listenerDirectories.contains(directory)) {
                        listenerDirectories.add(directory);
                        if (fileWatcher != null) {
                            fileWatcher.onDirectoryChange(directory, 
                                    listener.getFileFilter(), 
                                    listener.getFields(), 
                                    listener.getFilter(),
                                    listener.getEncoding());
                        }
                    }
                }
            }
            else {
                onDirectoryDelete(directory, listener);
            }
        }
        catch (IOException e) {
            logger.error("Caught IOException in onDirectoryChange : " + e.getMessage());
        }
    }

    
    void onDirectoryDelete(File directory, DirectoryChangeListener listener) {
        try {
            logger.debug("Deletion detected on directory : " + directory.getCanonicalPath());
            Set<File> listenerDirectories = getCreateListenerDirectories(listener);
            listenerDirectories.remove(directory);
            deleteObserver(directory, listener);
            if (fileWatcher != null) {
                fileWatcher.onDirectoryDelete(directory, 
                        listener.getFileFilter(), 
                        listener.getFields(), 
                        listener.getFilter());
            }
        }
        catch (IOException e) {
            logger.error("Caught IOException in onDirectoryDelete : " + e.getMessage());
        }
    }
    

    public void checkAndNotify() {
        // здесь коллекция observers динамически меняется в цикле, поэтому
        // мы вначале фиксируем коллекцию в отдельном списке, а затем ещё
        // дополнительно проверяем присутствие обрабатываемого observer'а
        // в основной коллекции, так как обработка предыдущего могла привести
        // к тому, что экземпляр текущего уже может быть удалён из основной
        // коллекции
        //
        List<FileAlterationObserver> observersList = new ArrayList<>(observersMap.values());
        for (FileAlterationObserver observer : observersList) {
            if (observersMap.containsValue(observer)) {
                observer.checkAndNotify();
            }
        }
    }


    public Set<File> getDirectories(String wildCard, IOFileFilter fileFilter, PredefinedFields fields, Filter filter, String encoding) {
        WatchKey key = new WatchKey(standardWildCard(wildCard), fileFilter, fields, filter, encoding);
        return directories.get(getCreateListener(key));
    }

    
}
