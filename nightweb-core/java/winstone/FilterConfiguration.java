/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import org.w3c.dom.Node;

/**
 * Corresponds to a filter object in the web app. Holds one instance only.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class FilterConfiguration implements javax.servlet.FilterConfig {
    final String ELEM_NAME = "filter-name";
    final String ELEM_DISPLAY_NAME = "display-name";
    final String ELEM_CLASS = "filter-class";
    final String ELEM_DESCRIPTION = "description";
    final String ELEM_INIT_PARAM = "init-param";
    final String ELEM_INIT_PARAM_NAME = "param-name";
    final String ELEM_INIT_PARAM_VALUE = "param-value";
    
    private String filterName;
    private String classFile;
    private Filter instance;
    private Map initParameters;
    private ServletContext context;
    private ClassLoader loader;
    private boolean unavailableException;
    private Object filterSemaphore = new Boolean(true);

    protected FilterConfiguration(ServletContext context, ClassLoader loader) {
        this.context = context;
        this.loader = loader;
        this.initParameters = new Hashtable();
    }

    /**
     * Constructor
     */
    public FilterConfiguration(ServletContext context, ClassLoader loader, Node elm) {
        this(context, loader);

        // Parse the web.xml file entry
        for (int n = 0; n < elm.getChildNodes().getLength(); n++) {
            Node child = elm.getChildNodes().item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE)
                continue;
            String nodeName = child.getNodeName();

            // Construct the servlet instances
            if (nodeName.equals(ELEM_NAME))
                this.filterName = WebAppConfiguration.getTextFromNode(child);
            else if (nodeName.equals(ELEM_CLASS))
                this.classFile = WebAppConfiguration.getTextFromNode(child);
            else if (nodeName.equals(ELEM_INIT_PARAM)) {
                String paramName = null;
                String paramValue = null;
                for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                    Node paramNode = child.getChildNodes().item(k);
                    if (paramNode.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                    else if (paramNode.getNodeName().equals(
                            ELEM_INIT_PARAM_NAME))
                        paramName = WebAppConfiguration.getTextFromNode(paramNode);
                    else if (paramNode.getNodeName().equals(
                            ELEM_INIT_PARAM_VALUE))
                        paramValue = WebAppConfiguration.getTextFromNode(paramNode);
                }
                if ((paramName != null) && (paramValue != null))
                    this.initParameters.put(paramName, paramValue);
            }
        }
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                "FilterConfiguration.DeployedInstance", new String[] {
                        this.filterName, this.classFile });
    }

    public String getFilterName() {
        return this.filterName;
    }

    public String getInitParameter(String paramName) {
        return (String) this.initParameters.get(paramName);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }

    public ServletContext getServletContext() {
        return this.context;
    }

    /**
     * Implements the first-time-init of an instance, and wraps it in a
     * dispatcher.
     */
    public Filter getFilter() throws ServletException {
        synchronized (this.filterSemaphore) {
            if (isUnavailable())
                throw new WinstoneException(Launcher.RESOURCES
                        .getString("FilterConfiguration.FilterUnavailable"));
            else if (this.instance == null)
                try {
                    ClassLoader cl = Thread.currentThread()
                            .getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(this.loader);

                    Class filterClass = Class.forName(classFile, true,
                            this.loader);
                    this.instance = (Filter) filterClass.newInstance();
                    Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                            "FilterConfiguration.init", this.filterName);

                    // Initialise with the correct classloader
                    this.instance.init(this);
                    Thread.currentThread().setContextClassLoader(cl);
                } catch (ClassNotFoundException err) {
                    Logger.log(Logger.ERROR, Launcher.RESOURCES,
                            "FilterConfiguration.ClassLoadError",
                            this.classFile, err);
                } catch (IllegalAccessException err) {
                    Logger.log(Logger.ERROR, Launcher.RESOURCES,
                            "FilterConfiguration.ClassLoadError",
                            this.classFile, err);
                } catch (InstantiationException err) {
                    Logger.log(Logger.ERROR, Launcher.RESOURCES,
                            "FilterConfiguration.ClassLoadError",
                            this.classFile, err);
                } catch (ServletException err) {
                    this.instance = null;
                    if (err instanceof UnavailableException)
                        setUnavailable();
                    throw err;
//                    throw new WinstoneException(resources
//                            .getString("FilterConfiguration.InitError"), err);
                }
        }
        return this.instance;
    }

    /**
     * Called when it's time for the container to shut this servlet down.
     */
    public void destroy() {
        synchronized (this.filterSemaphore) {
            setUnavailable();
        }
    }

    public String toString() {
        return Launcher.RESOURCES.getString("FilterConfiguration.Description",
                new String[] { this.filterName, this.classFile });
    }

    public boolean isUnavailable() {
        return this.unavailableException;
    }

    protected void setUnavailable() {
        this.unavailableException = true;
        if (this.instance != null) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                    "FilterConfiguration.destroy", this.filterName);
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.loader);
            this.instance.destroy();
            Thread.currentThread().setContextClassLoader(cl);
            this.instance = null;
        }
    }
    
    public void execute(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws ServletException, IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.loader);
        try {
            getFilter().doFilter(request, response, chain);
        } catch (UnavailableException err) {
            setUnavailable();
            throw new ServletException(Launcher.RESOURCES
                    .getString("RequestDispatcher.FilterError"), err);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
