package com.monitor.parser.onec;

/* Generated By:JavaCC: Do not edit this line. SimpleCharStream.java Version 6.1 */
 /* JavaCCOptions:STATIC=false,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
/**
 * An implementation of interface CharStream, where the stream is assumed to contain only ASCII characters (without
 * unicode processing).
 */
public class SimpleCharStream {

    public static final boolean staticFlag = false; // является ли парсер статическим
    
    private static final int BUFF_INCREMENT = 1024 * 32 * 1000; // размер приращения буфера (если токен не помещается)
    private static final int MAX_TOKEN_LENGTH = 1024 * 32; // максимальное количество символов в токене по умолчанию
    private static final int MIN_XTRA_LENGTH = BUFF_INCREMENT * 2; // минимальное количество символов для xtra-области
    
    int tokenBegin; // позиция в буфере, с которой начинаются данные текущего токена
    int available; // позиция в буфере, до которой можно заполнять буфер, чтобы не затереть данные текущего токена в буфере
    protected int maxNextCharInd = 0; // позиция в буфере, до которой заполнены данные из потока; maxNextCharInd <= available

    public int bufpos = -1; // позиция внутри буфера
    protected int bufline[]; // буефер номеров строк
    protected int bufcolumn[]; // буфер номеров колонок

    protected long bytesRead = 0; // сколько всего эффективно прочитано байт с момента начала чтения (не в буфер)
    protected boolean bytePerChar = false; // true для кодировок, отличных от utf-8, utf-16 (один байт на символ)

    protected int column = 0; // текущая колонка в файле
    protected int line = 1; // текущая строка файла

    protected boolean prevCharIsCR = false;
    protected boolean prevCharIsLF = false;

    protected java.io.Reader inputStream; // поток, из которого читаются данные через буфер

    protected char[] buffer; // рабочий буфер с символами
    int bufsize; // размер рабочего буфера
    
    protected int inBuf = 0; // количество байт, ранее прочитанных из потока, находящихся перед bufPos (после отката назад)
    protected int tabSize = 1; // размер табуляции в символах для вычисления номера колонки в файле
    
    
    int xtraBegin; // позиция в буфере, с которой начинается xtra-область с перезаписываемыми данными
    int xtraSize; // размер xtra-области с перезаписываемыми данными
    int maxXtraSize; // размер xtra-области с перезаписываемыми данными, достигший предела расширения
    int tokenLen; // сколько прочитано символов с начала токена
    
    int maxTokenLen; // максимальное количество символов в токене
    

    public void setTabSize(int i) {
        tabSize = i;
    }

    public int getTabSize() {
        return tabSize;
    }

