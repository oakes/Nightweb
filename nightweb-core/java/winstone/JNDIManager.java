/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

/**
 * Handles setup and teardown of the JNDI context
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: JNDIManager.java,v 1.2 2006/02/28 07:32:47 rickknowles Exp $
 */
public interface JNDIManager {
    /**
     * Add the objects passed to the constructor to the JNDI Context addresses
     * specified
     */
    public void setup();

    /**
     * Remove the objects under administration from the JNDI Context, and then
     * destroy the objects
     */
    public void tearDown();
}
