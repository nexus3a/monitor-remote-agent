package com.monitor.parser.onec.srvinfo;


import java.io.*;


public class OneCClusterInfoParser extends OneCSrvInfoParser {
    
    @Override
    protected void buildRecord(KeyValuesRecord kvrecord) throws IOException {
        if (DEBUG_RECORDS) { System.out.println("kvrecord = " + kvrecord); }

        OneCSrvInfoRecord logrec = kvrecord.lr;
        logrec.clear();
        byte kvreclength = kvrecord.count;

        Object vo;
        for (byte kv = 0; kv <= kvreclength; kv++) {
            
            KeyValueBounds kvi = kvrecord.kv[kv];

            if (kvi.kvr == null) {
                vo = kvi.vv;
            }
            else {
                // вложенное значение
                vo = null;
            }
            
            String k = "field" + kv;
            
            logrec.put(k, vo);
            if (DEBUG_RECORDS) { System.out.println(k + "=" + vo); }                
        }
    }
}
 