    protected void ExpandBuff(boolean wrapAround) {
        
        // перед физическим расширением буфера примем решение об использовании его xtra-области:
        // (xtra-область - это область буфера, удалённая от начала токена на расстояние более
        // масимально разрешенного - MAX_TOKEN_LENGTH символов - эту область можно использовать
        // для циклического перезатирания/перезаписи данных при чтении в буфер из потока)
        // если размер xtra-области ранее достиг определенного размера, то вместо физического
        // увеличения буфера сместим указатели на позицию начала xtra-области и буедм счетать,
        // что мы "уже увеличили" буфер на величину (bufsize - xtraBegin) символов
        //
        //
        //        tokenBegin       xtraBegin     bufsize         не-будем-расширять-до-сюда
        //    |-------X----------------X------------|. . . . . . |
        //                                        bufpos
        //
        //
        //        tokenBegin       xtraBegin     bufsize
        //    |-------X----------------X============|      используем перезатираемую область (=)
        //                           bufpos
        //
        
        if (xtraSize >= MIN_XTRA_LENGTH) {
            
            maxXtraSize = xtraSize;                   // после этого xtraSize не будет увеличиваться при чтении символов
            bufpos = xtraBegin;
        
            available = tokenBegin < xtraBegin ? bufsize : tokenBegin;
            maxNextCharInd = bufpos;

        }
        else {
        
            char[] newbuffer = new char[bufsize + BUFF_INCREMENT];
            int newbufline[] = new int[bufsize + BUFF_INCREMENT];
            int newbufcolumn[] = new int[bufsize + BUFF_INCREMENT];

            try {
                if (wrapAround) {
                    System.arraycopy(buffer, tokenBegin, newbuffer, 0, bufsize - tokenBegin);
                    System.arraycopy(buffer, 0, newbuffer, bufsize - tokenBegin, bufpos);
                    buffer = newbuffer;

                    System.arraycopy(bufline, tokenBegin, newbufline, 0, bufsize - tokenBegin);
                    System.arraycopy(bufline, 0, newbufline, bufsize - tokenBegin, bufpos);
                    bufline = newbufline;

                    System.arraycopy(bufcolumn, tokenBegin, newbufcolumn, 0, bufsize - tokenBegin);
                    System.arraycopy(bufcolumn, 0, newbufcolumn, bufsize - tokenBegin, bufpos);
                    bufcolumn = newbufcolumn;

                    bufpos += (bufsize - tokenBegin);
                }
                else {
                    System.arraycopy(buffer, tokenBegin, newbuffer, 0, bufsize - tokenBegin);
                    buffer = newbuffer;

                    System.arraycopy(bufline, tokenBegin, newbufline, 0, bufsize - tokenBegin);
                    bufline = newbufline;

                    System.arraycopy(bufcolumn, tokenBegin, newbufcolumn, 0, bufsize - tokenBegin);
                    bufcolumn = newbufcolumn;

                    bufpos -= tokenBegin;
                }
                if (xtraBegin != -1) {
                    if (xtraBegin >= tokenBegin) {
                        xtraBegin -= tokenBegin;
                    }
                    else {
                        xtraBegin += (bufsize - tokenBegin);
                    }
                }
            }
            catch (Throwable t) {
                throw new Error(t.getMessage());
            }

            bufsize += BUFF_INCREMENT;

            // в результате расширения буфера начало токена всегда будет находиться в его начале

            available = bufsize;
            maxNextCharInd = bufpos;
            tokenBegin = 0;
        }

    }

    protected void FillBuff() throws java.io.IOException {
        
    //    ,------------ 1. available -------------.
    //   /   3. bufpos                             \
    //  |--------X---------------------+------------| buffer           |  срабатывает, когда bufpos >= maxNextCharInd
    //   \                             /
    //    `---- 2. maxNextCharInd ----`
    //
    // 1. определяется available - сколько и где можно заполнять данные в буфере (до или после начала токена)
    // 2. заполняются данные в выбранной области из потока; данных в потоке может быть меньше, чем можно поместить
    //    в буфер в зону available, при следующем подчитывании из потока осташаяся область будет заполняться
    // 3. bufpos ползает по заполненным данным
    
    
        if (maxNextCharInd == available) {                             // уже заполнили по максимуму оставшееся ранее место - не важно - до конца буфера или до начала токена
            if (available == bufsize) {                                // режим работы с буфером "читаем до конца буфера"
                if (tokenBegin > BUFF_INCREMENT) {                     // закольцовываем данные:
                    bufpos = maxNextCharInd = 0;                       // - заполнение будет с начала буфера порцией BUFF_INCREMENT байт
                    available = tokenBegin;                            // - дочитывать можно будет не дальше, чем начало токена, с начала буфера, включается режим работы с буфером "дочитываем до начала токена"
                }
                else if (tokenBegin < 0) {                             // после BeginToken() в случае, когда bufpos в конце заполненных ранее данных
                    bufpos = maxNextCharInd = 0;                       // новый токен по стечению обстоятельств начнётся с начала буфера
                }
                else {                                                 // tokenBegin <= BUFF_INCREMENT - слева от начала токена мало места для дозаполнения
                    ExpandBuff(false);                                 // увеличиваем буфер сдвигом влево от начала токена, затирая всё, что до начала токена
                }
            }
            else if (available > tokenBegin) {                         // после BeginToken() в случае, когда bufpos ещё не добрался до конца заполненных ранее данных
                available = bufsize;                                   // продолжим работать в режиме "читаем до конца буфера"
            }
            else if ((tokenBegin - available) < BUFF_INCREMENT) {      // мы слева от начала токена, до начала токена осталось место, но слишком мало - расширим буфер
                ExpandBuff(true);                                      // увеличиваем буфер сдвигом влево от начала токена, перемщая всё, что до начала токена вправо
            }
            else {
                available = tokenBegin;                                // можем заполнить буфер аж до начала токена без расширения буфера - места много
            }
        }

        try {
            int i;
            if ((i = inputStream.read(buffer, maxNextCharInd, available - maxNextCharInd)) == -1) {  // заполняем буфер от maxNextCharInd до конца свободной области available данными из потока насколько хватает данных в нём
                inputStream.close();
                throw new java.io.IOException();
            }
            else {
                maxNextCharInd += i;
            }
        }
        catch (java.io.IOException e) {
            --bufpos;
            backup(0);
            if (tokenBegin == -1) { // после BeginToken()
                tokenBegin = bufpos;
            }
            throw e;
        }
    }

