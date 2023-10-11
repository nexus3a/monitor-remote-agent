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

import com.monitor.agent.server.Server;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.router.RouterNanoHTTPD;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class TJLogConfigHandler extends DefaultResponder {
    
    private static Document readXml(File file) 
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(file);
    }

    private static Document readXml(String data) 
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new BufferedInputStream(new ByteArrayInputStream(data.getBytes("UTF-8"))));
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
        
        Response response;
        
        try {
            
            RequestParameters parameters = getParameters();

            // получаем полное имя файла настройки логирования - logcfg.xml
            //
            String fileName = (String) parameters.get("filename", null);
            if (fileName == null) {
                throw new IllegalArgumentException("Не указан параметр \"filename\" в запросе");
            }
            if (!fileName.toLowerCase().replaceAll("\\\\", "/").endsWith("/logcfg.xml")) {
                throw new IllegalArgumentException("Нельзя обращаться к файлу, отличному от \"logcfg.xml\"");
            }

            // получаем содержимое имя файла настройки логирования; используется только если
            // необходимо записать указанный файл с переданными данными; если он не задан, то  
            // это значит, что мы хотим прочитать содержимое указанного файла
            //
            String fileData = (String) parameters.get("filedata", null);
            boolean writeData = !(fileData == null || fileData.isEmpty());
            
            if (writeData) {
                assert fileData != null;
                // записываемые данные должны быть формата xml - нельзя записывать под
                // видом logcfg.xml всякие исполняемые файлы - проверка для безопасности
                //
                try {
                    readXml(fileData);
                }
                catch (ParserConfigurationException | SAXException | IOException ex) {
                    throw new IllegalStateException("Данные для записи не в формате XML", ex);
                }

                try (FileOutputStream fos = new FileOutputStream(new File(fileName))) {
                    fos.write(fileData.getBytes("UTF-8"));
                }
                catch (IOException ex) {
                    throw new IllegalStateException("Ошибка при записи данных в logcfg.xml", ex);
                }

                response = NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        NanoHTTPD.MIME_PLAINTEXT,
                        "OK");
            }
            else {
                // получаемые данные должны быть формата xml - не должны случайно прихватить
                // важные данные под видом logcfg.xml - проверка для безопасности
                //
                File xmlFile = new File(fileName);
                FileInputStream fis;
                try {
                    fis = new FileInputStream(xmlFile);
                    readXml(xmlFile);
                }
                catch (FileNotFoundException ex) {
                    throw new IllegalStateException("Не найден файл \"logcfg.xml\"", ex);
                }
                catch (IOException ex) {
                    throw new IllegalStateException("Ошибка при чтении файла \"logcfg.xml\" (" + ex.getMessage() + ")", ex);
                }
                catch (ParserConfigurationException | SAXException ex) {
                    throw new IllegalStateException("Файл \"logcfg.xml\" не распознан как файл формата XML", ex);
                }
                
                response = NanoHTTPD.newChunkedResponse(
                        NanoHTTPD.Response.Status.OK,
                        NanoHTTPD.MIME_PLAINTEXT,
                        fis);
                response.setGzipEncoding(true);
            }
        }
        catch (Exception ex) {
            response = NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    ex.getMessage());
        }

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
