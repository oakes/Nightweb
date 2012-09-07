/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import winstone.Launcher;
import winstone.Logger;
import winstone.WinstoneException;
import winstone.WinstoneInputStream;
import winstone.WinstoneRequest;


/**
 * This is used by the ACL filter to allow a retry by using a key lookup
 * on old request. It's only used when retrying an old request that was blocked
 * by the ACL filter.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: RetryRequestWrapper.java,v 1.3 2007/02/26 00:28:05 rickknowles Exp $
 */
public class RetryRequestWrapper extends HttpServletRequestWrapper {
    protected static final DateFormat headerDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        headerDF.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final static String METHOD_HEAD = "GET";
    private final static String METHOD_GET = "GET";
    private final static String METHOD_POST = "POST";
    private final static String POST_PARAMETERS = "application/x-www-form-urlencoded";

    private RetryRequestParams oldRequest;

    // PARAMETER/BODY RELATED FUNCTIONS
    private String encoding;
    private Map parsedParams;
    private ServletInputStream inData;

    /**
     * Constructor - this populates the wrapper from the object in session
     */
    public RetryRequestWrapper(HttpServletRequest request, RetryRequestParams oldRequest)
                                 throws IOException {
        super(request);
        this.oldRequest = oldRequest;
        this.encoding = this.oldRequest.getEncoding();
    }

    private boolean hasBeenForwarded() {
        return (super.getAttribute("javax.servlet.forward.request_uri") != null);
    }
    
    public String getScheme() {
        if (hasBeenForwarded()) {
            return super.getScheme();
        } else {
            return this.oldRequest.getScheme();
        }
    }

    public String getMethod() {
        if (hasBeenForwarded()) {
            return super.getMethod();
        } else {
            return this.oldRequest.getMethod();
        }
    }

    public String getContextPath() {
        if (hasBeenForwarded()) {
            return super.getContextPath();
        } else {
            return this.oldRequest.getContextPath();
        }
    }

    public String getServletPath() {
        if (hasBeenForwarded()) {
            return super.getServletPath();
        } else {
            return this.oldRequest.getServletPath();
        }
    }

    public String getPathInfo() {
        if (hasBeenForwarded()) {
            return super.getPathInfo();
        } else {
            return this.oldRequest.getPathInfo();
        }
    }

    public String getQueryString() {
        if (hasBeenForwarded()) {
            return super.getQueryString();
        } else {
            return this.oldRequest.getQueryString();
        }
    }

    public String getRequestURI() {
        if (hasBeenForwarded()) {
            return super.getRequestURI();
        } else {
            String contextPath = this.oldRequest.getContextPath();
            String servletPath = this.oldRequest.getServletPath();
            String pathInfo = this.oldRequest.getPathInfo();
            String queryString = this.oldRequest.getQueryString();
            return contextPath + servletPath + ((pathInfo == null) ? "" : pathInfo)
                   + ((queryString == null) ? "" : ("?" + queryString));
        }
    }

