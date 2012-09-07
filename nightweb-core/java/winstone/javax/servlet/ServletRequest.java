/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Base request object interface definition.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ServletRequest {
    public Object getAttribute(String name);

    public Enumeration getAttributeNames();

    public String getCharacterEncoding();

    public int getContentLength();

    public String getContentType();

    public ServletInputStream getInputStream() throws IOException;

    public String getLocalAddr();

    public Locale getLocale();

    public Enumeration getLocales();

    public String getLocalName();

    public int getLocalPort();

    public String getParameter(String name);

    public Map getParameterMap();

    public Enumeration getParameterNames();

    public String[] getParameterValues(String name);

    public String getProtocol();

    public BufferedReader getReader() throws IOException;

    public String getRemoteAddr();

    public String getRemoteHost();

    public int getRemotePort();

    public RequestDispatcher getRequestDispatcher(String path);

    public String getScheme();

    public String getServerName();

    public int getServerPort();

    public boolean isSecure();

    public void removeAttribute(String name);

    public void setAttribute(String name, Object o);

    public void setCharacterEncoding(String enc) throws UnsupportedEncodingException;

    /**
     * @deprecated As of Version 2.1 of the Java Servlet API, use
     *             ServletContext.getRealPath(String) instead.
     */
    public String getRealPath(String path);
}
