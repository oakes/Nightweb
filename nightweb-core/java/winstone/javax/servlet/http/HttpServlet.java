/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Base class for http servlets
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public abstract class HttpServlet extends javax.servlet.GenericServlet
        implements Serializable {
    static final String METHOD_DELETE = "DELETE";
    static final String METHOD_HEAD = "HEAD";
    static final String METHOD_GET = "GET";
    static final String METHOD_OPTIONS = "OPTIONS";
    static final String METHOD_POST = "POST";
    static final String METHOD_PUT = "PUT";
    static final String METHOD_TRACE = "TRACE";
    static final String HEADER_IFMODSINCE = "If-Modified-Since";
    static final String HEADER_LASTMOD = "Last-Modified";

    public HttpServlet() {
        super();
    }

    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        if ((request instanceof HttpServletRequest)
                && (response instanceof HttpServletResponse))
            service((HttpServletRequest) request,
                    (HttpServletResponse) response);
        else
            throw new IllegalArgumentException(
                    "Not an Http servlet request - invalid types");
    }

    private void notAcceptedMethod(HttpServletRequest request,
            HttpServletResponse response, String method)
            throws ServletException, IOException {
        if (request.getProtocol().endsWith("1.1"))
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    method + " not allowed");
        else
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, method
                    + " not allowed");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        notAcceptedMethod(req, resp, "GET");
    }

    protected long getLastModified(HttpServletRequest req) {
        return -1;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        notAcceptedMethod(req, resp, "POST");
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        notAcceptedMethod(req, resp, "PUT");
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        notAcceptedMethod(req, resp, "DELETE");
    }

    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        notAcceptedMethod(req, resp, "OPTIONS");
    }

    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        notAcceptedMethod(req, resp, "TRACE");
    }

    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String method = request.getMethod();

        if (method.equals(METHOD_GET)) {
            long lastModified = getLastModified(request);
            if (lastModified == -1)
                doGet(request, response);
            else {
                long ifModifiedSince = request.getDateHeader(HEADER_IFMODSINCE);
                if (ifModifiedSince < (lastModified / 1000 * 1000)) {
                    if (!response.containsHeader(HEADER_LASTMOD)
                            && (lastModified >= 0))
                        response.setDateHeader(HEADER_LASTMOD, lastModified);
                    doGet(request, response);
                } else
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            }
        } else if (method.equals(METHOD_HEAD)) {
            long lastModified = getLastModified(request);
            if (!response.containsHeader(HEADER_LASTMOD) && (lastModified >= 0))
                response.setDateHeader(HEADER_LASTMOD, lastModified);
            doHead(request, response);
        } else if (method.equals(METHOD_POST))
            doPost(request, response);
        else if (method.equals(METHOD_PUT))
            doPut(request, response);
        else if (method.equals(METHOD_DELETE))
            doDelete(request, response);
        else if (method.equals(METHOD_OPTIONS))
            doOptions(request, response);
        else if (method.equals(METHOD_TRACE))
            doTrace(request, response);
        else
            notAcceptedMethod(request, response, method);
    }

    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        NoBodyResponse response = new NoBodyResponse(resp);
        doGet(req, response);
        response.setContentLength();
    }

    class NoBodyResponse extends HttpServletResponseWrapper {
        private NoBodyOutputStream noBody;

        private PrintWriter writer;

        private boolean contentLengthSet;

        NoBodyResponse(HttpServletResponse mainResponse) {
            super(mainResponse);
            this.noBody = new NoBodyOutputStream();
        }

        void setContentLength() {
            if (!contentLengthSet)
                setContentLength(this.noBody.getContentLength());
        }

        public void setContentLength(int length) {
            super.setContentLength(length);
            this.contentLengthSet = true;
        }

        public void setContentType(String type) {
            getResponse().setContentType(type);
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return noBody;
        }

        public String getCharacterEncoding() {
            return getResponse().getCharacterEncoding();
        }

        public PrintWriter getWriter() throws UnsupportedEncodingException {
            if (writer == null)
                writer = new PrintWriter(new OutputStreamWriter(noBody,
                        getCharacterEncoding()));
            return writer;
        }
    }

    class NoBodyOutputStream extends ServletOutputStream {
        private int contentLength = 0;

        NoBodyOutputStream() {
        }

        int getContentLength() {
            return contentLength;
        }

        public void write(int b) throws IOException {
            contentLength++;
        }

        public void write(byte buf[], int offset, int len) throws IOException {
            contentLength += len;
        }
    }
}
