/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Wraps a servlet request object using the decorator pattern.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class ServletRequestWrapper implements ServletRequest {
    private ServletRequest request;

    public ServletRequestWrapper(ServletRequest request) {
        setRequest(request);
    }

    public ServletRequest getRequest() {
        return this.request;
    }

    public void setRequest(ServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request was null");
        } else {
            this.request = request;
        }
    }

    public Object getAttribute(String name) {
        return this.request.getAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return this.request.getAttributeNames();
    }

    public void removeAttribute(String name) {
        this.request.removeAttribute(name);
    }

    public void setAttribute(String name, Object o) {
        this.request.setAttribute(name, o);
    }

    public String getCharacterEncoding() {
        return this.request.getCharacterEncoding();
    }

    public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
        this.request.setCharacterEncoding(enc);
    }

    public int getContentLength() {
        return this.request.getContentLength();
    }

    public String getContentType() {
        return this.request.getContentType();
    }

    public Locale getLocale() {
        return this.request.getLocale();
    }

    public Enumeration getLocales() {
        return this.request.getLocales();
    }

    public ServletInputStream getInputStream() throws IOException {
        return this.request.getInputStream();
    }

    public BufferedReader getReader() throws IOException {
        return this.request.getReader();
    }

    public String getParameter(String name) {
        return this.request.getParameter(name);
    }

    public Map getParameterMap() {
        return this.request.getParameterMap();
    }

    public Enumeration getParameterNames() {
        return this.request.getParameterNames();
    }

    public String[] getParameterValues(String name) {
        return this.request.getParameterValues(name);
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return this.request.getRequestDispatcher(path);
    }

    public String getProtocol() {
        return this.request.getProtocol();
    }

    public String getRemoteAddr() {
        return this.request.getRemoteAddr();
    }

    public String getRemoteHost() {
        return this.request.getRemoteHost();
    }

    public String getScheme() {
        return this.request.getScheme();
    }

    public String getServerName() {
        return this.request.getServerName();
    }

    public int getServerPort() {
        return this.request.getServerPort();
    }

    public String getLocalAddr() {
        return this.request.getLocalAddr();
    }

    public String getLocalName() {
        return this.request.getLocalName();
    }

    public int getLocalPort() {
        return this.request.getLocalPort();
    }

    public int getRemotePort() {
        return this.request.getRemotePort();
    }

    public boolean isSecure() {
        return this.request.isSecure();
    }

    /**
     * @deprecated
     */
    public String getRealPath(String path) {
        return this.request.getRealPath(path);
    }
}