    /**
     * Read a character.
     * @return 
     * @throws java.io.IOException
     */
    public char readChar() throws java.io.IOException {
        tokenLen++;
        
        if (inBuf > 0) {
            --inBuf;

            if (++bufpos == bufsize) {
                bufpos = 0;           // закольцовываем указатель
            }
            
            return buffer[bufpos];
        }

        if (++bufpos >= maxNextCharInd) {
            FillBuff();               // прочитали всё, что раньше заполнили в буфере, нужно подчитать в буфер из потока
        }
        
        if (xtraBegin == -1) {
            if (tokenLen > maxTokenLen) {
                xtraBegin = bufpos;
                xtraSize = 0;
                maxXtraSize = -1;
            }
        }
        else if (xtraSize != maxXtraSize) {
            xtraSize++;
        }
        
        char c = buffer[bufpos];

        if (bytePerChar)
            bytesRead = bytesRead + 1;
        else if (c == 0)
            bytesRead = bytesRead + 2;
        else if (c < 128) 
            bytesRead = bytesRead + 1;
        else if (c < 2048)
            bytesRead = bytesRead + 2;
        else if (c < 55296)
            bytesRead = bytesRead + 3; // суррогатная пара
        else if (c < 57344)
            bytesRead = bytesRead + 2;
        else if (c < 65536)
            bytesRead = bytesRead + 3;
        else
            bytesRead = bytesRead + 4;

        UpdateLineColumn(c);
        return c;
    }

    /**
     * Start.
     * @return 
     * @throws java.io.IOException
     */
    public char BeginToken() throws java.io.IOException {
        tokenBegin = xtraBegin = maxXtraSize = -1;
        tokenLen = xtraSize = 0;
        char c = readChar();
        tokenBegin = bufpos;

        return c;
    }

    protected void UpdateLineColumn(char c) {
        column++;

        if (prevCharIsLF) {
            prevCharIsLF = false;
            line += (column = 1);
        }
        else if (prevCharIsCR) {
            prevCharIsCR = false;
            if (c == '\n') {
                prevCharIsLF = true;
            }
            else {
                line += (column = 1);
            }
        }

        switch (c) {
            case '\r':
                prevCharIsCR = true;
                break;
            case '\n':
                prevCharIsLF = true;
                break;
            case '\t':
                column--;
                column += (tabSize - (column % tabSize));
                break;
            default:
                break;
        }

        bufline[bufpos] = line;
        bufcolumn[bufpos] = column;
    }

    /**
     * Get token end column number.
     * @return 
     */
    public int getEndColumn() {
        return bufcolumn[bufpos];
    }

    /**
     * Get token end line number.
     * @return 
     */
    public int getEndLine() {
        return bufline[bufpos];
    }

    /**
     * Get token beginning column number.
     * @return 
     */
    public int getBeginColumn() {
        return bufcolumn[tokenBegin];
    }

    /**
     * Get token beginning line number.
     * @return 
     */
    public int getBeginLine() {
        return bufline[tokenBegin];
    }
    
    /**
     * Set maximum token length; 0 is the same as maxint
     * @param maxTokenLength 
     */
    public final void setMaxTokenLength(int maxTokenLength) {
        maxTokenLen = (maxTokenLength == 0) ? Integer.MAX_VALUE : maxTokenLength;
    }

    /**
     * Backup a number of characters.
     * @param amount
     */
    public void backup(int amount) {
        inBuf += amount;
        if ((bufpos -= amount) < 0) {
            bufpos += bufsize;
        }
        tokenLen -= amount;
        // TODO: учесть xtra-область
    }

