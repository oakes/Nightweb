/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;


/**
 * This is used by the ACL filter to allow a retry by using a key lookup
 * on old request. It's only used when retrying an old request that was blocked
 * by the ACL filter.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: RetryRequestParams.java,v 1.2 2007/06/01 15:59:53 rickknowles Exp $
 */
public class RetryRequestParams implements java.io.Serializable {

    private String method;
    private String scheme;
    private String contextPath;
    private String servletPath;
    private String pathInfo;
    private String queryString;
    private String protocol;
    private int contentLength;
    private String contentType;
    private String encoding;
    private Map headers;
    private Vector locales;
    private Locale locale;
    private byte[] bodyContent;

    /**
     * Constructor - this populates the wrapper from the object in session
     */
    public RetryRequestParams(ServletRequest request) throws IOException {
        this.protocol = request.getProtocol();
        this.locales = new Vector(Collections.list(request.getLocales()));
        this.locale = request.getLocale();
        this.contentLength = request.getContentLength();
        this.contentType = request.getContentType();
        this.encoding = request.getCharacterEncoding();
        this.headers = new HashMap();

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            this.method = httpRequest.getMethod();
            this.contextPath = httpRequest.getContextPath();
            this.servletPath = httpRequest.getServletPath();
            this.pathInfo = httpRequest.getPathInfo();
            this.queryString = httpRequest.getQueryString();
            
            for (Enumeration names = httpRequest.getHeaderNames(); names.hasMoreElements();) {
                String name = (String) names.nextElement();
                headers.put(name.toLowerCase(), new Vector(Collections.list(httpRequest.getHeaders(name))));
            }
        }
        
        if (((this.method == null) || this.method.equalsIgnoreCase("POST")) && (this.contentLength != -1)) {
            InputStream inData = request.getInputStream();
            this.bodyContent = new byte[this.contentLength];
            int readCount = 0;
            int read = 0;
            while ((read = inData.read(this.bodyContent, readCount, this.contentLength - readCount)) >= 0) {
                readCount += read;
            }
            inData.close();
        }
    }

    public byte[] getBodyContent() {
        return bodyContent;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEncoding() {
        return encoding;
    }

    public Map getHeaders() {
        return headers;
    }

    public Locale getLocale() {
        return locale;
    }

    public Vector getLocales() {
        return locales;
    }

    public String getMethod() {
        return method;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getScheme() {
        return scheme;
    }

    public String getServletPath() {
        return servletPath;
    }

    public String getContextPath() {
        return contextPath;
    }
}
