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

public class ParserPipedStream {
    
    public static final String INTERRUPTED_BY_TIMEOUT = "INTERRUPTED_BY_TIMEOUT";
    
    private final ParserPipedInputStream input;
    private final ParserPipedOutputStream output;
    private final int timeout;

    public ParserPipedStream(int timeout) throws IOException {
        this.timeout = timeout;
        this.input = new ParserPipedInputStream(this, 16 * 1024);
        this.output = new ParserPipedOutputStream(this.input);
    }

    public ParserPipedStream() throws IOException {
        this(5000);
    }

    public ParserPipedInputStream getInput() {
        return input;
    }

    public ParserPipedOutputStream getOutput() {
        return output;
    }

    public final int getTimeout() {
        return timeout;
    }
    
    public void write(byte[] b) throws IOException {
        output.write(b);
    }

    public void close() throws IOException {
        output.close();
    }
    
}
