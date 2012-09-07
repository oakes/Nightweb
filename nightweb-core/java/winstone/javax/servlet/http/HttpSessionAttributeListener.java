/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.http;

/**
 * Interface for session attribute listeners
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface HttpSessionAttributeListener extends java.util.EventListener {
    public void attributeAdded(HttpSessionBindingEvent se);

    public void attributeRemoved(HttpSessionBindingEvent se);

    public void attributeReplaced(HttpSessionBindingEvent se);
}
