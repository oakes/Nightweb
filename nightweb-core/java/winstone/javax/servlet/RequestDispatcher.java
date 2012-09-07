/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

/**
 * Interface defining behaviour of servlet container dispatching of requests.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface RequestDispatcher {
    public void forward(ServletRequest request, ServletResponse response)
            throws ServletException, java.io.IOException;

    public void include(ServletRequest request, ServletResponse response)
            throws ServletException, java.io.IOException;
}
