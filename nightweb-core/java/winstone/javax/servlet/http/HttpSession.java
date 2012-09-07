/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.http;

import java.util.Enumeration;

import javax.servlet.ServletContext;

/**
 * Interface for http sessions on the server.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface HttpSession {
    public Object getAttribute(String name);

    public Enumeration getAttributeNames();

    public long getCreationTime();

    public String getId();

    public long getLastAccessedTime();

    public int getMaxInactiveInterval();

    public ServletContext getServletContext();

    public void invalidate();

    public boolean isNew();

    public void removeAttribute(String name);

    public void setAttribute(String name, Object value);

    public void setMaxInactiveInterval(int interval);

    /**
     * @deprecated As of Version 2.1, this method is deprecated and has no
     *             replacement. It will be removed in a future version of the
     *             Java Servlet API.
     */
    public HttpSessionContext getSessionContext();

    /**
     * @deprecated As of Version 2.2, this method is replaced by
     *             getAttribute(java.lang.String).
     */
    public Object getValue(String name);

    /**
     * @deprecated As of Version 2.2, this method is replaced by
     *             getAttributeNames()
     */
    public String[] getValueNames();

    /**
     * @deprecated As of Version 2.2, this method is replaced by
     *             setAttribute(java.lang.String, java.lang.Object)
     */
    public void putValue(String name, Object value);

    /**
     * @deprecated As of Version 2.2, this method is replaced by
     *             removeAttribute(java.lang.String)
     */
    public void removeValue(String name);
}
