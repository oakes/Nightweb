/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.util.EventObject;

/**
 * Request coming into scope or out of scope event
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ServletRequestEvent.java,v 1.2 2006/02/28 07:32:47 rickknowles Exp $
 */
public class ServletRequestEvent extends EventObject {
    private ServletRequest request;

    private ServletContext context;

    public ServletRequestEvent(ServletContext sc, ServletRequest request) {
        super(sc);
        this.request = request;
        this.context = sc;
    }

    public ServletRequest getServletRequest() {
        return this.request;
    }

    public ServletContext getServletContext() {
        return this.context;
    }
}
