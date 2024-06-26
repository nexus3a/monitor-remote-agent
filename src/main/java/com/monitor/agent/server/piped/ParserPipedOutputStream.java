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
import java.io.PipedOutputStream;

public class ParserPipedOutputStream extends PipedOutputStream {
    
    private final ParserPipedStream pipe;

    public ParserPipedOutputStream(ParserPipedInputStream input) throws IOException {
        super(input);
        this.pipe = input.getPipe();
    }

    public ParserPipedStream getPipe() {
        return pipe;
    }

    @Override
    public void close() throws IOException {
        pipe.getInput().enough();
        super.close();
    }
    
}
