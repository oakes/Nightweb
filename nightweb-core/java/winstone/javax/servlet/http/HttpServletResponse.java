/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.http;

import java.io.IOException;

/**
 * Interface definition for http response objects.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface HttpServletResponse extends javax.servlet.ServletResponse {
    public static final int SC_ACCEPTED = 202;
    public static final int SC_BAD_GATEWAY = 502;
    public static final int SC_BAD_REQUEST = 400;
    public static final int SC_CONFLICT = 409;
    public static final int SC_CONTINUE = 100;
    public static final int SC_CREATED = 201;
    public static final int SC_EXPECTATION_FAILED = 417;
    public static final int SC_FORBIDDEN = 403;
    public static final int SC_FOUND = 302;
    public static final int SC_GATEWAY_TIMEOUT = 504;
    public static final int SC_GONE = 410;
    public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
    public static final int SC_INTERNAL_SERVER_ERROR = 500;
    public static final int SC_LENGTH_REQUIRED = 411;
    public static final int SC_METHOD_NOT_ALLOWED = 405;
    public static final int SC_MOVED_PERMANENTLY = 301;
    public static final int SC_MOVED_TEMPORARILY = 302;
    public static final int SC_MULTIPLE_CHOICES = 300;
    public static final int SC_NO_CONTENT = 204;
    public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
    public static final int SC_NOT_ACCEPTABLE = 406;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_NOT_IMPLEMENTED = 501;
    public static final int SC_NOT_MODIFIED = 304;
    public static final int SC_OK = 200;
    public static final int SC_PARTIAL_CONTENT = 206;
    public static final int SC_PAYMENT_REQUIRED = 402;
    public static final int SC_PRECONDITION_FAILED = 412;
    public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
    public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int SC_REQUEST_TIMEOUT = 408;
    public static final int SC_REQUEST_URI_TOO_LONG = 414;
    public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    public static final int SC_RESET_CONTENT = 205;
    public static final int SC_SEE_OTHER = 303;
    public static final int SC_SERVICE_UNAVAILABLE = 503;
    public static final int SC_SWITCHING_PROTOCOLS = 101;
    public static final int SC_TEMPORARY_REDIRECT = 307;
    public static final int SC_UNAUTHORIZED = 401;
    public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int SC_USE_PROXY = 305;

    public void addCookie(Cookie cookie);

    public void addDateHeader(String name, long date);

    public void addHeader(String name, String value);

    public void addIntHeader(String name, int value);

    public boolean containsHeader(String name);

    public String encodeRedirectURL(String url);

    public String encodeURL(String url);

    public void sendError(int sc) throws IOException;

    public void sendError(int sc, String msg) throws IOException;

    public void sendRedirect(String location) throws IOException;

    public void setDateHeader(String name, long date);

    public void setHeader(String name, String value);

    public void setIntHeader(String name, int value);

    public void setStatus(int sc);

    /**
     * @deprecated As of version 2.1, due to ambiguous meaning of the message
     *             parameter. To set a status code use setStatus(int), to send
     *             an error with a description use sendError(int, String). Sets
     *             the status code and message for this response.
     */
    public void setStatus(int sc, String sm);

    /**
     * @deprecated As of version 2.1, use encodeRedirectURL(String url) instead
     */
    public String encodeRedirectUrl(String url);

    /**
     * @deprecated As of version 2.1, use encodeURL(String url) instead
     */
    public String encodeUrl(String url);
}
