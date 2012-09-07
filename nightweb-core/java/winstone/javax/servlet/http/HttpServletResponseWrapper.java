/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.http;

import java.io.IOException;

/**
 * Wraps HttpServletResponse objects in a decorator pattern
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class HttpServletResponseWrapper extends
        javax.servlet.ServletResponseWrapper implements HttpServletResponse {
    private HttpServletResponse httpResponse;

    public HttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
        this.httpResponse = response;
    }

    public void setResponse(javax.servlet.ServletResponse response) {
        if (response instanceof HttpServletResponse) {
            super.setResponse(response);
            this.httpResponse = (HttpServletResponse) response;
        } else
            throw new IllegalArgumentException("Not an HttpServletResponse");
    }

    public void addCookie(Cookie cookie) {
        this.httpResponse.addCookie(cookie);
    }

    public void addDateHeader(String name, long date) {
        this.httpResponse.addDateHeader(name, date);
    }

    public void addHeader(String name, String value) {
        this.httpResponse.addHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
        this.httpResponse.addIntHeader(name, value);
    }

    public boolean containsHeader(String name) {
        return this.httpResponse.containsHeader(name);
    }

    public String encodeRedirectURL(String url) {
        return this.httpResponse.encodeRedirectURL(url);
    }

    public String encodeURL(String url) {
        return this.httpResponse.encodeURL(url);
    }

    public void sendError(int sc) throws IOException {
        this.httpResponse.sendError(sc);
    }

    public void sendError(int sc, String msg) throws IOException {
        this.httpResponse.sendError(sc, msg);
    }

    public void sendRedirect(String location) throws IOException {
        this.httpResponse.sendRedirect(location);
    }

    public void setDateHeader(String name, long date) {
        this.httpResponse.setDateHeader(name, date);
    }

    public void setHeader(String name, String value) {
        this.httpResponse.setHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        this.httpResponse.setIntHeader(name, value);
    }

    public void setStatus(int sc) {
        this.httpResponse.setStatus(sc);
    }

    /**
     * @deprecated
     */
    public String encodeRedirectUrl(String url) {
        return this.httpResponse.encodeRedirectUrl(url);
    }

    /**
     * @deprecated
     */
    public String encodeUrl(String url) {
        return this.httpResponse.encodeUrl(url);
    }

    /**
     * @deprecated
     */
    public void setStatus(int sc, String sm) {
        this.httpResponse.setStatus(sc, sm);
    }

}
