/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

/**
 * A simple servlet that writes out the body of the error 
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ErrorServlet.java,v 1.3 2006/02/28 07:32:47 rickknowles Exp $
 */
public class ErrorServlet extends HttpServlet {
    
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        
        Integer sc = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String msg = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Throwable err = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (err != null) {
            err.printStackTrace(pw);
        } else {
            pw.println("(none)");
        }
        pw.flush();
         
        // If we are here there was no error servlet, so show the default error page
        String output = Launcher.RESOURCES.getString("WinstoneResponse.ErrorPage",
                new String[] { sc + "", (msg == null ? "" : msg), sw.toString(),
                Launcher.RESOURCES.getString("ServerVersion"),
                        "" + new Date() });
        response.setContentLength(output.getBytes(response.getCharacterEncoding()).length);
        Writer out = response.getWriter();
        out.write(output);
        out.flush();
    }
}
