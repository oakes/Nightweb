/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Master exception within the servlet container. This is thrown whenever a
 * non-recoverable error occurs that we want to throw to the top of the
 * application.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneException.java,v 1.1 2004/03/08 15:27:21 rickknowles
 *          Exp $
 */
public class WinstoneException extends RuntimeException {
    private Throwable nestedError = null;

    /**
     * Create an exception with a useful message for the system administrator.
     * 
     * @param pMsg
     *            Error message for to be used for administrative
     *            troubleshooting
     */
    public WinstoneException(String pMsg) {
        super(pMsg);
    }

    /**
     * Create an exception with a useful message for the system administrator
     * and a nested throwable object.
     * 
     * @param pMsg
     *            Error message for administrative troubleshooting
     * @param pError
     *            The actual exception that occurred
     */
    public WinstoneException(String pMsg, Throwable pError) {
        super(pMsg);
        this.setNestedError(pError);
    }

    /**
     * Get the nested error or exception
     * 
     * @return The nested error or exception
     */
    public Throwable getNestedError() {
        return this.nestedError;
    }

    /**
     * Set the nested error or exception
     * 
     * @param pError
     *            The nested error or exception
     */
    private void setNestedError(Throwable pError) {
        this.nestedError = pError;
    }

    public void printStackTrace(PrintWriter p) {
        if (this.nestedError != null)
            this.nestedError.printStackTrace(p);
        p.write("\n");
        super.printStackTrace(p);
    }

    public void printStackTrace(PrintStream p) {
        if (this.nestedError != null)
            this.nestedError.printStackTrace(p);
        p.println("\n");
        super.printStackTrace(p);
    }

    public void printStackTrace() {
        if (this.nestedError != null)
            this.nestedError.printStackTrace();
        super.printStackTrace();
    }
}
