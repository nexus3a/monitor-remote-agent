package com.monitor.agent.server;

/*
 * Copyright 2015 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * https://github.com/apache/pdfbox/blob/a27ee91/fontbox/src/main/java/org/apache/fontbox/ttf/BufferedRandomAccessFile.java
 * 
 * This class is a version of the one published at https://code.google.com/p/jmzreader/wiki/BufferedRandomAccessFile
 * augmented to handle unsigned bytes. The original class is published under Apache 2.0 license. Fix is marked below
 *
 * This is an optimized version of the RandomAccessFile class as described by Nick Zhang on JavaWorld.com. The article
 * can be found at http://www.javaworld.com/javaworld/javatips/jw-javatip26.html
 *
 * @author jg
 */

public class BufferedRandomAccessFileStream extends InputStream {

    private final RandomAccessFile file;
    private final byte[] buffer; // Uses a byte instead of a char buffer for efficiency reasons
    private int bufend = 0;
    private int bufpos = 0;
    private long realpos = 0; // The position inside the actual file
    private final int BUFSIZE; // Buffer size

    public BufferedRandomAccessFileStream(String filename, String mode, int bufsize)
            throws FileNotFoundException, IOException {
        this(new RandomAccessFile(filename, mode), bufsize);
    }

    public BufferedRandomAccessFileStream(File file, String mode, int bufsize)
            throws FileNotFoundException, IOException {
        this(file.getAbsolutePath(), mode, bufsize);
    }

    public BufferedRandomAccessFileStream(RandomAccessFile raf, int bufsize) throws IOException {
        file = raf;
        BUFSIZE = bufsize;
        buffer = new byte[BUFSIZE];
        invalidate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int read() throws IOException {
        if (bufpos >= bufend && fillBuffer() < 0) {
            return -1;
        }
        if (bufend == 0) {
            return -1;
        }
        // FIX to handle unsigned bytes
        return (buffer[bufpos++] + 256) & 0xFF;
        // End of fix
    }

    /**
     * Reads the next BUFSIZE bytes into the internal buffer.
     *
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the
     * file has been reached.
     * @throws IOException If the first byte cannot be read for any reason other than end of file, or if the random
     * access file has been closed, or if some other I/O error occurs.
     */
    private int fillBuffer() throws IOException {
        int n = file.read(buffer, 0, BUFSIZE);

        if (n >= 0) {
            realpos += n;
            bufend = n;
            bufpos = 0;
        }
        return n;
    }

    /**
     * Clears the local buffer.
     *
     * @throws IOException If an I/O error occurs.
     */
    private void invalidate() throws IOException {
        bufend = 0;
        bufpos = 0;
        realpos = file.getFilePointer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int curLen = len; // length of what is left to read (shrinks)
        int curOff = off; // offset where to put read data (grows)
        int totalRead = 0;

        while (true) {
            int leftover = bufend - bufpos;
            if (curLen <= leftover) {
                System.arraycopy(buffer, bufpos, b, curOff, curLen);
                bufpos += curLen;
                return totalRead + curLen;
            }
            // curLen > leftover, we need to read more than what remains in buffer
            System.arraycopy(buffer, bufpos, b, curOff, leftover);
            totalRead += leftover;
            bufpos += leftover;
            if (fillBuffer() > 0) {
                curOff += leftover;
                curLen -= leftover;
            }
            else {
                if (totalRead == 0) {
                    return -1;
                }
                return totalRead;
            }
        }
    }

    public long getFilePointer() throws IOException {
        return realpos - bufend + bufpos;
    }

    public void seek(long pos) throws IOException {
        int n = (int) (realpos - pos);
        if (n >= 0 && n <= bufend) {
            bufpos = bufend - n;
        }
        else {
            file.seek(pos);
            invalidate();
        }
    }

    public long length() throws IOException {
        return file.length();
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

}
