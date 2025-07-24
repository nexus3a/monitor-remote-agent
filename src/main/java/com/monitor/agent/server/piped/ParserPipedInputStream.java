package com.monitor.agent.server.piped;

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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.charset.StandardCharsets;

public class ParserPipedInputStream extends PipedInputStream {
    
    private final ParserPipedStream pipe;
    private final int timeout;
    private boolean eos = false;

    public ParserPipedInputStream(ParserPipedStream pipe, int pipeSize) {
        super(pipeSize);
        this.pipe = pipe;
        this.timeout = pipe.getTimeout();
    }

    public ParserPipedInputStream(ParserPipedStream pipe) {
        this(pipe, 1024);
    }

    public ParserPipedStream getPipe() {
        return pipe;
    }
    
    public void enough() {
        this.eos = true;
    }
    
    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        long now = System.currentTimeMillis();
        long till = now + timeout; // ещё вариант: times = timeout / 50;
        while (!eos && available() == 0 && now < till) {
            notifyAll();
            try {
                wait(1);
            }
            catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
            now = System.currentTimeMillis();
        }
        if (now >= till) {
//          pipe.getOutput().write(ParserPipedStream.INTERRUPTED_BY_TIMEOUT.getBytes(StandardCharsets.UTF_8));
//          pipe.getOutput().close();
//          throw new InterruptedByTimeoutException();
        }
        return super.read(b, off, available());
    }

    @Override
    public void close() throws IOException {
        pipe.getOutput().close();
        super.close();
        pipe.close();
    }
    
}
