/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Models the web.xml file's details ... basically just a bunch of configuration
 * details, plus the actual instances of mounted servlets.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WebAppConfiguration.java,v 1.55 2007/11/13 01:42:47 rickknowles Exp $
 */
public class WebAppConfiguration implements ServletContext, Comparator {
//    private static final String ELEM_DESCRIPTION = "description";
    private static final String ELEM_DISPLAY_NAME = "display-name";
    private static final String ELEM_SERVLET = "servlet";
    private static final String ELEM_SERVLET_MAPPING = "servlet-mapping";
    private static final String ELEM_SERVLET_NAME = "servlet-name";
    private static final String ELEM_FILTER = "filter";
    private static final String ELEM_FILTER_MAPPING = "filter-mapping";
    private static final String ELEM_FILTER_NAME = "filter-name";
    private static final String ELEM_DISPATCHER = "dispatcher";
    private static final String ELEM_URL_PATTERN = "url-pattern";
    private static final String ELEM_WELCOME_FILES = "welcome-file-list";
    private static final String ELEM_WELCOME_FILE = "welcome-file";
    private static final String ELEM_SESSION_CONFIG = "session-config";
    private static final String ELEM_SESSION_TIMEOUT = "session-timeout";
    private static final String ELEM_MIME_MAPPING = "mime-mapping";
    private static final String ELEM_MIME_EXTENSION = "extension";
    private static final String ELEM_MIME_TYPE = "mime-type";
    private static final String ELEM_CONTEXT_PARAM = "context-param";
    private static final String ELEM_PARAM_NAME = "param-name";
    private static final String ELEM_PARAM_VALUE = "param-value";
    private static final String ELEM_LISTENER = "listener";
    private static final String ELEM_LISTENER_CLASS = "listener-class";
    private static final String ELEM_DISTRIBUTABLE = "distributable";
    private static final String ELEM_ERROR_PAGE = "error-page";
    private static final String ELEM_EXCEPTION_TYPE = "exception-type";
    private static final String ELEM_ERROR_CODE = "error-code";
    private static final String ELEM_ERROR_LOCATION = "location";
    private static final String ELEM_SECURITY_CONSTRAINT = "security-constraint";
    private static final String ELEM_LOGIN_CONFIG = "login-config";
    private static final String ELEM_SECURITY_ROLE = "security-role";
    private static final String ELEM_ROLE_NAME = "role-name";
    private static final String ELEM_ENV_ENTRY = "env-entry";
    private static final String ELEM_LOCALE_ENC_MAP_LIST = "locale-encoding-mapping-list";
    private static final String ELEM_LOCALE_ENC_MAPPING = "locale-encoding-mapping";
    private static final String ELEM_LOCALE = "locale";
    private static final String ELEM_ENCODING = "encoding";
    private static final String ELEM_JSP_CONFIG = "jsp-config";
    private static final String ELEM_JSP_PROPERTY_GROUP = "jsp-property-group";
    
    private static final String DISPATCHER_REQUEST = "REQUEST";
    private static final String DISPATCHER_FORWARD = "FORWARD";
    private static final String DISPATCHER_INCLUDE = "INCLUDE";
    private static final String DISPATCHER_ERROR = "ERROR";
    private static final String JSP_SERVLET_NAME = "JspServlet";
    private static final String JSP_SERVLET_MAPPING = "*.jsp";
    private static final String JSPX_SERVLET_MAPPING = "*.jspx";
    private static final String JSP_SERVLET_LOG_LEVEL = "WARNING";
    private static final String INVOKER_SERVLET_NAME = "invoker";
    private static final String INVOKER_SERVLET_CLASS = "winstone.invoker.InvokerServlet";
    private static final String DEFAULT_INVOKER_PREFIX = "/servlet/";
    private static final String DEFAULT_SERVLET_NAME = "default";
    private static final String DEFAULT_SERVLET_CLASS = "winstone.StaticResourceServlet";
    private static final String DEFAULT_REALM_CLASS = "winstone.realm.ArgumentsRealm";
    private static final String DEFAULT_JNDI_MGR_CLASS = "winstone.jndi.WebAppJNDIManager";
    private static final String RELOADING_CL_CLASS = "winstone.classLoader.ReloadingClassLoader";
    private static final String WEBAPP_CL_CLASS = "winstone.classLoader.WebappClassLoader";    
    private static final String ERROR_SERVLET_NAME = "winstoneErrorServlet";
    private static final String ERROR_SERVLET_CLASS = "winstone.ErrorServlet";
    
    private static final String WEB_INF = "WEB-INF";
    private static final String CLASSES = "classes/";
    private static final String LIB = "lib";
    
    static final String JSP_SERVLET_CLASS = "org.apache.jasper.servlet.JspServlet";
    
    private HostConfiguration ownerHostConfig;
    private Cluster cluster;
    private String webRoot;
    private String prefix;
    private String contextName;
    private ClassLoader loader;
    private String displayName;
    private Map attributes;
    private Map initParameters;
    private Map sessions;
    private Map mimeTypes;
    private Map servletInstances;
    private Map filterInstances;
    private ServletContextAttributeListener contextAttributeListeners[];
    private ServletContextListener contextListeners[];
    private ServletRequestListener requestListeners[];
    private ServletRequestAttributeListener requestAttributeListeners[];
    private HttpSessionActivationListener sessionActivationListeners[];
    private HttpSessionAttributeListener sessionAttributeListeners[];
    private HttpSessionListener sessionListeners[];
    private Throwable contextStartupError;
    private Map exactServletMatchMounts;
    private Mapping patternMatches[];
    private Mapping filterPatternsRequest[];
    private Mapping filterPatternsForward[];
    private Mapping filterPatternsInclude[];
    private Mapping filterPatternsError[];
    private AuthenticationHandler authenticationHandler;
    private AuthenticationRealm authenticationRealm;
    private String welcomeFiles[];
    private Integer sessionTimeout;
    private Class[] errorPagesByExceptionKeysSorted;
    private Map errorPagesByException;
    private Map errorPagesByCode;
    private Map localeEncodingMap;
    private String defaultServletName;
    private String errorServletName;
    private JNDIManager jndiManager;
    private AccessLogger accessLogger;
    private Map filterMatchCache;
    private boolean useSavedSessions;
    
    public static boolean booleanArg(Map args, String name, boolean defaultTrue) {
        String value = (String) args.get(name);
        if (defaultTrue)
            return (value == null) || (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        else
            return (value != null) && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
    }

    public static String stringArg(Map args, String name, String defaultValue) {
        return (String) (args.get(name) == null ? defaultValue : args.get(name));
    }

    public static int intArg(Map args, String name, int defaultValue) {
        return Integer.parseInt(stringArg(args, name, "" + defaultValue));
    }

    public static String getTextFromNode(Node node) {
        if (node == null) {
            return null;
        }
        Node child = node.getFirstChild();
        if (child == null) {
            return "";
        }
        String textNode = child.getNodeValue();
        if (textNode == null) {
            return "";
        } else {
            return textNode.trim();
        }
    }
    
    public static boolean useSavedSessions(Map args) {
        return booleanArg(args, "useSavedSessions", false);
    }
    
    /**
     * Constructor. This parses the xml and sets up for basic routing
     */
    public WebAppConfiguration(HostConfiguration ownerHostConfig, Cluster cluster, String webRoot,
            String prefix, ObjectPool objectPool, Map startupArgs, Node elm,
            ClassLoader parentClassLoader, File parentClassPaths[], String contextName) {
        if (!prefix.equals("") && !prefix.startsWith("/")) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES, 
                    "WebAppConfig.AddingLeadingSlash", prefix);
            prefix = "/" + prefix;
        }
        this.ownerHostConfig = ownerHostConfig;
        this.webRoot = webRoot;
        this.prefix = prefix;
        this.contextName = contextName;

        List localLoaderClassPathFiles = new ArrayList();
        this.loader = buildWebAppClassLoader(startupArgs, parentClassLoader, 
                webRoot, localLoaderClassPathFiles);
        
        // Build switch values
        boolean useJasper = booleanArg(startupArgs, "useJasper", true);
        boolean useInvoker = booleanArg(startupArgs, "useInvoker", false);
        boolean useJNDI = booleanArg(startupArgs, "useJNDI", false);
        this.useSavedSessions = useSavedSessions(startupArgs);
        