    public String getCharacterEncoding() {
        if (hasBeenForwarded()) {
            return super.getCharacterEncoding();
        } else {
            return this.oldRequest.getEncoding();
        }
    }

    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
        if (hasBeenForwarded()) {
            super.setCharacterEncoding(encoding);
        } else {
            this.encoding = encoding;
        }
    }

    public int getContentLength() {
        if (hasBeenForwarded()) {
            return super.getContentLength();
        } else {
            return this.oldRequest.getContentLength();
        }
    }

    public String getContentType() {
        if (hasBeenForwarded()) {
            return super.getContentType();
        } else {
            return this.oldRequest.getContentType();
        }
    }

    public Locale getLocale() {
        if (hasBeenForwarded()) {
            return super.getLocale();
        } else {
            return this.oldRequest.getLocale();
        }
    }

    public Enumeration getLocales() {
        if (hasBeenForwarded()) {
            return super.getLocales();
        } else {
            return this.oldRequest.getLocales().elements();
        }
    }

    // -------------------------------------------------------------------
    // HEADER RELATED FUNCTIONS
    public long getDateHeader(String name) {
        if (hasBeenForwarded()) {
            return super.getDateHeader(name);
        } else {
            String dateHeader = getHeader(name);
            if (dateHeader == null) {
                return -1;
            } else {
                try {
                    synchronized (headerDF) {
                        return headerDF.parse(dateHeader).getTime();
                    }
                } catch (java.text.ParseException err) {
                    throw new IllegalArgumentException("Illegal date format: " + dateHeader);
                }
            }
        }
    }
    
    public int getIntHeader(String name) {
        if (hasBeenForwarded()) {
            return super.getIntHeader(name);
        } else {
            String header = getHeader(name);
            return header == null ? -1 : Integer.parseInt(header);
        }
    }

    public String getHeader(String name) {
        if (hasBeenForwarded()) {
            return super.getHeader(name);
        } else {
            Enumeration e = getHeaders(name);
            return (e != null) && e.hasMoreElements() ? (String) e.nextElement() : null;
        }
    }

    public Enumeration getHeaderNames() {
        if (hasBeenForwarded()) {
            return super.getHeaderNames();
        } else {
            return Collections.enumeration(this.oldRequest.getHeaders().keySet());
        }
    }

    public Enumeration getHeaders(String name) {
        if (hasBeenForwarded()) {
            return super.getHeaders(name);
        } else {
            Vector result = (Vector) this.oldRequest.getHeaders().get(name.toLowerCase()); 
            return result == null ? null : result.elements();
        }
    }

    public String getParameter(String name) {
        if (hasBeenForwarded()) {
            return super.getParameter(name);
        } else {
            parseRequestParameters();
            Object param = this.parsedParams.get(name);
            if (param == null) {
                return null;
            } else if (param instanceof String) {
                return (String) param;
            } else if (param instanceof String[]) {
                return ((String[]) param)[0];
            } else {
                return param.toString();
            }
        }
    }

    public Enumeration getParameterNames() {
        if (hasBeenForwarded()) {
            return super.getParameterNames();
        } else {
            parseRequestParameters();
            return Collections.enumeration(this.parsedParams.keySet());
        }
    }

    public String[] getParameterValues(String name) {
        if (hasBeenForwarded()) {
            return super.getParameterValues(name);
        } else {
            parseRequestParameters();
            Object param = this.parsedParams.get(name);
            if (param == null) {
                return null;
            } else if (param instanceof String) {
                return new String[] {(String) param};
            } else if (param instanceof String[]) {
                return (String[]) param;
            } else {
                throw new WinstoneException(Launcher.RESOURCES.getString(
                        "WinstoneRequest.UnknownParameterType", name + " - "
                                + param.getClass()));
            }
        }
    }

    public Map getParameterMap() {
        if (hasBeenForwarded()) {
            return super.getParameterMap();
        } else {
            Hashtable paramMap = new Hashtable();
            for (Enumeration names = this.getParameterNames(); names.hasMoreElements();) {
                String name = (String) names.nextElement();
                paramMap.put(name, getParameterValues(name));
            }
            return paramMap;
        }
    }

    public BufferedReader getReader() throws IOException {
        if (hasBeenForwarded()) {
            return super.getReader();
        } else if (getCharacterEncoding() != null) {
            return new BufferedReader(new InputStreamReader(getInputStream(), this.encoding));
        } else {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }
    }

    public ServletInputStream getInputStream() throws IOException {
        if (hasBeenForwarded()) {
            return super.getInputStream();
        } else if (this.parsedParams != null) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES, "WinstoneRequest.BothMethods");
        }

        if (this.inData == null) {
            this.inData = new WinstoneInputStream(this.oldRequest.getBodyContent());
        }

        return this.inData;
    }

    // -------------------------------------------------------------------

    /**
     * This takes the parameters in the body of the request and puts them into
     * the parameters map.
     */
    private void parseRequestParameters() {
        if (inData != null) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES, "WinstoneRequest.BothMethods");
        }

        if (this.parsedParams == null) {
            String contentType = this.oldRequest.getContentType();
            String queryString = this.oldRequest.getQueryString();
            String method = this.oldRequest.getMethod();
            Map workingParameters = new HashMap();
            try {
                // Parse query string from request
                if ((method.equals(METHOD_GET) || method.equals(METHOD_HEAD) || 
                        method.equals(METHOD_POST)) && (queryString != null)) {
                    WinstoneRequest.extractParameters(queryString, this.encoding, workingParameters, false);
                }
                
                if (method.equals(METHOD_POST) && (contentType != null)
                        && (contentType.equals(POST_PARAMETERS) || contentType.startsWith(POST_PARAMETERS + ";"))) {
                    // Parse params
                    String paramLine = (this.encoding == null ? new String(this.oldRequest.getBodyContent()) 
                            : new String(this.oldRequest.getBodyContent(), this.encoding));
                    WinstoneRequest.extractParameters(paramLine.trim(), this.encoding, workingParameters, false);
                } 
                
                this.parsedParams = workingParameters;
            } catch (UnsupportedEncodingException err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "WinstoneRequest.ErrorBodyParameters", err);
                this.parsedParams = null;
            }
        }
    }
}
