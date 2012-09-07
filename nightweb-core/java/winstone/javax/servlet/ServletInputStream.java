/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

/**
 * Provides the base class for servlet request streams.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public abstract class ServletInputStream extends java.io.InputStream {
    protected ServletInputStream() {
        super();
    }

    public int readLine(byte[] b, int off, int len) throws java.io.IOException {
        if (b == null)
            throw new IllegalArgumentException("null buffer");
        else if (len + off > b.length)
            throw new IllegalArgumentException(
                    "offset + length is greater than buffer length");

        int positionCounter = 0;
        int charRead = read();
        while (charRead != -1) {
            b[off + positionCounter++] = (byte) charRead;
            if ((charRead == '\n') || (off + positionCounter == len)) {
                return positionCounter;
            } else {
                charRead = read();
            }
        }
        return -1;
    }

}
