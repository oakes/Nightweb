/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;

/**
 * The base class from which all servlets extend.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public abstract class GenericServlet implements Servlet, ServletConfig,
        Serializable {
    private ServletConfig config;

    public GenericServlet() {
    }

    public String getInitParameter(String name) {
        return config.getInitParameter(name);
    }

    public Enumeration getInitParameterNames() {
        return config.getInitParameterNames();
    }

    public ServletConfig getServletConfig() {
        return this.config;
    }

    public void init(ServletConfig config) throws ServletException {
        this.config = config;
        init();
    }

    public void init() throws ServletException {
    }

    public void destroy() {
    }

    public ServletContext getServletContext() {
        return config.getServletContext();
    }

    public String getServletInfo() {
        return "";
    }

    public String getServletName() {
        return config.getServletName();
    }

    public void log(String msg) {
        config.getServletContext().log(msg);
    }

    public void log(String message, Throwable t) {
        config.getServletContext().log(message, t);
    }

    public abstract void service(ServletRequest req, ServletResponse res)
            throws IOException, ServletException;
}
