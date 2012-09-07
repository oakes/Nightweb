/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

/**
 * The event thrown to request attribute listeners
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ServletRequestAttributeEvent.java,v 1.2 2006/02/28 07:32:47 rickknowles Exp $
 */
public class ServletRequestAttributeEvent extends ServletRequestEvent {
    private String name;

    private Object value;

    public ServletRequestAttributeEvent(ServletContext sc,
            ServletRequest request, String name, Object value) {
        super(sc, request);
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public Object getValue() {
        return this.value;
    }
}
