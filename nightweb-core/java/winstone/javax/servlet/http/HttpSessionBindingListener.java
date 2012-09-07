/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet.http;

/**
 * Listener interface for listeners to the session binding events
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface HttpSessionBindingListener extends java.util.EventListener {
    public void valueBound(HttpSessionBindingEvent event);

    public void valueUnbound(HttpSessionBindingEvent event);
}
