/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public class ServletContextAttributeEvent extends ServletContextEvent {
    private String name;

    private Object value;

    public ServletContextAttributeEvent(ServletContext source, String name,
            Object value) {
        super(source);
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
