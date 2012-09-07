/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.util.EventListener;

/**
 * Interface defining request attribute listeners
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ServletRequestAttributeListener.java,v 1.2 2006/02/28 07:32:48 rickknowles Exp $
 */
public interface ServletRequestAttributeListener extends EventListener {
    public void attributeAdded(ServletRequestAttributeEvent srae);

    public void attributeRemoved(ServletRequestAttributeEvent srae);

    public void attributeReplaced(ServletRequestAttributeEvent srae);
}
