/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.invoker;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import winstone.Logger;
import winstone.Mapping;
import winstone.RequestDispatcher;
import winstone.ServletConfiguration;
import winstone.WebAppConfiguration;
import winstone.WinstoneResourceBundle;

/**
 * If a URI matches a servlet class name, mount an instance of that servlet, and
 * try to process the request using that servlet.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: InvokerServlet.java,v 1.6 2006/03/24 17:24:24 rickknowles Exp $
 */
public class InvokerServlet extends HttpServlet {
//    private static final String FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
    private static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";

    private static final WinstoneResourceBundle INVOKER_RESOURCES = 
        new WinstoneResourceBundle("winstone.invoker.LocalStrings");
    private Map mountedInstances;
//    private String prefix;
//    private String invokerPrefix;

    /**
     * Set up a blank map of servlet configuration instances
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.mountedInstances = new Hashtable();
//        this.prefix = config.getInitParameter("prefix");
//        this.invokerPrefix = config.getInitParameter("invokerPrefix");
    }

    /**
     * Destroy any mounted instances we might be holding, then destroy myself
     */
    public void destroy() {
        if (this.mountedInstances != null) {
            synchronized (this.mountedInstances) {
                for (Iterator i = this.mountedInstances.values().iterator(); i
                        .hasNext();)
                    ((ServletConfiguration) i.next()).destroy();
                this.mountedInstances.clear();
            }
        }
        this.mountedInstances = null;
//        this.prefix = null;
//        this.invokerPrefix = null;
    }

    /**
     * Get an instance of the servlet configuration object
     */
    protected ServletConfiguration getInvokableInstance(String servletName)
            throws ServletException, IOException {
        ServletConfiguration sc = null;
        synchronized (this.mountedInstances) {
            if (this.mountedInstances.containsKey(servletName)) {
                sc = (ServletConfiguration) this.mountedInstances.get(servletName);
            }
        }

        if (sc == null) {
            // If found, mount an instance
            try {
                // Class servletClass = Class.forName(servletName, true,
                // Thread.currentThread().getContextClassLoader());
                sc = new ServletConfiguration((WebAppConfiguration) this.getServletContext(), 
                        getServletConfig().getServletName() + ":" + servletName, servletName,
                        new Hashtable(), -1);
                this.mountedInstances.put(servletName, sc);
                Logger.log(Logger.DEBUG, INVOKER_RESOURCES,
                        "InvokerServlet.MountingServlet", new String[] {
                                servletName,
                                getServletConfig().getServletName() });
                // just to trigger the servlet.init()
                sc.ensureInitialization(); 
            } catch (Throwable err) {
                sc = null;
            }
        }
        return sc;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse rsp)
            throws ServletException, IOException {
        boolean isInclude = (req.getAttribute(INCLUDE_PATH_INFO) != null);
//        boolean isForward = (req.getAttribute(FORWARD_PATH_INFO) != null);
        String servletName = null;

        if (isInclude)
            servletName = (String) req.getAttribute(INCLUDE_PATH_INFO);
//        else if (isForward)
//            servletName = (String) req.getAttribute(FORWARD_PATH_INFO);
        else if (req.getPathInfo() != null)
            servletName = req.getPathInfo();
        else
            servletName = "";
        if (servletName.startsWith("/"))
            servletName = servletName.substring(1);
        ServletConfiguration invokedServlet = getInvokableInstance(servletName);

        if (invokedServlet == null) {
            Logger.log(Logger.WARNING, INVOKER_RESOURCES,
                    "InvokerServlet.NoMatchingServletFound", servletName);
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, INVOKER_RESOURCES
                    .getString("InvokerServlet.NoMatchingServletFound",
                            servletName));
        } else {
            RequestDispatcher rd = new RequestDispatcher(
                    (WebAppConfiguration) getServletContext(), 
                    invokedServlet);
            rd.setForNamedDispatcher(new Mapping[0], new Mapping[0]);
            rd.forward(req, rsp);
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp)
            throws ServletException, IOException {
        doGet(req, rsp);
    }
}
