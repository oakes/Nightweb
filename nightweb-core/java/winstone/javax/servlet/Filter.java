/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

/**
 * Interface definition for filter objects
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface Filter {
    public void destroy();

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws java.io.IOException, ServletException;

    public void init(FilterConfig filterConfig) throws ServletException;
}
