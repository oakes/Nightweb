/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

/**
 * Thrown when a change to the servletContext occurs.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ServletContextListener extends java.util.EventListener {
    public void contextDestroyed(ServletContextEvent sce);

    public void contextInitialized(ServletContextEvent sce);
}
