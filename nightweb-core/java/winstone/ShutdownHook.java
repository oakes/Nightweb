/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

/**
 * A jvm hook to force the calling of the web-app destroy before the process terminates
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ShutdownHook.java,v 1.3 2006/02/28 07:32:47 rickknowles Exp $
 */
public class ShutdownHook extends Thread {
    private Launcher launcher;

    public ShutdownHook(Launcher launcher) {
        this.launcher = launcher;
    }

    public void run() {
        if (this.launcher != null)
            this.launcher.shutdown();
    }
}
