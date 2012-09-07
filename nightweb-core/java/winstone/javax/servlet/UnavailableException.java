/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

/**
 * Thrown if a servlet is permanently or temporarily unavailable
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: UnavailableException.java,v 1.2 2006/02/28 07:32:47 rickknowles Exp $
 */
public class UnavailableException extends ServletException {
    private int seconds;

    private Servlet servlet;

    /**
     * @deprecated As of Java Servlet API 2.2, use UnavailableException(String,
     *             int) instead.
     */
    public UnavailableException(int seconds, Servlet servlet, String msg) {
        this(servlet, msg);
        this.seconds = (seconds <= 0 ? 0 : seconds);
    }

    /**
     * @deprecated As of Java Servlet API 2.2, use UnavailableException(String)
     *             instead.
     */
    public UnavailableException(Servlet servlet, String msg) {
        this(msg);
        this.servlet = servlet;
    }

    /**
     * Constructs a new exception with a descriptive message indicating that the
     * servlet is permanently unavailable.
     */
    public UnavailableException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new exception with a descriptive message indicating that the
     * servlet is temporarily unavailable and giving an estimate of how long it
     * will be unavailable.
     */
    public UnavailableException(String msg, int seconds) {
        this(msg);
        this.seconds = (seconds <= 0 ? 0 : seconds);
    }

    /**
     * @deprecated As of Java Servlet API 2.2, with no replacement. Returns the
     *             servlet that is reporting its unavailability.
     */
    public Servlet getServlet() {
        return this.servlet;
    }

    /**
     * Returns the number of seconds the servlet expects to be temporarily
     * unavailable.
     */
    public int getUnavailableSeconds() {
        return this.seconds;
    }

    /**
     * Returns a boolean indicating whether the servlet is permanently
     * unavailable.
     */
    public boolean isPermanent() {
        return this.seconds <= 0;
    }

}
