/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.http;

import java.util.Enumeration;
import java.security.Principal;

/**
 * Interface definition for http requests.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface HttpServletRequest extends javax.servlet.ServletRequest {
    public static final String BASIC_AUTH = "BASIC";
    public static final String CLIENT_CERT_AUTH = "CLIENT_CERT";
    public static final String DIGEST_AUTH = "DIGEST";
    public static final String FORM_AUTH = "FORM";

    public String getAuthType();

    public String getContextPath();

    public Cookie[] getCookies();

    public long getDateHeader(String name);

    public String getHeader(String name);

    public Enumeration getHeaderNames();

    public Enumeration getHeaders(String name);

    public int getIntHeader(String name);

    public String getMethod();

    public String getPathInfo();

    public String getPathTranslated();

    public String getQueryString();

    public String getRemoteUser();

    public String getRequestedSessionId();

    public String getRequestURI();

    public StringBuffer getRequestURL();

    public String getServletPath();

    public HttpSession getSession();

    public HttpSession getSession(boolean create);

    public Principal getUserPrincipal();

    public boolean isRequestedSessionIdFromCookie();

    public boolean isRequestedSessionIdFromURL();

    public boolean isRequestedSessionIdValid();

    public boolean isUserInRole(String role);

    /**
     * @deprecated As of Version 2.1 of the Java Servlet API, use
     *             isRequestedSessionIdFromURL() instead.
     */
    public boolean isRequestedSessionIdFromUrl();

}
