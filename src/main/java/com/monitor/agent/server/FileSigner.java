package com.monitor.agent.server;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.Adler32;

public class FileSigner {

    private static final Adler32 adler32 = new Adler32();

    public static long computeSignature(FileState state, int signatureLength) throws IOException {
        boolean opened = state.isRandomAccessFileOpened();
        RandomAccessFile file = state.getOpenedRandomAccessFile(); // opening for signature computation
        byte[] input = new byte[signatureLength];
        file.seek(0);
        file.read(input);
        if (!opened) {
            state.closeRandomAccessFile(); // restore raf open/close state after opening for signature computation
        }
        synchronized (adler32) {
            adler32.reset();
            adler32.update(input);
            return adler32.getValue();
        }
    }
}
