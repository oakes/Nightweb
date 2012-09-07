/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.util.Enumeration;
import java.net.URL;
import java.io.InputStream;
import java.util.Set;

/**
 * Models the web application concept as an interface.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ServletContext {
    public Object getAttribute(String name);

    public Enumeration getAttributeNames();

    public String getInitParameter(String name);

    public Enumeration getInitParameterNames();

    public String getServletContextName();

    public ServletContext getContext(String uripath);

    public String getServerInfo();

    public String getMimeType(String file);

    public int getMajorVersion();

    public int getMinorVersion();

    public RequestDispatcher getRequestDispatcher(String path);

    public RequestDispatcher getNamedDispatcher(String name);

    public String getRealPath(String path);

    public URL getResource(String path) throws java.net.MalformedURLException;

    public InputStream getResourceAsStream(String path);

    public Set getResourcePaths(String path);

    public String getContextPath();
    
    /**
     * @deprecated As of Java Servlet API 2.1, with no direct replacement.
     */
    public Servlet getServlet(String name) throws ServletException;

    /**
     * @deprecated As of Java Servlet API 2.1, with no replacement.
     */
    public Enumeration getServletNames();

    /**
     * @deprecated As of Java Servlet API 2.0, with no replacement.
     */
    public Enumeration getServlets();

    /**
     * @deprecated As of Java Servlet API 2.1, use log(String message, Throwable
     *             throwable) instead.
     */
    public void log(Exception exception, String msg);

    public void log(String msg);

    public void log(String message, Throwable throwable);

    public void removeAttribute(String name);

    public void setAttribute(String name, Object object);
}
