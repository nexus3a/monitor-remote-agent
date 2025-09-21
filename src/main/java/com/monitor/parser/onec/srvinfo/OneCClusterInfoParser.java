package com.monitor.parser.onec.srvinfo;


import static com.monitor.parser.onec.srvinfo.OneCSrvInfoParser.DEBUG_RECORDS;
import java.io.*;
import java.util.ArrayList;


public class OneCClusterInfoParser extends OneCSrvInfoParser {
    
    @Override
    protected void buildRecord(KeyValuesRecord kvrecord) throws IOException {
        if (DEBUG_RECORDS) { System.out.println("kvrecord = " + kvrecord); }

        OneCSrvInfoRecord logrec = kvrecord.lr;
        logrec.clear();
        byte kvreclength = kvrecord.count;
        
        if (kvreclength < 2) {
            return;
        }
        
        logrec.put("cluster", buildClusterValue(kvrecord.getComplex(1)));
        
        ArrayList<OneCSrvInfoRecord> infobasesValues = new ArrayList();
        KeyValuesRecord infobases = kvrecord.getComplex(2);
        for (int ci = 1; ci <= infobases.count; ci++) { // первое значение пропускаем - там количество инфобаз
            OneCSrvInfoRecord infobaseValue = new OneCSrvInfoRecord();
            infobasesValues.add(infobaseValue);
            KeyValuesRecord infobase = infobases.getComplex(ci);
            infobaseValue.put("uuid", infobase.getSimple(0));
            infobaseValue.put("name", infobase.getSimple(1));
            infobaseValue.put("description", infobase.getSimple(2));
            infobaseValue.put("dbm-system", infobase.getSimple(3));
            infobaseValue.put("dbm-server", infobase.getSimple(4));
            infobaseValue.put("dbm-base", infobase.getSimple(5));
            infobaseValue.put("dbm-user", infobase.getSimple(6));
            infobaseValue.put("dbm-password-hash", infobase.getSimple(7));
            infobaseValue.put("dbm-connection-string", infobase.getSimple(8));
            infobaseValue.put("secure-connection", "1".equals(infobase.getSimple(9)) ? "true" : "false");
            KeyValuesRecord ssb = infobases.getComplex(10);
            OneCSrvInfoRecord seansStartBlocking = new OneCSrvInfoRecord();
            seansStartBlocking.put("active", "1".equals(ssb.getSimple(0)) ? "true" : "false");
            seansStartBlocking.put("date-from", ssb.getSimple(1));
            seansStartBlocking.put("date-to", ssb.getSimple(2));
            seansStartBlocking.put("message", ssb.getSimple(3));
            seansStartBlocking.put("unblock-code", ssb.getSimple(4));
            seansStartBlocking.put("unblock-parameter", ssb.getSimple(5));
            infobaseValue.put("seans-start-blocking", seansStartBlocking);
            infobaseValue.put("scheduled-jobs-blocked", "1".equals(infobase.getSimple(11)) ? "true" : "false");
            infobaseValue.put("licensing-by-1c-server", "1".equals(infobase.getSimple(12)) ? "true" : "false");
            infobaseValue.put("seance-outer-management", infobase.getSimple(13));
            infobaseValue.put("strict-outer-management-using", "1".equals(infobase.getSimple(14)) ? "true" : "false");
            infobaseValue.put("security-profile", infobase.getSimple(15));
            infobaseValue.put("safe-mode-security-profile", infobase.getSimple(16));

            infobaseValue.put("field17", infobase.getSimple(17));
            infobaseValue.put("field18", infobase.getSimple(18));
            infobaseValue.put("field19", infobase.getSimple(19));
            infobaseValue.put("field20", infobase.getSimple(20));
            infobaseValue.put("field21", infobase.getSimple(21));
            infobaseValue.put("field22", infobase.getSimple(22));
            infobaseValue.put("field23", infobase.getSimple(23));
            infobaseValue.put("field24", infobase.getSimple(24));
            infobaseValue.put("field25", infobase.getSimple(25));
        }
        logrec.put("infobases", infobasesValues);

        KeyValuesRecord cluster3 = kvrecord.getComplex(3); // рабочие серверы (?)
        
        ArrayList<OneCSrvInfoRecord> adminsValues = new ArrayList();
        KeyValuesRecord admins = kvrecord.getComplex(4); // администраторы кластера
        for (int ci = 1; ci <= admins.count; ci++) { // первое значение пропускаем - там количество администраторов
            OneCSrvInfoRecord adminValue = new OneCSrvInfoRecord();
            adminsValues.add(adminValue);
            KeyValuesRecord admin = admins.getComplex(ci);
            adminValue.put("name", admin.getSimple(0));
            adminValue.put("description", admin.getSimple(1));
            String admin2 = admin.getSimple(2); // ?
            adminValue.put("password-hash", admin.getSimple(3));
            adminValue.put("os-user", admin.getSimple(4));
            adminValue.put("auth-by-password", admin.getSimple(5).equals("1") || admin.getSimple(5).equals("3"));
            adminValue.put("auth-by-os", admin.getSimple(5).equals("2") || admin.getSimple(5).equals("3"));
        }
        logrec.put("admins", adminsValues);

        ArrayList<OneCSrvInfoRecord> serversValues = new ArrayList();
        KeyValuesRecord servers = kvrecord.getComplex(5); // рабочие серверы и менеджеры кластера - массив
        for (int ci = 1; ci <= servers.count; ci++) { // первое значение пропускаем - там количество [рабочих] серверов
            OneCSrvInfoRecord serverValue = new OneCSrvInfoRecord();
            serversValues.add(serverValue);
            KeyValuesRecord server = servers.getComplex(ci);
            serverValue.put("uuid", server.getSimple(0));
            serverValue.put("description", server.getSimple(1));
            serverValue.put("port", server.getSimple(2));
            serverValue.put("name", server.getSimple(3));
            String server4 = server.getSimple(4);

            ArrayList<OneCSrvInfoRecord> portRangesValues = new ArrayList();
            KeyValuesRecord portRanges = server.getComplex(5);
            for (int di = 1; di <= portRanges.count; di++) { // первое значение пропускаем - там количество диапазонов
                OneCSrvInfoRecord portRangeValue = new OneCSrvInfoRecord();
                portRangesValues.add(portRangeValue);
                KeyValuesRecord portRange = portRanges.getComplex(di);
                portRangeValue.put("min-port", portRange.getSimple(0));
                portRangeValue.put("max-port", portRange.getSimple(1));
            }
            serverValue.put("port-ranges", portRangesValues);

            String server6 = server.getSimple(6);
            String server7 = server.getSimple(7); // хэш какой-то
            String server8 = server.getSimple(8);
            serverValue.put("safe-memory-consumption-per-call", server.getSimple(9));   // безопасный расход памяти за один вызов
            serverValue.put("number-of-ibs-per-process", server.getSimple(10)); // количество ИБ на процесс
            serverValue.put("number-of-connections-per-process", server.getSimple(11)); // количество соединений на процесс
            String server12 = server.getSimple(12);
            String server13 = server.getSimple(13);
            serverValue.put("manager-for-aech-service", "1".equals(server.getSimple(14)) ? "true" : "false"); // менеджер под каждый сервис (булево)
            String server15 = server.getSimple(15);
            String server16 = server.getSimple(16);
            serverValue.put("central-server", "1".equals(server.getSimple(17)) ? "true" : "false"); // центральный сервер
            serverValue.put("main-cluster-manager-port", server.getSimple(18)); // порт главного менеджера кластера
            serverValue.put("critical-memory-capacity-of-processes", server.getSimple(19)); // критический объём памяти процессов
            serverValue.put("temporarily-allowed-amount-of-memory-for-processes", server.getSimple(20)); // временно допустимый объём памяти процессов
            serverValue.put("interval-for-exceeding-allowed-memory-for-processes", server.getSimple(21)); // интервал превышения допустимого объёма памяти процессов
            serverValue.put("1c-enterprise-service-name", server.getSimple(22)); // имя службы (SPN) сервера 1С:Предприятия
            serverValue.put("speech-recognition-models-catalog", server.getSimple(23)); // каталог моделей распознавания речи
        }
        logrec.put("working-servers", serversValues);

        ArrayList<OneCSrvInfoRecord> managersValues = new ArrayList();
        KeyValuesRecord managers = kvrecord.getComplex(6); // менеджеры кластера
        for (int ci = 1; ci <= managers.count; ci++) { // первое значение пропускаем - там количество менеджеров
            OneCSrvInfoRecord managerValue = new OneCSrvInfoRecord();
            adminsValues.add(managerValue);
            KeyValuesRecord manager = managers.getComplex(ci);
            managerValue.put("uuid", manager.getSimple(0));
            managerValue.put("description", manager.getSimple(1));
            managerValue.put("name", manager.getSimple(2));
            managerValue.put("main", "1".equals(manager.getSimple(3)) ? "true" : "false"); // не точно!
            String admin4 = manager.getSimple(4); // ? число
            managerValue.put("server-uuid", manager.getSimple(5)); // идентификатор сервера, на котором работает менеджер
            String admin6 = manager.getSimple(6); // ? строка
        }
        logrec.put("managers", managersValues);

        KeyValuesRecord cluster7 = kvrecord.getComplex(7);
        KeyValuesRecord cluster8 = kvrecord.getComplex(8);
        KeyValuesRecord cluster9 = kvrecord.getComplex(9);
        KeyValuesRecord cluster10 = kvrecord.getComplex(10);
        KeyValuesRecord cluster11 = kvrecord.getComplex(11);
        KeyValuesRecord cluster12 = kvrecord.getComplex(12);
        String clusters13 = kvrecord.getSimple(13);
        KeyValuesRecord cluster14 = kvrecord.getComplex(14);
        KeyValuesRecord cluster15 = kvrecord.getComplex(15);
        KeyValuesRecord cluster16 = kvrecord.getComplex(16);
        KeyValuesRecord cluster17 = kvrecord.getComplex(17);
        KeyValuesRecord cluster18 = kvrecord.getComplex(18);
        KeyValuesRecord cluster19 = kvrecord.getComplex(19);
        KeyValuesRecord cluster20 = kvrecord.getComplex(20);
        KeyValuesRecord cluster21 = kvrecord.getComplex(21);
        KeyValuesRecord cluster22 = kvrecord.getComplex(22);
        String clusters23 = kvrecord.getSimple(23);
        KeyValuesRecord cluster24 = kvrecord.getComplex(24);
        KeyValuesRecord cluster25 = kvrecord.getComplex(25);
        KeyValuesRecord cluster26 = kvrecord.getComplex(26);
        KeyValuesRecord cluster27 = kvrecord.getComplex(27);
        KeyValuesRecord cluster28 = kvrecord.getComplex(28);
        String clusters29 = kvrecord.getSimple(29);
        
        if (DEBUG_RECORDS) { System.out.println(logrec); }                
    }
}
 
