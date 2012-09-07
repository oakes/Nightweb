/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


/**
 * Implements the request interface required by the servlet spec.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneRequest.java,v 1.38 2007/10/28 16:29:02 rickknowles Exp $
 */
public class WinstoneRequest implements HttpServletRequest {
    protected static DateFormat headerDF = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    protected static Random rnd = null;
    static {
        headerDF.setTimeZone(TimeZone.getTimeZone("GMT"));
        rnd = new Random(System.currentTimeMillis());
    }

    // Request header constants
    static final String CONTENT_LENGTH_HEADER = "Content-Length";
    static final String CONTENT_TYPE_HEADER = "Content-Type";
    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String LOCALE_HEADER = "Accept-Language";
    static final String HOST_HEADER = "Host";
    static final String IN_COOKIE_HEADER1 = "Cookie";
    static final String IN_COOKIE_HEADER2 = "Cookie2";
    static final String METHOD_HEAD = "HEAD";
    static final String METHOD_GET = "GET";
    static final String METHOD_POST = "POST";
    static final String POST_PARAMETERS = "application/x-www-form-urlencoded";

    protected Map attributes;
    protected Map parameters;
    protected Stack attributesStack;
    protected Stack parametersStack;
//    protected Map forwardedParameters;

    protected String headers[];
    protected Cookie cookies[];
    
    protected String method;
    protected String scheme;
    protected String serverName;
    protected String requestURI;
    protected String servletPath;
    protected String pathInfo;
    protected String queryString;
    protected String protocol;
    protected int contentLength;
    protected String contentType;
    protected String encoding;
    
    protected int serverPort;
    protected String remoteIP;
    protected String remoteName;
    protected int remotePort;
    protected String localAddr;
    protected String localName;
    protected int localPort;
    protected Boolean parsedParameters;
    protected Map requestedSessionIds;
    protected Map currentSessionIds;
    protected String deadRequestedSessionId;
    protected List locales;
    protected String authorization;
    protected boolean isSecure;
    
    protected WinstoneInputStream inputData;
    protected BufferedReader inputReader;
    protected ServletConfiguration servletConfig;
    protected WebAppConfiguration webappConfig;
    protected HostGroup hostGroup;

    protected AuthenticationPrincipal authenticatedUser;
    protected ServletRequestAttributeListener requestAttributeListeners[];
    protected ServletRequestListener requestListeners[];
    
    private MessageDigest md5Digester;
    
    private Set usedSessions;
    
    /**
     * InputStream factory method.
     */
    public WinstoneRequest() throws IOException {
        this.attributes = new Hashtable();
        this.parameters = new Hashtable();
        this.locales = new ArrayList();
        this.attributesStack = new Stack();
        this.parametersStack = new Stack();
//        this.forwardedParameters = new Hashtable();
        this.requestedSessionIds = new Hashtable();
        this.currentSessionIds = new Hashtable();
        this.usedSessions = new HashSet();
        this.contentLength = -1;
        this.isSecure = false;
        try {
            this.md5Digester = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException err) {
            throw new WinstoneException(
                    "MD5 digester unavailable - what the ...?");
        }
    }

    /**
     * Resets the request to be reused
     */
    public void cleanUp() {
        this.requestListeners = null;
        this.requestAttributeListeners = null;
        this.attributes.clear();
        this.parameters.clear();
        this.attributesStack.clear();
        this.parametersStack.clear();
//        this.forwardedParameters.clear();
        this.usedSessions.clear();
        this.headers = null;
        this.cookies = null;
        this.method = null;
        this.scheme = null;
        this.serverName = null;
        this.requestURI = null;
        this.servletPath = null;
        this.pathInfo = null;
        this.queryString = null;
        this.protocol = null;
        this.contentLength = -1;
        this.contentType = null;
        this.encoding = null;
        this.inputData = null;
        this.inputReader = null;
        this.servletConfig = null;
        this.webappConfig = null;
        this.hostGroup = null;
        this.serverPort = -1;
        this.remoteIP = null;
        this.remoteName = null;
        this.remotePort = -1;
        this.localAddr = null;
        this.localName = null;
        this.localPort = -1;
        this.parsedParameters = null;
        this.requestedSessionIds.clear();
        this.currentSessionIds.clear();
        this.deadRequestedSessionId = null;
        this.locales.clear();
        this.authorization = null;
        this.isSecure = false;
        this.authenticatedUser = null;
    }

    /**
     * Steps through the header array, searching for the first header matching
     */
    private String extractFirstHeader(String name) {
        for (int n = 0; n < this.headers.length; n++) {
            if (this.headers[n].toUpperCase().startsWith(name.toUpperCase() + ':')) {
                return this.headers[n].substring(name.length() + 1).trim(); // 1 for colon
            }
        }
        return null;
    }