    /**
     * Constructor.
     * @param dstream
     * @param encoding
     * @param startline
     * @param startcolumn
     * @param buffersize
     * @param maxTokenLength
     * @throws java.io.UnsupportedEncodingException
     */
    public SimpleCharStream(java.io.InputStream dstream, String encoding, int startline,
            int startcolumn, int buffersize, int maxTokenLength) throws java.io.UnsupportedEncodingException {

        inputStream = encoding == null 
                ? new java.io.InputStreamReader(dstream) 
                : new java.io.InputStreamReader(dstream, encoding);
        
        line = startline;
        column = startcolumn - 1;

        available = bufsize = buffersize;
        buffer = new char[buffersize];
        bufline = new int[buffersize];
        bufcolumn = new int[buffersize];
        
        setMaxTokenLength(maxTokenLength);

        bytePerChar = encoding == null
                ? false
                : (!"utf-8".equalsIgnoreCase(encoding) && !"utf-16".equalsIgnoreCase(encoding));
    }

    /**
     * Constructor.
     * @param dstream
     * @param encoding
     * @param startline
     * @param startcolumn
     * @param maxTokenLength
     * @throws java.io.UnsupportedEncodingException
     */
    public SimpleCharStream(java.io.InputStream dstream, String encoding, int startline,
            int startcolumn, int maxTokenLength) throws java.io.UnsupportedEncodingException {
        this(dstream, encoding, startline, startcolumn, BUFF_INCREMENT, maxTokenLength);
    }

    /**
     * Constructor.
     * @param dstream
     * @param encoding
     * @param startline
     * @param startcolumn
     * @throws java.io.UnsupportedEncodingException
     */
    public SimpleCharStream(java.io.InputStream dstream, String encoding, int startline,
            int startcolumn) throws java.io.UnsupportedEncodingException {
        this(dstream, encoding, startline, startcolumn, BUFF_INCREMENT, MAX_TOKEN_LENGTH);
    }

    /**
     * Reinitialise.
     * @param dstream
     * @param startline
     * @param startcolumn
     * @param buffersize
     */
    public void ReInit(java.io.Reader dstream, int startline, int startcolumn, int buffersize) {
        inputStream = dstream;
        bytesRead = 0;
        line = startline;
        column = startcolumn - 1;

        if (buffer == null || buffersize != buffer.length) {
            available = bufsize = buffersize;
            buffer = new char[buffersize];
            bufline = new int[buffersize];
            bufcolumn = new int[buffersize];
        }
        prevCharIsLF = prevCharIsCR = false;
        tokenBegin = inBuf = maxNextCharInd = tokenLen = xtraSize = 0;
        bufpos = xtraBegin = maxXtraSize = -1;
    }

    /**
     * Reinitialise.
     * @param dstream
     * @param startline
     * @param startcolumn
     */
    public void ReInit(java.io.Reader dstream, int startline, int startcolumn) {
        ReInit(dstream, startline, startcolumn, BUFF_INCREMENT);
    }

    /**
     * Reinitialise.
     * @param dstream
     */
    public void ReInit(java.io.Reader dstream) {
        ReInit(dstream, 1, 1, BUFF_INCREMENT);
    }

    /**
     * Reinitialise.
     * @param dstream
     * @param encoding
     * @param startline
     * @param startcolumn
     * @param buffersize
     * @throws java.io.UnsupportedEncodingException
     */
    public void ReInit(java.io.InputStream dstream, String encoding, int startline,
            int startcolumn, int buffersize) throws java.io.UnsupportedEncodingException {
        ReInit(encoding == null 
                ? new java.io.InputStreamReader(dstream) 
                : new java.io.InputStreamReader(dstream, encoding)
                , startline, startcolumn, buffersize);
    }

    /**
     * Reinitialise.
     * @param dstream
     * @param startline
     * @param startcolumn
     * @param buffersize
     */
    public void ReInit(java.io.InputStream dstream, int startline, int startcolumn, int buffersize) {
        ReInit(new java.io.InputStreamReader(dstream), startline, startcolumn, buffersize);
    }

