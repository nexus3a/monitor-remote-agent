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
        
        KeyValuesRecord cluster3 = kvrecord.getComplex(3); // центральный сервер?
        KeyValuesRecord cluster4 = kvrecord.getComplex(4);
        KeyValuesRecord cluster5 = kvrecord.getComplex(5); // рабочие сервеы кластера - массив
        KeyValuesRecord cluster6 = kvrecord.getComplex(6); // менеджеры кластера - массив
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
        
        logrec.put("field3", "");
        logrec.put("field4", "");
        logrec.put("working-servers", new ArrayList());
        logrec.put("managers", new ArrayList());
        
        if (DEBUG_RECORDS) { System.out.println(logrec); }                
    }
}
 