        // Check jasper is available - simple tests
        if (useJasper) {
            try {
                Class.forName("javax.servlet.jsp.JspFactory", true, parentClassLoader);
                Class.forName(JSP_SERVLET_CLASS, true, this.loader);
            } catch (Throwable err) {
                if (booleanArg(startupArgs, "useJasper", false)) {
                    Logger.log(Logger.WARNING, Launcher.RESOURCES, 
                            "WebAppConfig.JasperNotFound");
                    Logger.log(Logger.DEBUG, Launcher.RESOURCES, 
                            "WebAppConfig.JasperLoadException", err);
                }
                useJasper = false;
            }
        }
        if (useInvoker) {
            try {
                Class.forName(INVOKER_SERVLET_CLASS, false, this.loader);
            } catch (Throwable err) {
                Logger.log(Logger.WARNING, Launcher.RESOURCES, 
                        "WebAppConfig.InvokerNotFound");
                useInvoker = false;
            }
        }

        this.attributes = new Hashtable();
        this.initParameters = new HashMap();
        this.sessions = new Hashtable();

        this.servletInstances = new HashMap();
        this.filterInstances = new HashMap();
        this.filterMatchCache = new HashMap();

        List contextAttributeListeners = new ArrayList();
        List contextListeners = new ArrayList();
        List requestListeners = new ArrayList();
        List requestAttributeListeners = new ArrayList();
        List sessionActivationListeners = new ArrayList();
        List sessionAttributeListeners = new ArrayList();
        List sessionListeners = new ArrayList();

        this.errorPagesByException = new HashMap();
        this.errorPagesByCode = new HashMap();
        boolean distributable = false;

        this.exactServletMatchMounts = new Hashtable();
        List localFolderPatterns = new ArrayList();
        List localExtensionPatterns = new ArrayList();

        List lfpRequest = new ArrayList();
        List lfpForward = new ArrayList();
        List lfpInclude = new ArrayList();
        List lfpError = new ArrayList();

        List localWelcomeFiles = new ArrayList();
        List startupServlets = new ArrayList();

        Set rolesAllowed = new HashSet();
        List constraintNodes = new ArrayList();
        List envEntryNodes = new ArrayList();
        List localErrorPagesByExceptionList = new ArrayList();

        Node loginConfigNode = null;

        // Add the class loader as an implicit context listener if it implements the interface
        addListenerInstance(this.loader, contextAttributeListeners,
                contextListeners, requestAttributeListeners, requestListeners,
                sessionActivationListeners, sessionAttributeListeners, 
                sessionListeners);
         
        // init mimeTypes set
        this.mimeTypes = new Hashtable();
        String allTypes = Launcher.RESOURCES.getString("WebAppConfig.DefaultMimeTypes");
        StringTokenizer mappingST = new StringTokenizer(allTypes, ":", false);
        for (; mappingST.hasMoreTokens();) {
            String mapping = mappingST.nextToken();
            int delimPos = mapping.indexOf('=');
            if (delimPos == -1)
                continue;
            String extension = mapping.substring(0, delimPos);
            String mimeType = mapping.substring(delimPos + 1);
            this.mimeTypes.put(extension.toLowerCase(), mimeType);
        }

        this.localeEncodingMap = new HashMap();
        String encodingMapSet = Launcher.RESOURCES.getString("WebAppConfig.EncodingMap");
        StringTokenizer st = new StringTokenizer(encodingMapSet, ";");
        for (; st.hasMoreTokens();) {
            String token = st.nextToken();
            int delimPos = token.indexOf("=");
            if (delimPos == -1)
                continue;
            this.localeEncodingMap.put(token.substring(0, delimPos), token
                    .substring(delimPos + 1));
        }
        
        // init jsp mappings set
        List jspMappings = new ArrayList();
        jspMappings.add(JSP_SERVLET_MAPPING);
        jspMappings.add(JSPX_SERVLET_MAPPING);

        // Add required context atttributes
        File tmpDir = new File(new File(new File(System.getProperty("java.io.tmpdir"), 
                "winstone.tmp"), ownerHostConfig.getHostname()), contextName); 
        tmpDir.mkdirs();
        this.attributes.put("javax.servlet.context.tempdir", tmpDir);

