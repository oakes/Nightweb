/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.util.Locale;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Base response interface definition.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ServletResponse {
    public void flushBuffer() throws IOException;

    public int getBufferSize();

    public void reset();

    public void resetBuffer();

    public void setBufferSize(int size);

    public boolean isCommitted();

    public String getCharacterEncoding();

    public void setCharacterEncoding(String charset);

    public String getContentType();

    public void setContentType(String type);

    public void setContentLength(int len);

    public Locale getLocale();

    public void setLocale(Locale loc);

    public ServletOutputStream getOutputStream() throws IOException;

    public PrintWriter getWriter() throws IOException;
}
