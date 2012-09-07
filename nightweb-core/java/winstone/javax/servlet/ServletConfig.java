/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

/**
 * Basic servlet configuation interface
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ServletConfig {
    public String getInitParameter(String name);

    public java.util.Enumeration getInitParameterNames();

    public ServletContext getServletContext();

    public String getServletName();
}