    /**
     * Reinitialise.
     * @param dstream
     * @param encoding
     * @throws java.io.UnsupportedEncodingException
     */
    public void ReInit(java.io.InputStream dstream, String encoding) throws java.io.UnsupportedEncodingException {
        ReInit(dstream, encoding, 1, 1, BUFF_INCREMENT);
    }

    /**
     * Reinitialise.
     * @param dstream
     */
    public void ReInit(java.io.InputStream dstream) {
        ReInit(dstream, 1, 1, BUFF_INCREMENT);
    }

    /**
     * Reinitialise.
     * @param dstream
     * @param encoding
     * @param startline
     * @param startcolumn
     * @throws java.io.UnsupportedEncodingException
     */
    public void ReInit(java.io.InputStream dstream, String encoding, int startline,
            int startcolumn) throws java.io.UnsupportedEncodingException {
        ReInit(dstream, encoding, startline, startcolumn, BUFF_INCREMENT);
    }

    /**
     * Reinitialise.
     * @param dstream
     * @param startline
     * @param startcolumn
     */
    public void ReInit(java.io.InputStream dstream, int startline, int startcolumn) {
        ReInit(dstream, startline, startcolumn, BUFF_INCREMENT);
    }

    /**
     * Get token literal value.
     * @return 
     */
    public String GetImage() {
        if (tokenLen > maxTokenLen) {
            char first = buffer[tokenBegin];
            if (xtraBegin >= tokenBegin) {
                return new String(buffer, tokenBegin, xtraBegin - tokenBegin + 1) 
                        + " (... ещё " + (tokenLen - maxTokenLen) + " симв.)"
                        + (first == buffer[bufpos] ? first : "");
            }
            else {
                return new String(buffer, tokenBegin, bufsize - tokenBegin)
                        + new String(buffer, 0, xtraBegin + 1)
                        + " (... ещё " + (tokenLen - maxTokenLen) + " симв.)"
                        + (first == buffer[bufpos] ? first : "");
            }
        }
        else {
            if (bufpos >= tokenBegin) {
                return new String(buffer, tokenBegin, bufpos - tokenBegin + 1);
            }
            else {
                return new String(buffer, tokenBegin, bufsize - tokenBegin)
                        + new String(buffer, 0, bufpos + 1);
            }
        }
    }

    /**
     * Get the suffix.
     * @param len
     * @return 
     */
    public char[] GetSuffix(int len) {
        if (maxXtraSize > 0) {
            len = len % (maxXtraSize - maxTokenLen);
        }
        char[] ret = new char[len];

        if ((bufpos + 1) >= len) {
            System.arraycopy(buffer, bufpos - len + 1, ret, 0, len);
        }
        else {
            System.arraycopy(buffer, bufsize - (len - bufpos - 1), ret, 0, len - bufpos - 1);
            System.arraycopy(buffer, 0, ret, len - bufpos - 1, bufpos + 1);
        }

        return ret;
    }

    /**
     * Reset buffer when finished.
     */
    public void Done() {
        buffer = null;
        bufline = null;
        bufcolumn = null;
    }

    /**
     * Method to adjust line and column numbers for the start of a token.
     * @param newLine
     * @param newCol
     */
    public void adjustBeginLineColumn(int newLine, int newCol) {
        int start = tokenBegin;
        int len;

        if (bufpos >= tokenBegin) {
            len = bufpos - tokenBegin + inBuf + 1;
        }
        else {
            len = bufsize - tokenBegin + bufpos + 1 + inBuf;
        }

        int i = 0, j = 0, k = 0;
        int nextColDiff, columnDiff = 0;

        while (i < len && bufline[j = start % bufsize] == bufline[k = ++start % bufsize]) {
            bufline[j] = newLine;
            nextColDiff = columnDiff + bufcolumn[k] - bufcolumn[j];
            bufcolumn[j] = newCol + columnDiff;
            columnDiff = nextColDiff;
            i++;
        }

        if (i < len) {
            bufline[j] = newLine++;
            bufcolumn[j] = newCol + columnDiff;

            while (i++ < len) {
                if (bufline[j = start % bufsize] != bufline[++start % bufsize]) {
                    bufline[j] = newLine++;
                }
                else {
                    bufline[j] = newLine;
                }
            }
        }

        line = bufline[j];
        column = bufcolumn[j];
    }

}