    private Collection extractHeaderNameList() {
        Collection headerNames = new HashSet();
        for (int n = 0; n < this.headers.length; n++) {
            String name = this.headers[n];
            int colonPos = name.indexOf(':');
            headerNames.add(name.substring(0, colonPos));
        }
        return headerNames;
    }

    public Map getAttributes() {
        return this.attributes;
    }

    public Map getParameters() {
        return this.parameters;
    }
//
//    public Map getForwardedParameters() {
//        return this.forwardedParameters;
//    }

    public Stack getAttributesStack() {
        return this.attributesStack;
    }
    
    public Stack getParametersStack() {
        return this.parametersStack;
    }
    
    public Map getCurrentSessionIds() {
        return this.currentSessionIds;
    }
    
    public Map getRequestedSessionIds() {
        return this.requestedSessionIds;
    }
    
    public String getDeadRequestedSessionId() {
        return this.deadRequestedSessionId;
    }

    public HostGroup getHostGroup() {
        return this.hostGroup;
    }

    public WebAppConfiguration getWebAppConfig() {
        return this.webappConfig;
    }

    public ServletConfiguration getServletConfig() {
        return this.servletConfig;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public Boolean getParsedParameters() {
        return this.parsedParameters;
    }

    public List getListLocales() {
        return this.locales;
    }

    public void setInputStream(WinstoneInputStream inputData) {
        this.inputData = inputData;
    }

    public void setHostGroup(HostGroup hostGroup) {
        this.hostGroup = hostGroup;
    }

    public void setWebAppConfig(WebAppConfiguration webappConfig) {
        this.webappConfig = webappConfig;
    }

    public void setServletConfig(ServletConfiguration servletConfig) {
        this.servletConfig = servletConfig;
    }

    public void setServerPort(int port) {
        this.serverPort = port;
    }

    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    public void setRemoteName(String name) {
        this.remoteName = name;
    }

    public void setRemotePort(int port) {
        this.remotePort = port;
    }

    public void setLocalAddr(String ip) {
        this.localName = ip;
    }

    public void setLocalName(String name) {
        this.localName = name;
    }

    public void setLocalPort(int port) {
        this.localPort = port;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setIsSecure(boolean isSecure) {
        this.isSecure = isSecure;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public void setServerName(String name) {
        this.serverName = name;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public void setProtocol(String protocolString) {
        this.protocol = protocolString;
    }

    public void setRemoteUser(AuthenticationPrincipal user) {
        this.authenticatedUser = user;
    }

    public void setContentLength(int len) {
        this.contentLength = len;
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public void setAuthorization(String auth) {
        this.authorization = auth;
    }

    public void setLocales(List locales) {
        this.locales = locales;
    }

    public void setCurrentSessionIds(Map currentSessionIds) {
        this.currentSessionIds = currentSessionIds;
    }
    
    public void setRequestedSessionIds(Map requestedSessionIds) {
        this.requestedSessionIds = requestedSessionIds;
    }

    public void setDeadRequestedSessionId(String deadRequestedSessionId) {
        this.deadRequestedSessionId = deadRequestedSessionId;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setParsedParameters(Boolean parsed) {
        this.parsedParameters = parsed;
    }

    public void setRequestListeners(ServletRequestListener rl[]) {
        this.requestListeners = rl;
    }

    public void setRequestAttributeListeners(
            ServletRequestAttributeListener ral[]) {
        this.requestAttributeListeners = ral;
    }

    /**
     * Gets parameters from the url encoded parameter string
     */
    public static void extractParameters(String urlEncodedParams,
            String encoding, Map outputParams, boolean overwrite) {
        Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                "WinstoneRequest.ParsingParameters", new String[] {
                        urlEncodedParams, encoding });
        StringTokenizer st = new StringTokenizer(urlEncodedParams, "&", false);
        Set overwrittenParamNames = null;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int equalPos = token.indexOf('=');
            try {
                String decodedNameDefault = decodeURLToken(equalPos == -1 ? token 
                        : token.substring(0, equalPos));
                String decodedValueDefault = (equalPos == -1 ? "" 
                        : decodeURLToken(token.substring(equalPos + 1)));
                String decodedName = (encoding == null ? decodedNameDefault
                        : new String(decodedNameDefault.getBytes("8859_1"), encoding));
                String decodedValue = (encoding == null ? decodedValueDefault
                        : new String(decodedValueDefault.getBytes("8859_1"), encoding));

                Object already = null;
                if (overwrite) {
                    if (overwrittenParamNames == null) {
                        overwrittenParamNames = new HashSet();
                    }
                    if (!overwrittenParamNames.contains(decodedName)) {
                        overwrittenParamNames.add(decodedName);
                        outputParams.remove(decodedName);
                    }
                }
                already = outputParams.get(decodedName);
                if (already == null) {
                    outputParams.put(decodedName, decodedValue);
                } else if (already instanceof String) {
                    String pair[] = new String[2];
                    pair[0] = (String) already;
                    pair[1] = decodedValue;
                    outputParams.put(decodedName, pair);
                } else if (already instanceof String[]) {
                    String alreadyArray[] = (String[]) already;
                    String oneMore[] = new String[alreadyArray.length + 1];
                    System.arraycopy(alreadyArray, 0, oneMore, 0,
                            alreadyArray.length);
                    oneMore[oneMore.length - 1] = decodedValue;
                    outputParams.put(decodedName, oneMore);
                } else {
                    Logger.log(Logger.WARNING, Launcher.RESOURCES,
                            "WinstoneRequest.UnknownParameterType",
                            decodedName + " = " + decodedValue.getClass());
                }
            } catch (UnsupportedEncodingException err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES,
                        "WinstoneRequest.ErrorParameters", err);
            }
        }
    }

    /**
     * For decoding the URL encoding used on query strings
     */
    public static String decodeURLToken(String in) {
        StringBuffer workspace = new StringBuffer();
        for (int n = 0; n < in.length(); n++) {
            char thisChar = in.charAt(n);
            if (thisChar == '+')
                workspace.append(' ');
            else if (thisChar == '%') {
                String token = in.substring(Math.min(n + 1, in.length()), 
                        Math.min(n + 3, in.length())); 
                try {
                    int decoded = Integer.parseInt(token, 16);
                    workspace.append((char) decoded);
                    n += 2;
                } catch (RuntimeException err) {
                    Logger.log(Logger.WARNING, Launcher.RESOURCES, "WinstoneRequest.InvalidURLTokenChar", token);
                    workspace.append(thisChar);
                }
            } else
                workspace.append(thisChar);
        }
        return workspace.toString();
    }
    
    public void discardRequestBody() {
        if (getContentLength() > 0) {
            try {
                Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneResponse.ForceBodyParsing");
                // If body not parsed
                if ((this.parsedParameters == null) || 
                        (this.parsedParameters.equals(Boolean.FALSE))) {
                    // read full stream length
                    try {
                        InputStream in = getInputStream();
                        byte buffer[] = new byte[2048];
                        while (in.read(buffer) != -1);
                    } catch (IllegalStateException err) {
                        Reader in = getReader();
                        char buffer[] = new char[2048];
                        while (in.read(buffer) != -1);
                    }
                }
            } catch (IOException err) {
                Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneResponse.ErrorForceBodyParsing", err);
            }
        }
    }    

    /**
     * This takes the parameters in the body of the request and puts them into
     * the parameters map.
     */
    public void parseRequestParameters() {
        if ((parsedParameters != null) && !parsedParameters.booleanValue()) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES,
                    "WinstoneRequest.BothMethods");
            this.parsedParameters = Boolean.TRUE;
        } else if (parsedParameters == null) {
            Map workingParameters = new HashMap();
            try {
                // Parse query string from request
                if ((method.equals(METHOD_GET) || method.equals(METHOD_HEAD) || 
                        method.equals(METHOD_POST))
                        && (this.queryString != null)) {
                    extractParameters(this.queryString, this.encoding, workingParameters, false);
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "WinstoneRequest.ParamLine", "" + workingParameters);
                }
                 
                if (method.equals(METHOD_POST) && (contentType != null)
                        && (contentType.equals(POST_PARAMETERS)
                        || contentType.startsWith(POST_PARAMETERS + ";"))) {
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "WinstoneRequest.ParsingBodyParameters");

                    // Parse params
                    byte paramBuffer[] = new byte[contentLength];
                    int readCount = this.inputData.read(paramBuffer);
                    if (readCount != contentLength)
                        Logger.log(Logger.WARNING, Launcher.RESOURCES,
                                "WinstoneRequest.IncorrectContentLength",
                                new String[] { contentLength + "",
                                        readCount + "" });
                    String paramLine = (this.encoding == null ? new String(
                            paramBuffer) : new String(paramBuffer,
                            this.encoding));
                    extractParameters(paramLine.trim(), this.encoding, workingParameters, false);
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "WinstoneRequest.ParamLine", "" + workingParameters);
                } 
                
                this.parameters.putAll(workingParameters);
                this.parsedParameters = Boolean.TRUE;
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES,
                        "WinstoneRequest.ErrorBodyParameters", err);
                this.parsedParameters = null;
            }
        }
    }

    /**
     * Go through the list of headers, and build the headers/cookies arrays for
     * the request object.
     */
    public void parseHeaders(List headerList) {
        // Iterate through headers
        List outHeaderList = new ArrayList();
        List cookieList = new ArrayList();
        for (Iterator i = headerList.iterator(); i.hasNext();) {
            String header = (String) i.next();
            int colonPos = header.indexOf(':');
            String name = header.substring(0, colonPos);
            String value = header.substring(colonPos + 1).trim();

            // Add it to out headers if it's not a cookie
            outHeaderList.add(header);
//            if (!name.equalsIgnoreCase(IN_COOKIE_HEADER1)
//                    && !name.equalsIgnoreCase(IN_COOKIE_HEADER2))

            if (name.equalsIgnoreCase(AUTHORIZATION_HEADER))
                this.authorization = value;
            else if (name.equalsIgnoreCase(LOCALE_HEADER))
                this.locales = parseLocales(value);
            else if (name.equalsIgnoreCase(CONTENT_LENGTH_HEADER))
                this.contentLength = Integer.parseInt(value);
            else if (name.equalsIgnoreCase(HOST_HEADER)) {
                int nextColonPos = value.indexOf(':');
                if ((nextColonPos == -1) || (nextColonPos == value.length() - 1)) {
                    this.serverName = value;
                    if (this.scheme != null) {
                        if (this.scheme.equals("http")) {
                            this.serverPort = 80;
                        } else if (this.scheme.equals("https")) {
                            this.serverPort = 443;
                        }
                    }
                } else {
                    this.serverName = value.substring(0, nextColonPos);
                    this.serverPort = Integer.parseInt(value.substring(nextColonPos + 1));
                }
            }
            else if (name.equalsIgnoreCase(CONTENT_TYPE_HEADER)) {
                this.contentType = value;
                int semicolon = value.lastIndexOf(';');
                if (semicolon != -1) {
                    String encodingClause = value.substring(semicolon + 1).trim();
                    if (encodingClause.startsWith("charset="))
                        this.encoding = encodingClause.substring(8);
                }
            } else if (name.equalsIgnoreCase(IN_COOKIE_HEADER1)
                    || name.equalsIgnoreCase(IN_COOKIE_HEADER2))
                parseCookieLine(value, cookieList);
        }
        this.headers = (String[]) outHeaderList.toArray(new String[0]);
        if (cookieList.isEmpty()) {
            this.cookies = null;
        } else {
            this.cookies = (Cookie[]) cookieList.toArray(new Cookie[0]);
        }
    }

    private static String nextToken(StringTokenizer st) {
        if (st.hasMoreTokens()) {
            return st.nextToken();
        } else {
            return null;
        }
    }
    
    private void parseCookieLine(String headerValue, List cookieList) {
        StringTokenizer st = new StringTokenizer(headerValue, ";", false);
        int version = 0;
        String cookieLine = nextToken(st);

        // check cookie version flag
        if ((cookieLine != null) && cookieLine.startsWith("$Version=")) {
            int equalPos = cookieLine.indexOf('=');
            try {
                version = Integer.parseInt(extractFromQuotes(
                        cookieLine.substring(equalPos + 1).trim()));
            } catch (NumberFormatException err) {
                version = 0;
            }
            cookieLine = nextToken(st);
        }

        // process remainder - parameters
        while (cookieLine != null) {
            cookieLine = cookieLine.trim();
            int equalPos = cookieLine.indexOf('=');
            if (equalPos == -1) {
                // next token
                cookieLine = nextToken(st);
            } else {
                String name = cookieLine.substring(0, equalPos);
                String value = extractFromQuotes(cookieLine.substring(equalPos + 1));
                Cookie thisCookie = new Cookie(name, value);
                thisCookie.setVersion(version);
                thisCookie.setSecure(isSecure());
                cookieList.add(thisCookie);

                // check for path / domain / port
                cookieLine = nextToken(st);
                while ((cookieLine != null) && cookieLine.trim().startsWith("$")) {
                    cookieLine = cookieLine.trim();
                    equalPos = cookieLine.indexOf('=');
                    String attrValue = equalPos == -1 ? "" : cookieLine
                            .substring(equalPos + 1).trim();
                    if (cookieLine.startsWith("$Path")) {
                        thisCookie.setPath(extractFromQuotes(attrValue));
                    } else if (cookieLine.startsWith("$Domain")) {
                        thisCookie.setDomain(extractFromQuotes(attrValue));
                    }
                    cookieLine = nextToken(st);
                }

                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "WinstoneRequest.CookieFound", thisCookie.toString());
                if (thisCookie.getName().equals(WinstoneSession.SESSION_COOKIE_NAME)) {
                    // Find a context that manages this key
                    HostConfiguration hostConfig = this.hostGroup.getHostByName(this.serverName);
                    WebAppConfiguration ownerContext = hostConfig.getWebAppBySessionKey(thisCookie.getValue());
                    if (ownerContext != null) {
                        this.requestedSessionIds.put(ownerContext.getContextPath(), 
                                thisCookie.getValue());
                        this.currentSessionIds.put(ownerContext.getContextPath(), 
                                thisCookie.getValue());
                    }
                    // If not found, it was probably dead
                    else {
                        this.deadRequestedSessionId = thisCookie.getValue();
                    }
//                    this.requestedSessionId = thisCookie.getValue();
//                    this.currentSessionId = thisCookie.getValue();
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "WinstoneRequest.SessionCookieFound", 
                            new String[] {thisCookie.getValue(), 
                            ownerContext == null ? "" : "prefix:" + ownerContext.getContextPath()});
                }
            }
        }
    }
    
    private static String extractFromQuotes(String input) {
        if ((input != null) && input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
    }

    private List parseLocales(String header) {
        // Strip out the whitespace
        StringBuffer lb = new StringBuffer();
        for (int n = 0; n < header.length(); n++) {
            char c = header.charAt(n);
            if (!Character.isWhitespace(c))
                lb.append(c);
        }

        // Tokenize by commas
        Map localeEntries = new HashMap();
        StringTokenizer commaTK = new StringTokenizer(lb.toString(), ",", false);
        for (; commaTK.hasMoreTokens();) {
            String clause = commaTK.nextToken();

            // Tokenize by semicolon
            Float quality = new Float(1);
            if (clause.indexOf(";q=") != -1) {
                int pos = clause.indexOf(";q=");
                try {
                    quality = new Float(clause.substring(pos + 3));
                } catch (NumberFormatException err) {
                    quality = new Float(0);
                }
                clause = clause.substring(0, pos);
            }

            // Build the locale
            String language = "";
            String country = "";
            String variant = "";
            int dpos = clause.indexOf('-');
            if (dpos == -1)
                language = clause;
            else {
                language = clause.substring(0, dpos);
                String remainder = clause.substring(dpos + 1);
                int d2pos = remainder.indexOf('-');
                if (d2pos == -1)
                    country = remainder;
                else {
                    country = remainder.substring(0, d2pos);
                    variant = remainder.substring(d2pos + 1);
                }
            }
            Locale loc = new Locale(language, country, variant);

            // Put into list by quality
            List localeList = (List) localeEntries.get(quality);
            if (localeList == null) {
                localeList = new ArrayList();
                localeEntries.put(quality, localeList);
            }
            localeList.add(loc);
        }

        // Extract and build the list
        Float orderKeys[] = (Float[]) localeEntries.keySet().toArray(new Float[0]);
        Arrays.sort(orderKeys);
        List outputLocaleList = new ArrayList();
        for (int n = 0; n < orderKeys.length; n++) {
            // Skip backwards through the list of maps and add to the output list
            int reversedIndex = (orderKeys.length - 1) - n;
            if ((orderKeys[reversedIndex].floatValue() <= 0)
                    || (orderKeys[reversedIndex].floatValue() > 1))
                continue;
            List localeList = (List) localeEntries.get(orderKeys[reversedIndex]);
            for (Iterator i = localeList.iterator(); i.hasNext();)
                outputLocaleList.add(i.next());
        }

        return outputLocaleList;
    }

    public void addIncludeQueryParameters(String queryString) {
        Map lastParams = new Hashtable();
        if (!this.parametersStack.isEmpty()) {
            lastParams.putAll((Map) this.parametersStack.peek());
        }
        Map newQueryParams = new HashMap();
        if (queryString != null) {
            extractParameters(queryString, this.encoding, newQueryParams, false);
        }
        lastParams.putAll(newQueryParams);
        this.parametersStack.push(lastParams);
    }

    public void addIncludeAttributes(String requestURI, String contextPath,
            String servletPath, String pathInfo, String queryString) {
        Map includeAttributes = new HashMap();
        if (requestURI != null) {
            includeAttributes.put(RequestDispatcher.INCLUDE_REQUEST_URI, requestURI);
        }
        if (contextPath != null) {
            includeAttributes.put(RequestDispatcher.INCLUDE_CONTEXT_PATH, contextPath);
        }
        if (servletPath != null) {
            includeAttributes.put(RequestDispatcher.INCLUDE_SERVLET_PATH, servletPath);
        }
        if (pathInfo != null) {
            includeAttributes.put(RequestDispatcher.INCLUDE_PATH_INFO, pathInfo);
        }
        if (queryString != null) {
            includeAttributes.put(RequestDispatcher.INCLUDE_QUERY_STRING, queryString);
        }
        this.attributesStack.push(includeAttributes);
    }
    
    public void removeIncludeQueryString() {
        if (!this.parametersStack.isEmpty()) {
            this.parametersStack.pop(); 
        }
    }
    
    public void clearIncludeStackForForward() {
        this.parametersStack.clear();
        this.attributesStack.clear();
    }
    
    public void setForwardQueryString(String forwardQueryString) {
//        this.forwardedParameters.clear();
        
        // Parse query string from include / forward
        if (forwardQueryString != null) {
            String oldQueryString = this.queryString == null ? "" : this.queryString;
            boolean needJoiner = !forwardQueryString.equals("") && !oldQueryString.equals("");  
            this.queryString = forwardQueryString + (needJoiner ? "&" : "") + oldQueryString;
            
            if (this.parsedParameters != null) {
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "WinstoneRequest.ParsingParameters", new String[] {
                        forwardQueryString, this.encoding });
                extractParameters(forwardQueryString, this.encoding, this.parameters, true);
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "WinstoneRequest.ParamLine", "" + this.parameters);
            }
        }

    }
    
    public void removeIncludeAttributes() {
        if (!this.attributesStack.isEmpty()) {
            this.attributesStack.pop();
        }
    }
    
    // Implementation methods for the servlet request stuff
    public Object getAttribute(String name) {
        if (!this.attributesStack.isEmpty()) {
            Map includedAttributes = (Map) this.attributesStack.peek();
            Object value = includedAttributes.get(name);
            if (value != null) {
                return value;
            }
        }
        return this.attributes.get(name);
    }

    public Enumeration getAttributeNames() {
        Map attributes = new HashMap(this.attributes);
        if (!this.attributesStack.isEmpty()) {
            Map includedAttributes = (Map) this.attributesStack.peek();
            attributes.putAll(includedAttributes);
        }
        return Collections.enumeration(attributes.keySet());
    }

    public void removeAttribute(String name) {
        Object value = attributes.get(name);
        if (value == null)
            return;

        // fire event
        if (this.requestAttributeListeners != null) {
            for (int n = 0; n < this.requestAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                this.requestAttributeListeners[n].attributeRemoved(
                        new ServletRequestAttributeEvent(this.webappConfig, 
                                this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
        
        this.attributes.remove(name);
    }

    public void setAttribute(String name, Object o) {
        if ((name != null) && (o != null)) {
            Object oldValue = attributes.get(name);
            attributes.put(name, o); // make sure it's set at the top level

            // fire event
            if (this.requestAttributeListeners != null) {
                if (oldValue == null) {
                    for (int n = 0; n < this.requestAttributeListeners.length; n++) {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                        this.requestAttributeListeners[n].attributeAdded(
                                new ServletRequestAttributeEvent(this.webappConfig, 
                                        this, name, o));
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                } else {
                    for (int n = 0; n < this.requestAttributeListeners.length; n++) {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getWebAppConfig().getLoader());
                        this.requestAttributeListeners[n]
                                .attributeReplaced(new ServletRequestAttributeEvent(
                                        this.webappConfig, this, name, oldValue));
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                }
            }
        } else if (name != null) {
            removeAttribute(name);
        }
    }

    public String getCharacterEncoding() {
        return this.encoding;
    }

    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
        "blah".getBytes(encoding); // throws an exception if the encoding is unsupported
        if (this.inputReader == null) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneRequest.SetCharEncoding",
                    new String[] { this.encoding, encoding });
            this.encoding = encoding;
        }
    }

    public int getContentLength() {
        return this.contentLength;
    }

    public String getContentType() {
        return this.contentType;
    }

    public Locale getLocale() {
        return this.locales.isEmpty() ? Locale.getDefault()
                : (Locale) this.locales.get(0);
    }

    public Enumeration getLocales() {
        List sendLocales = this.locales;
        if (sendLocales.isEmpty())
            sendLocales.add(Locale.getDefault());
        return Collections.enumeration(sendLocales);
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getScheme() {
        return this.scheme;
    }

    public boolean isSecure() {
        return this.isSecure;
    }

    public BufferedReader getReader() throws IOException {
        if (this.inputReader != null) {
            return this.inputReader;
        } else {
            if (this.parsedParameters != null) {
                if (this.parsedParameters.equals(Boolean.TRUE)) {
                    Logger.log(Logger.WARNING, Launcher.RESOURCES, "WinstoneRequest.BothMethodsReader");
                } else {
                    throw new IllegalStateException(Launcher.RESOURCES.getString(
                            "WinstoneRequest.CalledReaderAfterStream"));
                }
            }
            if (this.encoding != null) {
                this.inputReader = new BufferedReader(new InputStreamReader(
                        this.inputData, this.encoding));
            } else {
                this.inputReader = new BufferedReader(new InputStreamReader(
                        this.inputData));
            }
            this.parsedParameters = new Boolean(false);
            return this.inputReader;
        }
    }

    public ServletInputStream getInputStream() throws IOException {
        if (this.inputReader != null) {
            throw new IllegalStateException(Launcher.RESOURCES.getString(
                    "WinstoneRequest.CalledStreamAfterReader"));
        }
        if (this.parsedParameters != null) {
            if (this.parsedParameters.equals(Boolean.TRUE)) {
                Logger.log(Logger.WARNING, Launcher.RESOURCES, "WinstoneRequest.BothMethods");
            }
        }
        this.parsedParameters = new Boolean(false);
        return this.inputData;
    }

    public String getParameter(String name) {
        parseRequestParameters();
        Object param = null;
        if (!this.parametersStack.isEmpty()) {
            param = ((Map) this.parametersStack.peek()).get(name);
        }
//        if ((param == null) && this.forwardedParameters.get(name) != null) {
//            param = this.forwardedParameters.get(name);
//        }
        if (param == null) {
            param = this.parameters.get(name);
        }
        if (param == null)
            return null;
        else if (param instanceof String)
            return (String) param;
        else if (param instanceof String[])
            return ((String[]) param)[0];
        else
            return param.toString();
    }

    public Enumeration getParameterNames() {
        parseRequestParameters();
        Set parameterKeys = new HashSet(this.parameters.keySet());
//        parameterKeys.addAll(this.forwardedParameters.keySet());
        if (!this.parametersStack.isEmpty()) {
            parameterKeys.addAll(((Map) this.parametersStack.peek()).keySet());
        }
        return Collections.enumeration(parameterKeys);
    }

    public String[] getParameterValues(String name) {
        parseRequestParameters();
        Object param = null;
        if (!this.parametersStack.isEmpty()) {
            param = ((Map) this.parametersStack.peek()).get(name);
        }
//        if ((param == null) && this.forwardedParameters.get(name) != null) {
//            param = this.forwardedParameters.get(name);
//        }
        if (param == null) {
            param = this.parameters.get(name);
        }
        if (param == null)
            return null;
        else if (param instanceof String) {
            return new String[] {(String) param};
        } else if (param instanceof String[])
            return (String[]) param;
        else
            throw new WinstoneException(Launcher.RESOURCES.getString(
                    "WinstoneRequest.UnknownParameterType", name + " - "
                            + param.getClass()));
    }

    public Map getParameterMap() {
        Hashtable paramMap = new Hashtable();
        for (Enumeration names = this.getParameterNames(); names
                .hasMoreElements();) {
            String name = (String) names.nextElement();
            paramMap.put(name, getParameterValues(name));
        }
        return paramMap;
    }

    public String getServerName() {
        return this.serverName;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public String getRemoteAddr() {
        return this.remoteIP;
    }

    public String getRemoteHost() {
        return this.remoteName;
    }

    public int getRemotePort() {
        return this.remotePort;
    }

    public String getLocalAddr() {
        return this.localAddr;
    }

    public String getLocalName() {
        return this.localName;
    }

    public int getLocalPort() {
        return this.localPort;
    }

    public javax.servlet.RequestDispatcher getRequestDispatcher(String path) {
        if (path.startsWith("/"))
            return this.webappConfig.getRequestDispatcher(path);

        // Take the servlet path + pathInfo, and make an absolute path
        String fullPath = getServletPath()
                + (getPathInfo() == null ? "" : getPathInfo());
        int lastSlash = fullPath.lastIndexOf('/');
        String currentDir = (lastSlash == -1 ? "/" : fullPath.substring(0,
                lastSlash + 1));
        return this.webappConfig.getRequestDispatcher(currentDir + path);
    }

    // Now the stuff for HttpServletRequest
    public String getContextPath() {
        return this.webappConfig.getContextPath();
    }

    public Cookie[] getCookies() {
        return this.cookies;
    }

    public long getDateHeader(String name) {
        String dateHeader = getHeader(name);
        if (dateHeader == null) {
            return -1;
        } else try {
            Date date = null;
            synchronized (headerDF) {
                date = headerDF.parse(dateHeader);
            }
            return date.getTime();
        } catch (java.text.ParseException err) {
            throw new IllegalArgumentException(Launcher.RESOURCES.getString(
                    "WinstoneRequest.BadDate", dateHeader));
        }
    }

    public int getIntHeader(String name) {
        String header = getHeader(name);
        return header == null ? -1 : Integer.parseInt(header);
    }

    public String getHeader(String name) {
        return extractFirstHeader(name);
    }

    public Enumeration getHeaderNames() {
        return Collections.enumeration(extractHeaderNameList());
    }

    public Enumeration getHeaders(String name) {
        List headers = new ArrayList();
        for (int n = 0; n < this.headers.length; n++)
            if (this.headers[n].toUpperCase().startsWith(
                    name.toUpperCase() + ':'))
                headers
                        .add(this.headers[n].substring(name.length() + 1)
                                .trim()); // 1 for colon
        return Collections.enumeration(headers);
    }

    public String getMethod() {
        return this.method;
    }

    public String getPathInfo() {
        return this.pathInfo;
    }

    public String getPathTranslated() {
        return this.webappConfig.getRealPath(this.pathInfo);
    }

    public String getQueryString() {
        return this.queryString;
    }

    public String getRequestURI() {
        return this.requestURI;
    }

    public String getServletPath() {
        return this.servletPath;
    }

    public String getRequestedSessionId() {
        String actualSessionId = (String) this.requestedSessionIds.get(this.webappConfig.getContextPath());
        if (actualSessionId != null) {
            return actualSessionId;
        } else {
            return this.deadRequestedSessionId;
        }
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        url.append(getScheme()).append("://");
        url.append(getServerName());
        if (!((getServerPort() == 80) && getScheme().equals("http"))
                && !((getServerPort() == 443) && getScheme().equals("https")))
            url.append(':').append(getServerPort());
        url.append(getRequestURI()); // need encoded form, so can't use servlet path + path info
        return url;
    }

    public Principal getUserPrincipal() {
        return this.authenticatedUser;
    }

    public boolean isUserInRole(String role) {
        if (this.authenticatedUser == null)
            return false;
        else if (this.servletConfig.getSecurityRoleRefs() == null)
            return this.authenticatedUser.isUserIsInRole(role);
        else {
            String replacedRole = (String) this.servletConfig.getSecurityRoleRefs().get(role);
            return this.authenticatedUser
                    .isUserIsInRole(replacedRole == null ? role : replacedRole);
        }
    }

    public String getAuthType() {
        return this.authenticatedUser == null ? null : this.authenticatedUser
                .getAuthType();
    }

    public String getRemoteUser() {
        return this.authenticatedUser == null ? null : this.authenticatedUser
                .getName();
    }

    public boolean isRequestedSessionIdFromCookie() {
        return (getRequestedSessionId() != null);
    }

    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    public boolean isRequestedSessionIdValid() {
        String requestedId = getRequestedSessionId();
        if (requestedId == null) {
            return false;
        }
        WinstoneSession ws = this.webappConfig.getSessionById(requestedId, false);
        return (ws != null);
//        if (ws == null) {
//            return false;
//        } else {
//            return (validationCheck(ws, System.currentTimeMillis(), false) != null);
//        }
    }

    public HttpSession getSession() {
        return getSession(true);
    }

    public HttpSession getSession(boolean create) {
        String cookieValue = (String) this.currentSessionIds.get(this.webappConfig.getContextPath());

        // Handle the null case
        if (cookieValue == null) {
            if (!create) {
                return null;
            } else {
                cookieValue = makeNewSession().getId();
            }
        }

        // Now get the session object
        WinstoneSession session = this.webappConfig.getSessionById(cookieValue, false);
        if (session != null) {
//            long nowDate = System.currentTimeMillis();
//            session = validationCheck(session, nowDate, create);
//            if (session == null) {
//                this.currentSessionIds.remove(this.webappConfig.getContextPath());
//            }
        }
        if (create && (session == null)) {
            session = makeNewSession();
        }
        if (session != null) {
            this.usedSessions.add(session);
            session.addUsed(this);
        }
        return session;
    }

    /**
     * Make a new session, and return the id
     */
    private WinstoneSession makeNewSession() {
        String cookieValue = "Winstone_" + this.remoteIP + "_"
                + this.serverPort + "_" + System.currentTimeMillis() + rnd.nextLong();
        byte digestBytes[] = this.md5Digester.digest(cookieValue.getBytes());

        // Write out in hex format
        char outArray[] = new char[32];
        for (int n = 0; n < digestBytes.length; n++) {
            int hiNibble = (digestBytes[n] & 0xFF) >> 4;
            int loNibble = (digestBytes[n] & 0xF);
            outArray[2 * n] = (hiNibble > 9 ? (char) (hiNibble + 87)
                    : (char) (hiNibble + 48));
            outArray[2 * n + 1] = (loNibble > 9 ? (char) (loNibble + 87)
                    : (char) (loNibble + 48));
        }

        String newSessionId = new String(outArray);
        this.currentSessionIds.put(this.webappConfig.getContextPath(), newSessionId);
        return this.webappConfig.makeNewSession(newSessionId);
    }

    public void markSessionsAsRequestFinished(long lastAccessedTime, boolean saveSessions) {
        for (Iterator i = this.usedSessions.iterator(); i.hasNext(); ) {
            WinstoneSession session = (WinstoneSession) i.next();
            session.setLastAccessedDate(lastAccessedTime);
            session.removeUsed(this);
            if (saveSessions) {
                session.saveToTemp();
            }
        }
        this.usedSessions.clear();
    }
    
    /**
     * @deprecated
     */
    public String getRealPath(String path) {
        return this.webappConfig.getRealPath(path);
    }

    /**
     * @deprecated
     */
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

}