        // Parse the web.xml file
        if (elm != null) {
            NodeList children = elm.getChildNodes();
            for (int n = 0; n < children.getLength(); n++) {
                Node child = children.item(n);
                if (child.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                String nodeName = child.getNodeName();

                if (nodeName.equals(ELEM_DISPLAY_NAME))
                    this.displayName = getTextFromNode(child);

                else if (nodeName.equals(ELEM_DISTRIBUTABLE))
                    distributable = true;

                else if (nodeName.equals(ELEM_SECURITY_CONSTRAINT))
                    constraintNodes.add(child);

                else if (nodeName.equals(ELEM_ENV_ENTRY))
                    envEntryNodes.add(child);

                else if (nodeName.equals(ELEM_LOGIN_CONFIG))
                    loginConfigNode = child;

                // Session config elements
                else if (nodeName.equals(ELEM_SESSION_CONFIG)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node timeoutElm = child.getChildNodes().item(m);
                        if ((timeoutElm.getNodeType() == Node.ELEMENT_NODE)
                                && (timeoutElm.getNodeName().equals(ELEM_SESSION_TIMEOUT))) {
                            String timeoutStr = getTextFromNode(timeoutElm);
                            if (!timeoutStr.equals("")) {
                                this.sessionTimeout = Integer.valueOf(timeoutStr);
                            }
                        }
                    }
                }

                // Construct the security roles
                else if (child.getNodeName().equals(ELEM_SECURITY_ROLE)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node roleElm = child.getChildNodes().item(m);
                        if ((roleElm.getNodeType() == Node.ELEMENT_NODE)
                                && (roleElm.getNodeName()
                                        .equals(ELEM_ROLE_NAME)))
                            rolesAllowed.add(getTextFromNode(roleElm));
                    }
                }

                // Construct the servlet instances
                else if (nodeName.equals(ELEM_SERVLET)) {
                    ServletConfiguration instance = new ServletConfiguration(
                            this, child);
                    this.servletInstances.put(instance.getServletName(),
                            instance);
                    if (instance.getLoadOnStartup() >= 0)
                        startupServlets.add(instance);
                }

                // Construct the servlet instances
                else if (nodeName.equals(ELEM_FILTER)) {
                    FilterConfiguration instance = new FilterConfiguration(
                            this, this.loader, child);
                    this.filterInstances.put(instance.getFilterName(), instance);
                }

                // Construct the servlet instances
                else if (nodeName.equals(ELEM_LISTENER)) {
                    String listenerClass = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node listenerElm = child.getChildNodes().item(m);
                        if ((listenerElm.getNodeType() == Node.ELEMENT_NODE)
                                && (listenerElm.getNodeName()
                                        .equals(ELEM_LISTENER_CLASS)))
                            listenerClass = getTextFromNode(listenerElm);
                    }
                    if (listenerClass != null)
                        try {
                            Class listener = Class.forName(listenerClass, true,
                                    this.loader);
                            Object listenerInstance = listener.newInstance();
                            addListenerInstance(listenerInstance, contextAttributeListeners,
                                    contextListeners, requestAttributeListeners, requestListeners,
                                    sessionActivationListeners, sessionAttributeListeners, 
                                    sessionListeners);
                            Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                                    "WebAppConfig.AddListener", listenerClass);
                        } catch (Throwable err) {
                            Logger.log(Logger.WARNING, Launcher.RESOURCES,
                                    "WebAppConfig.InvalidListener",
                                    listenerClass);
                        }
                }

                // Process the servlet mappings
                else if (nodeName.equals(ELEM_SERVLET_MAPPING)) {
                    String name = null;
                    List mappings = new ArrayList();

                    // Parse the element and extract
                    NodeList mappingChildren = child.getChildNodes(); 
                    for (int k = 0; k < mappingChildren.getLength(); k++) {
                        Node mapChild = mappingChildren.item(k);
                        if (mapChild.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        String mapNodeName = mapChild.getNodeName();
                        if (mapNodeName.equals(ELEM_SERVLET_NAME)) {
                            name = getTextFromNode(mapChild);
                        } else if (mapNodeName.equals(ELEM_URL_PATTERN)) {
                            mappings.add(getTextFromNode(mapChild));
                        }
                    }
                    for (Iterator i = mappings.iterator(); i.hasNext(); ) {
                        processMapping(name, (String) i.next(), this.exactServletMatchMounts, 
                                localFolderPatterns, localExtensionPatterns);
                    }
                }

                // Process the filter mappings
                else if (nodeName.equals(ELEM_FILTER_MAPPING)) {
                    String filterName = null;
                    List mappings = new ArrayList();
                    boolean onRequest = false;
                    boolean onForward = false;
                    boolean onInclude = false;
                    boolean onError = false;

                    // Parse the element and extract
                    for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                        Node mapChild = child.getChildNodes().item(k);
                        if (mapChild.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        String mapNodeName = mapChild.getNodeName();
                        if (mapNodeName.equals(ELEM_FILTER_NAME)) {
                            filterName = getTextFromNode(mapChild);
                        } else if (mapNodeName.equals(ELEM_SERVLET_NAME)) {
                            mappings.add("srv:" + getTextFromNode(mapChild));
                        } else if (mapNodeName.equals(ELEM_URL_PATTERN)) {
                            mappings.add("url:" + getTextFromNode(mapChild));
                        } else if (mapNodeName.equals(ELEM_DISPATCHER)) {
                            String dispatcherValue = getTextFromNode(mapChild);
                            if (dispatcherValue.equals(DISPATCHER_REQUEST))
                                onRequest = true;
                            else if (dispatcherValue.equals(DISPATCHER_FORWARD))
                                onForward = true;
                            else if (dispatcherValue.equals(DISPATCHER_INCLUDE))
                                onInclude = true;
                            else if (dispatcherValue.equals(DISPATCHER_ERROR))
                                onError = true;
                        }
                    }
                    if (!onRequest && !onInclude && !onForward && !onError) {
                        onRequest = true;
                    }
                    if (mappings.isEmpty()) {
                        throw new WinstoneException(Launcher.RESOURCES.getString(
                                "WebAppConfig.BadFilterMapping", filterName));
                    }
                        
                    for (Iterator i = mappings.iterator(); i.hasNext(); ) {
                        String item = (String) i.next();
                        Mapping mapping = null;
                        try {
                            if (item.startsWith("srv:")) {
                                mapping = Mapping.createFromLink(filterName, item.substring(4));
                            } else {
                                mapping = Mapping.createFromURL(filterName, item.substring(4));
                            }
                            if (onRequest)
                                lfpRequest.add(mapping);
                            if (onForward)
                                lfpForward.add(mapping);
                            if (onInclude)
                                lfpInclude.add(mapping);
                            if (onError)
                                lfpError.add(mapping);
                        } catch (WinstoneException err) {
                            Logger.log(Logger.WARNING, Launcher.RESOURCES, "WebAppConfig.ErrorMapURL",
                                    err.getMessage());
                        }
                    }
                }

                // Process the list of welcome files
                else if (nodeName.equals(ELEM_WELCOME_FILES)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node welcomeFile = child.getChildNodes().item(m);
                        if ((welcomeFile.getNodeType() == Node.ELEMENT_NODE)
                                && welcomeFile.getNodeName().equals(ELEM_WELCOME_FILE)) {
                            String welcomeStr = getTextFromNode(welcomeFile);
                            if (!welcomeStr.equals("")) {
                                localWelcomeFiles.add(welcomeStr);
                            }
                        }
                    }
                }
                
                // Process the error pages
                else if (nodeName.equals(ELEM_ERROR_PAGE)) {
                    String code = null;
                    String exception = null;
                    String location = null;

                    // Parse the element and extract
                    for (int k = 0; k < child.getChildNodes().getLength(); k++) {
                        Node errorChild = child.getChildNodes().item(k);
                        if (errorChild.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        String errorChildName = errorChild.getNodeName();
                        if (errorChildName.equals(ELEM_ERROR_CODE))
                            code = getTextFromNode(errorChild);
                        else if (errorChildName.equals(ELEM_EXCEPTION_TYPE))
                            exception = getTextFromNode(errorChild);
                        else if (errorChildName.equals(ELEM_ERROR_LOCATION))
                            location = getTextFromNode(errorChild);
                    }
                    if ((code != null) && (location != null))
                        this.errorPagesByCode.put(code.trim(), location.trim());
                    if ((exception != null) && (location != null))
                        try {
                            Class exceptionClass = Class.forName(exception
                                    .trim(), false, this.loader);
                            localErrorPagesByExceptionList.add(exceptionClass);
                            this.errorPagesByException.put(exceptionClass,
                                    location.trim());
                        } catch (ClassNotFoundException err) {
                            Logger.log(Logger.ERROR, Launcher.RESOURCES,
                                            "WebAppConfig.ExceptionNotFound",
                                            exception);
                        }
                }

                // Process the list of welcome files
                else if (nodeName.equals(ELEM_MIME_MAPPING)) {
                    String extension = null;
                    String mimeType = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node mimeTypeNode = child.getChildNodes().item(m);
                        if (mimeTypeNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        else if (mimeTypeNode.getNodeName().equals(
                                ELEM_MIME_EXTENSION))
                            extension = getTextFromNode(mimeTypeNode);
                        else if (mimeTypeNode.getNodeName().equals(
                                ELEM_MIME_TYPE))
                            mimeType = getTextFromNode(mimeTypeNode);
                    }
                    if ((extension != null) && (mimeType != null))
                        this.mimeTypes.put(extension.toLowerCase(), mimeType);
                    else
                        Logger.log(Logger.WARNING, Launcher.RESOURCES,
                                "WebAppConfig.InvalidMimeMapping",
                                new String[] { extension, mimeType });
                }

                // Process the list of welcome files
                else if (nodeName.equals(ELEM_CONTEXT_PARAM)) {
                    String name = null;
                    String value = null;
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node contextParamNode = child.getChildNodes().item(m);
                        if (contextParamNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        else if (contextParamNode.getNodeName().equals(
                                ELEM_PARAM_NAME))
                            name = getTextFromNode(contextParamNode);
                        else if (contextParamNode.getNodeName().equals(
                                ELEM_PARAM_VALUE))
                            value = getTextFromNode(contextParamNode);
                    }
                    if ((name != null) && (value != null))
                        this.initParameters.put(name, value);
                    else
                        Logger.log(Logger.WARNING, Launcher.RESOURCES,
                                "WebAppConfig.InvalidInitParam", new String[] {
                                        name, value });
                }

                // Process locale encoding mapping elements
                else if (nodeName.equals(ELEM_LOCALE_ENC_MAP_LIST)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node mappingNode = child.getChildNodes().item(m);
                        if (mappingNode.getNodeType() != Node.ELEMENT_NODE)
                            continue;
                        else if (mappingNode.getNodeName().equals(ELEM_LOCALE_ENC_MAPPING)) {
                            String localeName = "";
                            String encoding = "";
                            for (int l = 0; l < mappingNode.getChildNodes().getLength(); l++) {
                                Node mappingChildNode = mappingNode.getChildNodes().item(l);
                                if (mappingChildNode.getNodeType() != Node.ELEMENT_NODE)
                                    continue;
                                else if (mappingChildNode.getNodeName().equals(ELEM_LOCALE))
                                    localeName = getTextFromNode(mappingChildNode);
                                else if (mappingChildNode.getNodeName().equals(ELEM_ENCODING))
                                    encoding = getTextFromNode(mappingChildNode);
                            }
                            if (!encoding.equals("") && !localeName.equals(""))
                                this.localeEncodingMap.put(localeName, encoding);
                        }
                    }
                }

                // Record the url mappings for jsp files if set
                else if (nodeName.equals(ELEM_JSP_CONFIG)) {
                    for (int m = 0; m < child.getChildNodes().getLength(); m++) {
                        Node propertyGroupNode = child.getChildNodes().item(m);
                        if ((propertyGroupNode.getNodeType() == Node.ELEMENT_NODE)
                                && propertyGroupNode.getNodeName().equals(ELEM_JSP_PROPERTY_GROUP)) {
                            for (int l = 0; l < propertyGroupNode.getChildNodes().getLength(); l++) {
                                Node urlPatternNode = propertyGroupNode.getChildNodes().item(l);
                                if ((urlPatternNode.getNodeType() == Node.ELEMENT_NODE)
                                        && urlPatternNode.getNodeName().equals(ELEM_URL_PATTERN)) {
                                    String jm = getTextFromNode(urlPatternNode);
                                    if (!jm.equals("")) {
                                        jspMappings.add(jm);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // If not distributable, remove the cluster reference
        if (!distributable && (cluster != null)) {
            Logger.log(Logger.INFO, Launcher.RESOURCES,
                    "WebAppConfig.ClusterOffNotDistributable", this.contextName);
        } else {
            this.cluster = cluster;
        }


        // Build the login/security role instance
        if (!constraintNodes.isEmpty() && (loginConfigNode != null)) {
            String authMethod = null;
            for (int n = 0; n < loginConfigNode.getChildNodes().getLength(); n++) {
                if (loginConfigNode.getChildNodes().item(n).getNodeName().equals("auth-method")) {
                    authMethod = getTextFromNode(loginConfigNode.getChildNodes().item(n));
                }
            }
            // Load the appropriate auth class
            if (authMethod == null) {
                authMethod = "BASIC";
            } else {
                authMethod = WinstoneResourceBundle.globalReplace(authMethod, "-", "");
            }
            String realmClassName = stringArg(startupArgs, "realmClassName",
                    DEFAULT_REALM_CLASS).trim();
            String authClassName = "winstone.auth."
                    + authMethod.substring(0, 1).toUpperCase()
                    + authMethod.substring(1).toLowerCase()
                    + "AuthenticationHandler";
            try {
                // Build the realm
                Class realmClass = Class.forName(realmClassName, true, parentClassLoader);
                Constructor realmConstr = realmClass.getConstructor(
                        new Class[] {Set.class, Map.class });
                this.authenticationRealm = (AuthenticationRealm) realmConstr.newInstance(
                        new Object[] { rolesAllowed, startupArgs });

                // Build the authentication handler
                Class authClass = Class.forName(authClassName);
                Constructor authConstr = authClass
                        .getConstructor(new Class[] { Node.class, List.class,
                                Set.class, AuthenticationRealm.class });
                this.authenticationHandler = (AuthenticationHandler) authConstr
                        .newInstance(new Object[] { loginConfigNode,
                                constraintNodes, rolesAllowed,
                                authenticationRealm });
            } catch (ClassNotFoundException err) {
                Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                        "WebAppConfig.AuthDisabled", authMethod);
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES,
                        "WebAppConfig.AuthError", new String[] { authClassName,
                                realmClassName }, err);
            }
        } else if (!stringArg(startupArgs, "realmClassName", "").trim().equals("")) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WebAppConfig.NoWebXMLSecurityDefs");
        }

        // Instantiate the JNDI manager
        String jndiMgrClassName = stringArg(startupArgs, "webappJndiClassName",
                DEFAULT_JNDI_MGR_CLASS).trim();
        if (useJNDI) {
            try {
                // Build the realm
                Class jndiMgrClass = Class.forName(jndiMgrClassName, true, parentClassLoader);
                Constructor jndiMgrConstr = jndiMgrClass.getConstructor(new Class[] { 
                        Map.class, List.class, ClassLoader.class });
                this.jndiManager = (JNDIManager) jndiMgrConstr.newInstance(new Object[] { 
                        null, envEntryNodes, this.loader });
                if (this.jndiManager != null)
                    this.jndiManager.setup();
            } catch (ClassNotFoundException err) {
                Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                        "WebAppConfig.JNDIDisabled");
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES,
                        "WebAppConfig.JNDIError", jndiMgrClassName, err);
            }
        }
        
        String loggerClassName = stringArg(startupArgs, "accessLoggerClassName", "").trim();
        if (!loggerClassName.equals("")) {
            try {
                // Build the realm
                Class loggerClass = Class.forName(loggerClassName, true, parentClassLoader);
                Constructor loggerConstr = loggerClass.getConstructor(new Class[] { 
                        WebAppConfiguration.class, Map.class });
                this.accessLogger = (AccessLogger) loggerConstr.newInstance(new Object[] {
                        this, startupArgs});
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES,
                        "WebAppConfig.LoggerError", loggerClassName, err);
            }
        } else {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WebAppConfig.LoggerDisabled");
            
        }
        
        // Add the default index.html welcomeFile if none are supplied
        if (localWelcomeFiles.isEmpty()) {
            if (useJasper) {
                localWelcomeFiles.add("index.jsp");
            }
            localWelcomeFiles.add("index.html");
        }

        // Put the name filters after the url filters, then convert to string arrays
        this.filterPatternsRequest = (Mapping[]) lfpRequest.toArray(new Mapping[0]);
        this.filterPatternsForward = (Mapping[]) lfpForward.toArray(new Mapping[0]);
        this.filterPatternsInclude = (Mapping[]) lfpInclude.toArray(new Mapping[0]);
        this.filterPatternsError = (Mapping[]) lfpError.toArray(new Mapping[0]);

        if (this.filterPatternsRequest.length > 0)
            Arrays.sort(this.filterPatternsRequest, this.filterPatternsRequest[0]);
        if (this.filterPatternsForward.length > 0)
            Arrays.sort(this.filterPatternsForward, this.filterPatternsForward[0]);
        if (this.filterPatternsInclude.length > 0)
            Arrays.sort(this.filterPatternsInclude, this.filterPatternsInclude[0]);
        if (this.filterPatternsError.length > 0)
            Arrays.sort(this.filterPatternsError, this.filterPatternsError[0]);

        this.welcomeFiles = (String[]) localWelcomeFiles.toArray(new String[0]);
        this.errorPagesByExceptionKeysSorted = (Class[]) localErrorPagesByExceptionList
                .toArray(new Class[0]);
        Arrays.sort(this.errorPagesByExceptionKeysSorted, this);

        // Put the listeners into their arrays
        this.contextAttributeListeners = (ServletContextAttributeListener[]) contextAttributeListeners
                .toArray(new ServletContextAttributeListener[0]);
        this.contextListeners = (ServletContextListener[]) contextListeners
                .toArray(new ServletContextListener[0]);
        this.requestListeners = (ServletRequestListener[]) requestListeners
                .toArray(new ServletRequestListener[0]);
        this.requestAttributeListeners = (ServletRequestAttributeListener[]) requestAttributeListeners
                .toArray(new ServletRequestAttributeListener[0]);
        this.sessionActivationListeners = (HttpSessionActivationListener[]) sessionActivationListeners
                .toArray(new HttpSessionActivationListener[0]);
        this.sessionAttributeListeners = (HttpSessionAttributeListener[]) sessionAttributeListeners
                .toArray(new HttpSessionAttributeListener[0]);
        this.sessionListeners = (HttpSessionListener[]) sessionListeners
                .toArray(new HttpSessionListener[0]);

        // If we haven't explicitly mapped the default servlet, map it here
        if (this.defaultServletName == null)
            this.defaultServletName = DEFAULT_SERVLET_NAME;
        if (this.errorServletName == null)
            this.errorServletName = ERROR_SERVLET_NAME;

        // If we don't have an instance of the default servlet, mount the inbuilt one
        if (this.servletInstances.get(this.defaultServletName) == null) {
            boolean useDirLists = booleanArg(startupArgs, "directoryListings", true);
            
            Map staticParams = new Hashtable();
            staticParams.put("webRoot", webRoot);
            staticParams.put("prefix", this.prefix);
            staticParams.put("directoryList", "" + useDirLists);
            ServletConfiguration defaultServlet = new ServletConfiguration(
                    this,  this.defaultServletName, DEFAULT_SERVLET_CLASS,
                    staticParams, 0);
            this.servletInstances.put(this.defaultServletName, defaultServlet);
            startupServlets.add(defaultServlet);
        }

        // If we don't have an instance of the default servlet, mount the inbuilt one
        if (this.servletInstances.get(this.errorServletName) == null) {
            ServletConfiguration errorServlet = new ServletConfiguration(
                    this,  this.errorServletName, ERROR_SERVLET_CLASS,
                    new HashMap(), 0);
            this.servletInstances.put(this.errorServletName, errorServlet);
            startupServlets.add(errorServlet);
        }
        
        // Initialise jasper servlet if requested
        if (useJasper) {
            setAttribute("org.apache.catalina.classloader", this.loader);
            try {
                StringBuffer cp = new StringBuffer();
                for (Iterator i = localLoaderClassPathFiles.iterator(); i.hasNext(); ) {
                    cp.append(((File) i.next()).getCanonicalPath()).append(
                            File.pathSeparatorChar);
                }
                for (int n = 0; n < parentClassPaths.length; n++) {
                    cp.append(parentClassPaths[n].getCanonicalPath()).append(
                            File.pathSeparatorChar);
                }
                setAttribute("org.apache.catalina.jsp_classpath",
                        (cp.length() > 0 ? cp.substring(0, cp.length() - 1) : ""));
            } catch (IOException err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.ErrorSettingJSPPaths", err);
            }

            Map jspParams = new HashMap();
            addJspServletParams(jspParams);
            ServletConfiguration sc = new ServletConfiguration(this,
                    JSP_SERVLET_NAME, JSP_SERVLET_CLASS, jspParams, 3);
            this.servletInstances.put(JSP_SERVLET_NAME, sc);
            startupServlets.add(sc);
            for (Iterator mapIt = jspMappings.iterator(); mapIt.hasNext(); ) {
                processMapping(JSP_SERVLET_NAME, (String) mapIt.next(),
                        this.exactServletMatchMounts, localFolderPatterns,
                        localExtensionPatterns);
            }
        }

        // Initialise invoker servlet if requested
        if (useInvoker) {
            // Get generic options
            String invokerPrefix = stringArg(startupArgs, "invokerPrefix",
                    DEFAULT_INVOKER_PREFIX);
            Map invokerParams = new HashMap();
            invokerParams.put("prefix", this.prefix);
            invokerParams.put("invokerPrefix", invokerPrefix);
            ServletConfiguration sc = new ServletConfiguration(this,
                    INVOKER_SERVLET_NAME, INVOKER_SERVLET_CLASS, 
                    invokerParams, 3);
            this.servletInstances.put(INVOKER_SERVLET_NAME, sc);
            processMapping(INVOKER_SERVLET_NAME, invokerPrefix + Mapping.STAR,
                    this.exactServletMatchMounts, localFolderPatterns,
                    localExtensionPatterns);
        }

        // Sort the folder patterns so the longest paths are first
        localFolderPatterns.addAll(localExtensionPatterns);
        this.patternMatches = (Mapping[]) localFolderPatterns.toArray(new Mapping[0]);
        if (this.patternMatches.length > 0)
            Arrays.sort(this.patternMatches, this.patternMatches[0]);

        // Send init notifies
        try {
            for (int n = 0; n < this.contextListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.loader);
                this.contextListeners[n].contextInitialized(new ServletContextEvent(this));
                Thread.currentThread().setContextClassLoader(cl);
            }
        } catch (Throwable err) {
            Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.ContextStartupError", this.contextName, err);
            this.contextStartupError = err;
        }

        if (this.contextStartupError == null) {
            // Load sessions if enabled
            if (this.useSavedSessions) {
                WinstoneSession.loadSessions(this);
            }
            
            // Initialise all the filters
            for (Iterator i = this.filterInstances.values().iterator(); i.hasNext();) {
                FilterConfiguration config = (FilterConfiguration) i.next();
                try {
                    config.getFilter();
                } catch (ServletException err) {
                    Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.FilterStartupError", 
                            config.getFilterName(), err);
                }
            }

            // Initialise load on startup servlets
            Object autoStarters[] = startupServlets.toArray();
            Arrays.sort(autoStarters);
            for (int n = 0; n < autoStarters.length; n++) {
                ((ServletConfiguration) autoStarters[n]).ensureInitialization();
            }
        }
    }

    /**
     * Build the web-app classloader. This tries to load the preferred classloader first, 
     * but if it fails, falls back to a simple URLClassLoader.
     */
    private ClassLoader buildWebAppClassLoader(Map startupArgs, ClassLoader parentClassLoader,
            String webRoot, List classPathFileList) {
        List urlList = new ArrayList();
        
        try {
            // Web-inf folder
            File webInfFolder = new File(webRoot, WEB_INF);

            // Classes folder
            File classesFolder = new File(webInfFolder, CLASSES);
            if (classesFolder.exists()) {
                Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                        "WebAppConfig.WebAppClasses");
                String classesFolderURL = classesFolder.getCanonicalFile().toURL().toString(); 
                urlList.add(new URL(classesFolderURL.endsWith("/") ? classesFolderURL : classesFolderURL + "/"));
                classPathFileList.add(classesFolder);
            } else {
                Logger.log(Logger.WARNING, Launcher.RESOURCES,
                        "WebAppConfig.NoWebAppClasses", 
                        classesFolder.toString());
            }

            // Lib folder's jar files
            File libFolder = new File(webInfFolder, LIB);
            if (libFolder.exists()) {
                File jars[] = libFolder.listFiles();
                for (int n = 0; n < jars.length; n++) {
                    String jarName = jars[n].getName().toLowerCase();
                    if (jarName.endsWith(".jar") || jarName.endsWith(".zip")) {
                        Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                                "WebAppConfig.WebAppLib", jars[n].getName());
                        urlList.add(jars[n].toURL());
                        classPathFileList.add(jars[n]);
                    }
                }
            } else {
                Logger.log(Logger.WARNING, Launcher.RESOURCES,
                        "WebAppConfig.NoWebAppLib", libFolder
                                .toString());
            }
        } catch (MalformedURLException err) {
            throw new WinstoneException(Launcher.RESOURCES
                    .getString("WebAppConfig.BadURL"), err);
        } catch (IOException err) {
            throw new WinstoneException(Launcher.RESOURCES
                    .getString("WebAppConfig.IOException"), err);
        }

        URL jarURLs[] = (URL []) urlList.toArray(new URL[urlList.size()]);
        
        String preferredClassLoader = stringArg(startupArgs, "preferredClassLoader", WEBAPP_CL_CLASS);
        if (booleanArg(startupArgs, "useServletReloading", false) && 
                stringArg(startupArgs, "preferredClassLoader", "").equals("")) {
            preferredClassLoader = RELOADING_CL_CLASS;
        }
        
        // Try to set up the preferred class loader, and if we fail, use the normal one
        ClassLoader outputCL = null;
        if (!preferredClassLoader.equals("")) {
            try {
                Class preferredCL = Class.forName(preferredClassLoader, true, parentClassLoader);
                Constructor reloadConstr = preferredCL.getConstructor(new Class[] { 
                        URL[].class, ClassLoader.class});
                outputCL = (ClassLoader) reloadConstr.newInstance(new Object[] { 
                        jarURLs, parentClassLoader});
            } catch (Throwable err) {
                if (!stringArg(startupArgs, "preferredClassLoader", "").equals("") || 
                        !preferredClassLoader.equals(WEBAPP_CL_CLASS)) {
                    Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.CLError", err);
                }
            }
        }

        if (outputCL == null) {
            outputCL = new URLClassLoader(jarURLs, parentClassLoader);
        }

        Logger.log(Logger.MAX, Launcher.RESOURCES, "WebAppConfig.WebInfClassLoader", outputCL.toString());
        return outputCL;
    }
    
    private void addListenerInstance(Object listenerInstance, List contextAttributeListeners,
            List contextListeners, List requestAttributeListeners, List requestListeners,
            List sessionActivationListeners, List sessionAttributeListeners, 
            List sessionListeners) {
        if (listenerInstance instanceof ServletContextAttributeListener)
            contextAttributeListeners.add(listenerInstance);
        if (listenerInstance instanceof ServletContextListener)
            contextListeners.add(listenerInstance);
        if (listenerInstance instanceof ServletRequestAttributeListener)
            requestAttributeListeners.add(listenerInstance);
        if (listenerInstance instanceof ServletRequestListener)
            requestListeners.add(listenerInstance);
        if (listenerInstance instanceof HttpSessionActivationListener)
            sessionActivationListeners.add(listenerInstance);
        if (listenerInstance instanceof HttpSessionAttributeListener)
            sessionAttributeListeners.add(listenerInstance);
        if (listenerInstance instanceof HttpSessionListener)
            sessionListeners.add(listenerInstance);
    }
    
    public String getContextPath() {
        return this.prefix;
    }

    public String getWebroot() {
        return this.webRoot;
    }

    public ClassLoader getLoader() {
        return this.loader;
    }

    public AccessLogger getAccessLogger() {
        return this.accessLogger;
    }

    public Map getFilters() {
        return this.filterInstances;
    }
    
    public String getContextName() {
        return this.contextName;
    }

    public Class[] getErrorPageExceptions() {
        return this.errorPagesByExceptionKeysSorted;
    }

    public Map getErrorPagesByException() {
        return this.errorPagesByException;
    }

    public Map getErrorPagesByCode() {
        return this.errorPagesByCode;
    }

    public Map getLocaleEncodingMap() {
        return this.localeEncodingMap;
    }

    public String[] getWelcomeFiles() {
        return this.welcomeFiles;
    }

    public boolean isDistributable() {
        return (this.cluster != null);
    }

    public Map getFilterMatchCache() {
        return this.filterMatchCache;
    }
    
    public String getOwnerHostname() {
        return this.ownerHostConfig.getHostname();
    }
    
    public ServletRequestListener[] getRequestListeners() {
        return this.requestListeners;
    }

    public ServletRequestAttributeListener[] getRequestAttributeListeners() {
        return this.requestAttributeListeners;
    }

    public static void addJspServletParams(Map jspParams) {
        jspParams.put("logVerbosityLevel", JSP_SERVLET_LOG_LEVEL);
        jspParams.put("fork", "false");
    }

    public int compare(Object one, Object two) {
        if (!(one instanceof Class) || !(two instanceof Class))
            throw new IllegalArgumentException(
                    "This comparator is only for sorting classes");
        Class classOne = (Class) one;
        Class classTwo = (Class) two;
        if (classOne.isAssignableFrom(classTwo))
            return 1;
        else if (classTwo.isAssignableFrom(classOne))
            return -1;
        else
            return 0;
    }

    public String getServletURIFromRequestURI(String requestURI) {
        if (prefix.equals("")) {
            return requestURI;
        } else if (requestURI.startsWith(prefix)) {
            return requestURI.substring(prefix.length());
        } else {
            throw new WinstoneException("This shouldn't happen, " +
                    "since we aborted earlier if we didn't match");
        }
    }
    
    /**
     * Iterates through each of the servlets/filters and calls destroy on them
     */
    public void destroy() {
        synchronized (this.filterMatchCache) {
            this.filterMatchCache.clear();
        }
        
        Collection filterInstances = new ArrayList(this.filterInstances.values());
        for (Iterator i = filterInstances.iterator(); i.hasNext();) {
            try {
                ((FilterConfiguration) i.next()).destroy();
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.ShutdownError", err);
            }
        }
        this.filterInstances.clear();
        
        Collection servletInstances = new ArrayList(this.servletInstances.values());
        for (Iterator i = servletInstances.iterator(); i.hasNext();) {
            try {
                ((ServletConfiguration) i.next()).destroy();
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.ShutdownError", err);
            }
        }
        this.servletInstances.clear();

        // Drop all sessions
        Collection sessions = new ArrayList(this.sessions.values());
        for (Iterator i = sessions.iterator(); i.hasNext();) {
            WinstoneSession session = (WinstoneSession) i.next(); 
            try {
                if (this.useSavedSessions) {
                    session.saveToTemp();
                } else {
                    session.invalidate();
                }
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.ShutdownError", err);
            }
        }
        this.sessions.clear();

        // Send destroy notifies - backwards
        for (int n = this.contextListeners.length - 1; n >= 0; n--) {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.loader);
                this.contextListeners[n].contextDestroyed(new ServletContextEvent(this));
                this.contextListeners[n] = null;
                Thread.currentThread().setContextClassLoader(cl);
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.ShutdownError", err);
            }
        }
        this.contextListeners = null;
        
        // Terminate class loader reloading thread if running
        if (this.loader != null) {
            // already shutdown/handled by the servlet context listeners
//            try {
//                Method methDestroy = this.loader.getClass().getMethod("destroy", new Class[0]);
//                methDestroy.invoke(this.loader, new Object[0]);
//            } catch (Throwable err) {
//                Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.ShutdownError", err);
//            }
            this.loader = null;
        }

        // Kill JNDI manager if we have one
        if (this.jndiManager != null) {
            this.jndiManager.tearDown();
            this.jndiManager = null;
        }

        // Kill JNDI manager if we have one
        if (this.accessLogger != null) {
            this.accessLogger.destroy();
            this.accessLogger = null;
        }
    }

    /**
     * Triggered by the admin thread on the reloading class loader. This will
     * cause a full shutdown and reinstantiation of the web app - not real
     * graceful, but you shouldn't have reloading turned on in high load
     * environments.
     */
    public void resetClassLoader() throws IOException {
        this.ownerHostConfig.reloadWebApp(getContextPath());
    }

    /**
     * Here we process url patterns into the exactMatch and patternMatch lists
     */
    private void processMapping(String name, String pattern, Map exactPatterns,
            List folderPatterns, List extensionPatterns) {
        
        Mapping urlPattern = null;
        try {
            urlPattern = Mapping.createFromURL(name, pattern);
        } catch (WinstoneException err) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES, "WebAppConfig.ErrorMapURL",
                    err.getMessage());
            return;
        }

        // put the pattern in the correct list
        if (urlPattern.getPatternType() == Mapping.EXACT_PATTERN) {
            exactPatterns.put(urlPattern.getUrlPattern(), name);
        } else if (urlPattern.getPatternType() == Mapping.FOLDER_PATTERN) {
            folderPatterns.add(urlPattern);
        } else if (urlPattern.getPatternType() == Mapping.EXTENSION_PATTERN) {
            extensionPatterns.add(urlPattern);
        } else if (urlPattern.getPatternType() == Mapping.DEFAULT_SERVLET) {
            this.defaultServletName = name;
        } else {
            Logger.log(Logger.WARNING, Launcher.RESOURCES, "WebAppConfig.InvalidMount",
                    new String[] { name, pattern });
        }
    }

    /**
     * Execute the pattern match, and try to return a servlet that matches this
     * URL
     */
    private ServletConfiguration urlMatch(String path,
            StringBuffer servletPath, StringBuffer pathInfo) {
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WebAppConfig.URLMatch", path);

        // Check exact matches first
        String exact = (String) this.exactServletMatchMounts.get(path);
        if (exact != null) {
            if (this.servletInstances.get(exact) != null) {
                servletPath.append(WinstoneRequest.decodeURLToken(path));
                // pathInfo.append(""); // a hack - empty becomes null later
                return (ServletConfiguration) this.servletInstances.get(exact);
            }
        }

        // Inexact mount check
        for (int n = 0; n < this.patternMatches.length; n++) {
            Mapping urlPattern = this.patternMatches[n];
            if (urlPattern.match(path, servletPath, pathInfo) && 
                    (this.servletInstances.get(urlPattern.getMappedTo()) != null)) {
                return (ServletConfiguration) this.servletInstances
                        .get(urlPattern.getMappedTo());
            }
        }

        // return default servlet
        // servletPath.append(""); // unneeded
        if (this.servletInstances.get(this.defaultServletName) == null)
            throw new WinstoneException(Launcher.RESOURCES.getString(
                    "WebAppConfig.MatchedNonExistServlet",
                    this.defaultServletName));
//        pathInfo.append(path);
        servletPath.append(WinstoneRequest.decodeURLToken(path));
        return (ServletConfiguration) this.servletInstances.get(this.defaultServletName);
    }

    /**
     * Constructs a session instance with the given sessionId
     * 
     * @param sessionId The sessionID for the new session
     * @return A valid session object
     */
    public WinstoneSession makeNewSession(String sessionId) {
        WinstoneSession ws = new WinstoneSession(sessionId);
        ws.setWebAppConfiguration(this);
        setSessionListeners(ws);
        if ((this.sessionTimeout != null) && (this.sessionTimeout.intValue() > 0)) {
            ws.setMaxInactiveInterval(this.sessionTimeout.intValue() * 60);
        } else {
            ws.setMaxInactiveInterval(-1);
        }
        ws.setLastAccessedDate(System.currentTimeMillis());
        ws.sendCreatedNotifies();
        this.sessions.put(sessionId, ws);
        return ws;
    }

    /**
     * Retrieves the session by id. If the web app is distributable, it asks the
     * other members of the cluster if it doesn't have it itself.
     * 
     * @param sessionId The id of the session we want
     * @return A valid session instance
     */
    public WinstoneSession getSessionById(String sessionId, boolean localOnly) {
        if (sessionId == null) {
            return null;
        }
        WinstoneSession session = (WinstoneSession) this.sessions.get(sessionId);
        if (session != null) {
            return session;
        }

        // If I'm distributable ... check remotely
        if ((this.cluster != null) && !localOnly) {
            session = this.cluster.askClusterForSession(sessionId, this);
            if (session != null) {
                this.sessions.put(sessionId, session);
            }
            return session;
        } else {
            return null;
        }
    }

    /**
     * Add/Remove the session from the collection
     */
    void removeSessionById(String sessionId) {
        this.sessions.remove(sessionId);
    }
    void addSession(String sessionId, WinstoneSession session) {
        this.sessions.put(sessionId, session);
    }

    public void invalidateExpiredSessions() {
        Object allSessions[] = this.sessions.values().toArray();
        int expiredCount = 0;
        
        for (int n = 0; n < allSessions.length; n++) {
            WinstoneSession session = (WinstoneSession) allSessions[n];
            if (!session.isNew() && session.isUnusedByRequests() && session.isExpired()) {
                session.invalidate();
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                    "WebAppConfig.InvalidatedSessions", expiredCount + "");
        }
    }
    
    public void setSessionListeners(WinstoneSession session) {
        session.setSessionActivationListeners(this.sessionActivationListeners);
        session.setSessionAttributeListeners(this.sessionAttributeListeners);
        session.setSessionListeners(this.sessionListeners);
    }

    public void removeServletConfigurationAndMappings(ServletConfiguration config) {
        this.servletInstances.remove(config.getServletName());
        // The urlMatch method will only match to non-null mappings, so we don't need
        // to remove anything here
    }
    
    /***************************************************************************
     * 
     * OK ... from here to the end is the interface implementation methods for
     * the servletContext interface.
     * 
     **************************************************************************/

    // Application level attributes
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Enumeration getAttributeNames() {
        return Collections.enumeration(this.attributes.keySet());
    }

    public void removeAttribute(String name) {
        Object me = this.attributes.get(name);
        this.attributes.remove(name);
        if (me != null)
            for (int n = 0; n < this.contextAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getLoader());
                this.contextAttributeListeners[n].attributeRemoved(
                        new ServletContextAttributeEvent(this, name, me));
                Thread.currentThread().setContextClassLoader(cl);
            }
    }

    public void setAttribute(String name, Object object) {
        if (object == null) {
            removeAttribute(name);
        } else {
            Object me = this.attributes.get(name);
            this.attributes.put(name, object);
            if (me != null) {
                for (int n = 0; n < this.contextAttributeListeners.length; n++) {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(getLoader());
                    this.contextAttributeListeners[n].attributeReplaced(
                            new ServletContextAttributeEvent(this, name, me));
                    Thread.currentThread().setContextClassLoader(cl);
                }
            } else {
                for (int n = 0; n < this.contextAttributeListeners.length; n++) {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(getLoader());
                    this.contextAttributeListeners[n].attributeAdded(
                            new ServletContextAttributeEvent(this, name, object));
                    Thread.currentThread().setContextClassLoader(cl);
                }
            }
        }
    }

    // Application level init parameters
    public String getInitParameter(String name) {
        return (String) this.initParameters.get(name);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }

    // Server info
    public String getServerInfo() {
        return Launcher.RESOURCES.getString("ServerVersion");
    }

    public int getMajorVersion() {
        return 2;
    }

    public int getMinorVersion() {
        return 5;
    }

    // Weird mostly deprecated crap to do with getting servlet instances
    public javax.servlet.ServletContext getContext(String uri) {
        return this.ownerHostConfig.getWebAppByURI(uri);
    }

    public String getServletContextName() {
        return this.displayName;
    }

    /**
     * Look up the map of mimeType extensions, and return the type that matches
     */
    public String getMimeType(String fileName) {
        int dotPos = fileName.lastIndexOf('.');
        if ((dotPos != -1) && (dotPos != fileName.length() - 1)) {
            String extension = fileName.substring(dotPos + 1).toLowerCase();
            String mimeType = (String) this.mimeTypes.get(extension);
            return mimeType;
        } else
            return null;
    }

    // Context level log statements
    public void log(String message) {
        Logger.logDirectMessage(Logger.INFO, this.contextName, message, null);
    }

    public void log(String message, Throwable throwable) {
        Logger.logDirectMessage(Logger.ERROR, this.contextName, message, throwable);
    }

    /**
     * Named dispatcher - this basically gets us a simple exact dispatcher (no
     * url matching, no request attributes and no security)
     */
    public javax.servlet.RequestDispatcher getNamedDispatcher(String name) {
        ServletConfiguration servlet = (ServletConfiguration) this.servletInstances.get(name);
        if (servlet != null) {
            RequestDispatcher rd = new RequestDispatcher(this, servlet);
            if (rd != null) {
                rd.setForNamedDispatcher(this.filterPatternsForward, this.filterPatternsInclude);
                return rd;
            }
        }
        return null;
    }

    /**
     * Gets a dispatcher, which sets the request attributes, etc on a
     * forward/include. Doesn't execute security though.
     */
    public javax.servlet.RequestDispatcher getRequestDispatcher(
            String uriInsideWebapp) {
        if (uriInsideWebapp == null) {
            return null;
        } else if (!uriInsideWebapp.startsWith("/")) {
            return null;
        }

        // Parse the url for query string, etc
        String queryString = "";
        int questionPos = uriInsideWebapp.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != uriInsideWebapp.length() - 1) {
                queryString = uriInsideWebapp.substring(questionPos + 1);
            }
            uriInsideWebapp = uriInsideWebapp.substring(0, questionPos);
        }

        // Return the dispatcher
        StringBuffer servletPath = new StringBuffer();
        StringBuffer pathInfo = new StringBuffer();
        ServletConfiguration servlet = urlMatch(uriInsideWebapp, servletPath, pathInfo);
        if (servlet != null) {
            RequestDispatcher rd = new RequestDispatcher(this, servlet);
            if (rd != null) {
                rd.setForURLDispatcher(servletPath.toString(), pathInfo.toString()
                        .equals("") ? null : pathInfo.toString(), queryString,
                        uriInsideWebapp, this.filterPatternsForward,
                        this.filterPatternsInclude);
                return rd;
            }
        }
        return null;
    }

    /**
     * Creates the dispatcher that corresponds to a request level dispatch (ie
     * the initial entry point). The difference here is that we need to set up
     * the dispatcher so that on a forward, it executes the security checks and
     * the request filters, while not setting any of the request attributes for
     * a forward. Also, we can't return a null dispatcher in error case - instead 
     * we have to return a dispatcher pre-init'd for showing an error page (eg 404).
     * A null dispatcher is interpreted to mean a successful 302 has occurred. 
     */
    public RequestDispatcher getInitialDispatcher(String uriInsideWebapp,
            WinstoneRequest request, WinstoneResponse response)
            throws IOException {
        if (!uriInsideWebapp.equals("") && !uriInsideWebapp.startsWith("/")) {
            return this.getErrorDispatcherByCode(
                    HttpServletResponse.SC_BAD_REQUEST,
                    Launcher.RESOURCES.getString("WebAppConfig.InvalidURI", uriInsideWebapp),
                    null);
        } else if (this.contextStartupError != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw, true);
            this.contextStartupError.printStackTrace(pw);
            return this.getErrorDispatcherByCode(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Launcher.RESOURCES.getString("WebAppConfig.ErrorDuringStartup", sw.toString()), 
                    this.contextStartupError);
        }

        // Parse the url for query string, etc 
        String queryString = "";
        int questionPos = uriInsideWebapp.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != uriInsideWebapp.length() - 1)
                queryString = uriInsideWebapp.substring(questionPos + 1);
            uriInsideWebapp = uriInsideWebapp.substring(0, questionPos);
        }

        // Return the dispatcher
        StringBuffer servletPath = new StringBuffer();
        StringBuffer pathInfo = new StringBuffer();
        ServletConfiguration servlet = urlMatch(uriInsideWebapp, servletPath, pathInfo);
        if (servlet != null) {
            // If the default servlet was returned, we should check for welcome files
            if (servlet.getServletName().equals(this.defaultServletName)) {
                // Is path a directory ?
                String directoryPath = servletPath.toString();
                if (directoryPath.endsWith("/")) {
                    directoryPath = directoryPath.substring(0, directoryPath.length() - 1);
                }
                if (directoryPath.startsWith("/")) {
                    directoryPath = directoryPath.substring(1);
                }

                File res = new File(webRoot, directoryPath);
                if (res.exists() && res.isDirectory() && 
                        (request.getMethod().equals("GET") || request.getMethod().equals("HEAD"))) {
                    // Check for the send back with slash case
                    if (!servletPath.toString().endsWith("/")) {
                        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                                "WebAppConfig.FoundNonSlashDirectory", servletPath.toString());
                        response.sendRedirect(this.prefix
                                + servletPath.toString()
                                + pathInfo.toString()
                                + "/"
                                + (queryString.equals("") ? "" : "?" + queryString));
                        return null;
                    }

                    // Check for welcome files
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "WebAppConfig.CheckWelcomeFile", servletPath.toString()
                                    + pathInfo.toString());
                    String welcomeFile = matchWelcomeFiles(servletPath.toString()
                            + pathInfo.toString(), request, queryString);
                    if (welcomeFile != null) {
                        response.sendRedirect(this.prefix + welcomeFile);
//                                + servletPath.toString()
//                                + pathInfo.toString()
//                                + welcomeFile
//                                + (queryString.equals("") ? "" : "?" + queryString));
                        return null;
                    }
                }
            }

            RequestDispatcher rd = new RequestDispatcher(this, servlet);
            rd.setForInitialDispatcher(servletPath.toString(), 
                    pathInfo.toString().equals("") ? null : pathInfo.toString(), queryString,
                    uriInsideWebapp, this.filterPatternsRequest, this.authenticationHandler);
            return rd;
        }
        
        // If we are here, return a 404
        return this.getErrorDispatcherByCode(HttpServletResponse.SC_NOT_FOUND,
                Launcher.RESOURCES.getString("StaticResourceServlet.PathNotFound", 
                        uriInsideWebapp), null);
    }

    /**
     * Gets a dispatcher, set up for error dispatch.
     */
    public RequestDispatcher getErrorDispatcherByClass(
            Throwable exception) {

        // Check for exception class match
        Class exceptionClasses[] = this.errorPagesByExceptionKeysSorted;
        Throwable errWrapper = new ServletException(exception);
        
        while (errWrapper instanceof ServletException) {
            errWrapper = ((ServletException) errWrapper).getRootCause();
            if (errWrapper == null) {
                break;
            }
            for (int n = 0; n < exceptionClasses.length; n++) {

                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "WinstoneResponse.TestingException",
                        new String[] {this.errorPagesByExceptionKeysSorted[n].getName(),
                            errWrapper.getClass().getName()});
                if (exceptionClasses[n].isInstance(errWrapper)) {
                    String errorURI = (String) this.errorPagesByException.get(exceptionClasses[n]);
                    if (errorURI != null) {
                        RequestDispatcher rd = buildErrorDispatcher(errorURI, 
                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                null, errWrapper);
                        if (rd != null) {
                            return rd;
                        }
                    } else {
                        Logger.log(Logger.WARNING, Launcher.RESOURCES, 
                                "WinstoneResponse.SkippingException",
                                new String[] {exceptionClasses[n].getName(),
                                    (String) this.errorPagesByException.get(exceptionClasses[n]) });
                    }
                } else {
                    Logger.log(Logger.WARNING, Launcher.RESOURCES, 
                            "WinstoneResponse.ExceptionNotMatched", 
                            exceptionClasses[n].getName());
                }
            }
        }
        
        // Otherwise throw a code error
        Throwable errPassDown = exception;
        while ((errPassDown instanceof ServletException) && 
                (((ServletException) errPassDown).getRootCause() != null)) {
            errPassDown = ((ServletException) errPassDown).getRootCause();
        }
        return getErrorDispatcherByCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                null, errPassDown);
    }
    
    public RequestDispatcher getErrorDispatcherByCode(
            int statusCode, String summaryMessage, Throwable exception) {
        // Check for status code match
        String errorURI = (String) getErrorPagesByCode().get("" + statusCode);
        if (errorURI != null) {
            RequestDispatcher rd = buildErrorDispatcher(errorURI, statusCode, 
                    summaryMessage, exception);
            if (rd != null) {
                return rd;
            }
        }
        
        // If no dispatcher available, return a dispatcher to the default error formatter
        ServletConfiguration errorServlet = (ServletConfiguration) 
                    this.servletInstances.get(this.errorServletName);
        if (errorServlet != null) {
            RequestDispatcher rd = new RequestDispatcher(this, errorServlet);
            if (rd != null) {
                rd.setForErrorDispatcher(null, null, null, statusCode, 
                        summaryMessage, exception, null, this.filterPatternsError);
                return rd;
            }
        }
        
        // Otherwise log and return null
        Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebAppConfig.NoErrorServlet", "" + statusCode, exception);
        return null;
    }

    /**
     * Build a dispatcher to the error handler if it's available. If it fails, return null.
     */
    private RequestDispatcher buildErrorDispatcher(String errorURI, int statusCode, 
            String summaryMessage, Throwable exception) {
        // Parse the url for query string, etc 
        String queryString = "";
        int questionPos = errorURI.indexOf('?');
        if (questionPos != -1) {
            if (questionPos != errorURI.length() - 1) {
                queryString = errorURI.substring(questionPos + 1);
            }
            errorURI = errorURI.substring(0, questionPos);
        }
        
        // Get the message by recursing if none supplied
        ServletException errIterator = new ServletException(exception);
        while ((summaryMessage == null) && (errIterator != null)) {
            summaryMessage = errIterator.getMessage();
            if (errIterator.getRootCause() instanceof ServletException) {
                errIterator = (ServletException) errIterator.getRootCause(); 
            } else {
                if (summaryMessage == null) {
                    summaryMessage = errIterator.getRootCause().getMessage();
                }
                errIterator = null;
            }
        }

        // Return the dispatcher
        StringBuffer servletPath = new StringBuffer();
        StringBuffer pathInfo = new StringBuffer();
        ServletConfiguration servlet = urlMatch(errorURI, servletPath, pathInfo);
        if (servlet != null) {
            RequestDispatcher rd = new RequestDispatcher(this, servlet);
            if (rd != null) {
                rd.setForErrorDispatcher(servletPath.toString(), 
                        pathInfo.toString().equals("") ? null : pathInfo.toString(), 
                                queryString, statusCode, summaryMessage, 
                                exception, errorURI, this.filterPatternsError);
                return rd;
            }
        }
        return null;
    }

    /**
     * Check if any of the welcome files under this path are available. Returns the 
     * name of the file if found, null otherwise. Returns the full internal webapp uri
     */
    private String matchWelcomeFiles(String path, WinstoneRequest request, String queryString) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        String qs = (queryString.equals("") ? "" : "?" + queryString);
        for (int n = 0; n < this.welcomeFiles.length; n++) {
            String welcomeFile = this.welcomeFiles[n];
            while (welcomeFile.startsWith("/")) {
                welcomeFile = welcomeFile.substring(1);
            }
            welcomeFile = path + welcomeFile;
            
            String exact = (String) this.exactServletMatchMounts.get(welcomeFile);
            if (exact != null) {
                return welcomeFile + qs;
            }

            // Inexact folder mount check - note folder mounts only
            for (int j = 0; j < this.patternMatches.length; j++) {
                Mapping urlPattern = this.patternMatches[j];
                if ((urlPattern.getPatternType() == Mapping.FOLDER_PATTERN)
                        && urlPattern.match(welcomeFile, null, null)) {
                    return welcomeFile + qs;
                }
            }

            try {
                if (getResource(welcomeFile) != null) {
                    return welcomeFile + qs;
                }
            } catch (MalformedURLException err) {}
        }
        return null;
    }

    // Getting resources via the classloader
    public URL getResource(String path) throws MalformedURLException {
        if (path == null) {
            return null;
        } else if (!path.startsWith("/")) {
            throw new MalformedURLException(Launcher.RESOURCES.getString(
                    "WebAppConfig.BadResourcePath", path));
        } else if (!path.equals("/") && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        File res = new File(webRoot, path.substring(1));
        return (res != null) && res.exists() ? res.toURL() : null;
    }

    public InputStream getResourceAsStream(String path) {
        try {
            URL res = getResource(path);
            return res == null ? null : res.openStream();
        } catch (IOException err) {
            throw new WinstoneException(Launcher.RESOURCES
                    .getString("WebAppConfig.ErrorOpeningStream"), err);
        }
    }

    public String getRealPath(String path) {
        // Trim the prefix
        if (path == null)
            return null;
        else {
            try {
                File res = new File(this.webRoot, path);
                if (res.isDirectory())
                    return res.getCanonicalPath() + "/";
                else
                    return res.getCanonicalPath();
            } catch (IOException err) {
                return null;
            }
        }
    }

    public Set getResourcePaths(String path) {
        // Trim the prefix
        if (path == null)
            return null;
        else if (!path.startsWith("/"))
            throw new WinstoneException(Launcher.RESOURCES.getString(
                    "WebAppConfig.BadResourcePath", path));
        else {
            String workingPath = null;
            if (path.equals("/"))
                workingPath = "";
            else {
                boolean lastCharIsSlash = path.charAt(path.length() - 1) == '/';
                workingPath = path.substring(1, path.length()
                        - (lastCharIsSlash ? 1 : 0));
            }
            File inPath = new File(this.webRoot, workingPath.equals("") ? "."
                    : workingPath).getAbsoluteFile();
            if (!inPath.exists())
                return null;
            else if (!inPath.isDirectory())
                return null;

            // Find all the files in this folder
            File children[] = inPath.listFiles();
            Set out = new HashSet();
            for (int n = 0; n < children.length; n++) {
                // Write the entry as subpath + child element
                String entry = //this.prefix + 
                "/" + (workingPath.length() != 0 ? workingPath + "/" : "")
                        + children[n].getName()
                        + (children[n].isDirectory() ? "/" : "");
                out.add(entry);
            }
            return out;
        }
    }

    /**
     * @deprecated
     */
    public javax.servlet.Servlet getServlet(String name) {
        return null;
    }

    /**
     * @deprecated
     */
    public Enumeration getServletNames() {
        return Collections.enumeration(new ArrayList());
    }

    /**
     * @deprecated
     */
    public Enumeration getServlets() {
        return Collections.enumeration(new ArrayList());
    }

    /**
     * @deprecated
     */
    public void log(Exception exception, String msg) {
        this.log(msg, exception);
    }

}
