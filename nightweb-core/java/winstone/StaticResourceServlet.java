/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to handle static resources. Simply finds and sends them, or
 * dispatches to the error servlet.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: StaticResourceServlet.java,v 1.17 2004/12/31 07:21:00
 *          rickknowles Exp $
 */
public class StaticResourceServlet extends HttpServlet {
    // final String JSP_FILE = "org.apache.catalina.jsp_file";
    final static String FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
    final static String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
    final static String CACHED_RESOURCE_DATE_HEADER = "If-Modified-Since";
    final static String LAST_MODIFIED_DATE_HEADER = "Last-Modified";
    final static String RANGE_HEADER = "Range";
    final static String ACCEPT_RANGES_HEADER = "Accept-Ranges";
    final static String CONTENT_RANGE_HEADER = "Content-Range";
    final static String RESOURCE_FILE = "winstone.LocalStrings";
    private DateFormat sdfFileDate = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private File webRoot;
    private String prefix;
    private boolean directoryList;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.webRoot = new File(config.getInitParameter("webRoot"));
        this.prefix = config.getInitParameter("prefix");
        String dirList = config.getInitParameter("directoryList");
        this.directoryList = (dirList == null)
                || dirList.equalsIgnoreCase("true")
                || dirList.equalsIgnoreCase("yes");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean isInclude = (request.getAttribute(INCLUDE_SERVLET_PATH) != null);
        boolean isForward = (request.getAttribute(FORWARD_SERVLET_PATH) != null);
        String path = null;

        if (isInclude)
            path = (String) request.getAttribute(INCLUDE_SERVLET_PATH);
        else  {
            path = request.getServletPath();
        }

        // URL decode path
        path = WinstoneRequest.decodeURLToken(path);

        long cachedResDate = request.getDateHeader(CACHED_RESOURCE_DATE_HEADER);
        Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                "StaticResourceServlet.PathRequested", new String[] {
                        getServletConfig().getServletName(), path });

        // Check for the resource
        File res = path.equals("") ? this.webRoot : new File(
                this.webRoot, path);

        // Send a 404 if not found
        if (!res.exists())
            response.sendError(HttpServletResponse.SC_NOT_FOUND, Launcher.RESOURCES
                    .getString("StaticResourceServlet.PathNotFound", path));

        // Check we are below the webroot
        else if (!isDescendant(this.webRoot, res, this.webRoot)) {
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "StaticResourceServlet.OutsideWebroot",
                    new String[] {res.getCanonicalPath(), this.webRoot.toString()});
            response.sendError(HttpServletResponse.SC_FORBIDDEN, Launcher.RESOURCES
                    .getString("StaticResourceServlet.PathInvalid", path));
        }

        // Check we are not below the web-inf
        else if (!isInclude && !isForward && isDescendant(new File(this.webRoot, "WEB-INF"), res, this.webRoot)) 
            response.sendError(HttpServletResponse.SC_NOT_FOUND, Launcher.RESOURCES
                    .getString("StaticResourceServlet.PathInvalid", path));

        // Check we are not below the meta-inf
        else if (!isInclude && !isForward && isDescendant(new File(this.webRoot, "META-INF"), res, this.webRoot)) 
            response.sendError(HttpServletResponse.SC_NOT_FOUND, Launcher.RESOURCES
                    .getString("StaticResourceServlet.PathInvalid", path));

        // check for the directory case
        else if (res.isDirectory()) {
            if (path.endsWith("/")) {
                // Try to match each of the welcome files
                // String matchedWelcome = matchWelcomeFiles(path, res);
                // if (matchedWelcome != null)
                // response.sendRedirect(this.prefix + path + matchedWelcome);
                // else
                if (this.directoryList)
                    generateDirectoryList(request, response, path);
                else
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            Launcher.RESOURCES.getString("StaticResourceServlet.AccessDenied"));
            } else
                response.sendRedirect(this.prefix + path + "/");
        }

        // Send a 304 if not modified
        else if (!isInclude && (cachedResDate != -1)
                && (cachedResDate < (System.currentTimeMillis() / 1000L * 1000L))
                && (cachedResDate >= (res.lastModified() / 1000L * 1000L))) {
            String mimeType = getServletContext().getMimeType(
                    res.getName().toLowerCase());
            if (mimeType != null)
                response.setContentType(mimeType);
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setContentLength(0);
            response.flushBuffer();
        }

        // Write out the resource if not range or is included
        else if ((request.getHeader(RANGE_HEADER) == null) || isInclude) {
            String mimeType = getServletContext().getMimeType(
                    res.getName().toLowerCase());
            if (mimeType != null)
                response.setContentType(mimeType);
            InputStream resStream = new FileInputStream(res);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength((int) res.length());
//            response.addHeader(ACCEPT_RANGES_HEADER, "bytes");
            response.addDateHeader(LAST_MODIFIED_DATE_HEADER, res.lastModified());
            OutputStream out = null;
            Writer outWriter = null;
            try {
                out = response.getOutputStream();
            } catch (IllegalStateException err) {
                outWriter = response.getWriter();
            } catch (IllegalArgumentException err) {
                outWriter = response.getWriter();
            }
            byte buffer[] = new byte[4096];
            int read = resStream.read(buffer);
            while (read > 0) {
                if (out != null) {
                    out.write(buffer, 0, read);
                } else {
                    outWriter.write(new String(buffer, 0, read, 
                            response.getCharacterEncoding()));
                }
                read = resStream.read(buffer);
            }
            resStream.close();
        } else if (request.getHeader(RANGE_HEADER).startsWith("bytes=")) {
            String mimeType = getServletContext().getMimeType(
                    res.getName().toLowerCase());
            if (mimeType != null)
                response.setContentType(mimeType);
            InputStream resStream = new FileInputStream(res);

            List ranges = new ArrayList();
            StringTokenizer st = new StringTokenizer(request.getHeader(
                    RANGE_HEADER).substring(6).trim(), ",", false);
            int totalSent = 0;
            String rangeText = "";
            while (st.hasMoreTokens()) {
                String rangeBlock = st.nextToken();
                int start = 0;
                int end = (int) res.length();
                int delim = rangeBlock.indexOf('-');
                if (delim != 0)
                    start = Integer.parseInt(rangeBlock.substring(0, delim)
                            .trim());
                if (delim != rangeBlock.length() - 1)
                    end = Integer.parseInt(rangeBlock.substring(delim + 1)
                            .trim());
                totalSent += (end - start);
                rangeText += "," + start + "-" + end;
                ranges.add(start + "-" + end);
            }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.addHeader(CONTENT_RANGE_HEADER, "bytes "
                    + rangeText.substring(1) + "/" + res.length());
            response.setContentLength(totalSent);

            response.addHeader(ACCEPT_RANGES_HEADER, "bytes");
            response.addDateHeader(LAST_MODIFIED_DATE_HEADER, res
                    .lastModified());
            OutputStream out = response.getOutputStream();
            int bytesRead = 0;
            for (Iterator i = ranges.iterator(); i.hasNext();) {
                String rangeBlock = (String) i.next();
                int delim = rangeBlock.indexOf('-');
                int start = Integer.parseInt(rangeBlock.substring(0, delim));
                int end = Integer.parseInt(rangeBlock.substring(delim + 1));
                int read = 0;
                while ((read != -1) && (bytesRead <= res.length())) {
                    read = resStream.read();
                    if ((bytesRead >= start) && (bytesRead < end))
                        out.write(read);
                    bytesRead++;
                }
            }
            resStream.close();
        } else
            response
                    .sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    /**
     * Generate a list of the files in this directory
     */
    private void generateDirectoryList(HttpServletRequest request,
            HttpServletResponse response, String path) throws ServletException,
            IOException {
        // Get the file list
        File dir = path.equals("") ? this.webRoot : new File(
                this.webRoot, path);
        File children[] = dir.listFiles();
        Arrays.sort(children);

        // Build row content
        StringWriter rowString = new StringWriter();
        String oddColour = Launcher.RESOURCES
                .getString("StaticResourceServlet.DirectoryList.OddColour");
        String evenColour = Launcher.RESOURCES
                .getString("StaticResourceServlet.DirectoryList.EvenColour");
        String rowTextColour = Launcher.RESOURCES
                .getString("StaticResourceServlet.DirectoryList.RowTextColour");

        String directoryLabel = Launcher.RESOURCES
                .getString("StaticResourceServlet.DirectoryList.DirectoryLabel");
        String parentDirLabel = Launcher.RESOURCES
                .getString("StaticResourceServlet.DirectoryList.ParentDirectoryLabel");
        String noDateLabel = Launcher.RESOURCES
                .getString("StaticResourceServlet.DirectoryList.NoDateLabel");

        int rowCount = 0;

        // Write the parent dir row
        if (!path.equals("") && !path.equals("/")) {
            rowString.write(Launcher.RESOURCES.getString(
                    "StaticResourceServlet.DirectoryList.Row", new String[] {
                            rowTextColour, evenColour, parentDirLabel, "..",
                            noDateLabel, directoryLabel }));
            rowCount++;
        }

        // Write the rows for each file
        for (int n = 0; n < children.length; n++) {
            if (!children[n].getName().equalsIgnoreCase("web-inf") && 
                    !children[n].getName().equalsIgnoreCase("meta-inf")) {
                File file = children[n];
                String date = noDateLabel;
                String size = directoryLabel;
                if (!file.isDirectory()) {
                    size = "" + file.length();
                    synchronized (sdfFileDate) {
                        date = sdfFileDate.format(new Date(file.lastModified()));
                    }
                }
                rowString.write(Launcher.RESOURCES.getString(
                        "StaticResourceServlet.DirectoryList.Row",
                        new String[] {
                                rowTextColour,
                                rowCount % 2 == 0 ? evenColour : oddColour,
                                file.getName() + (file.isDirectory() ? "/" : ""),
                                "./" + file.getName() + (file.isDirectory() ? "/" : ""),
                                date, size}));
                rowCount++;
            }
        }
        
        // Build wrapper body
        String out = Launcher.RESOURCES.getString("StaticResourceServlet.DirectoryList.Body",
                new String[] {
                        Launcher.RESOURCES.getString("StaticResourceServlet.DirectoryList.HeaderColour"),
                        Launcher.RESOURCES.getString("StaticResourceServlet.DirectoryList.HeaderTextColour"),
                        Launcher.RESOURCES.getString("StaticResourceServlet.DirectoryList.LabelColour"),
                        Launcher.RESOURCES.getString("StaticResourceServlet.DirectoryList.LabelTextColour"),
                        new Date() + "",
                        Launcher.RESOURCES.getString("ServerVersion"),
                        path.equals("") ? "/" : path,
                        rowString.toString() });

        response.setContentLength(out.getBytes().length);
        response.setContentType("text/html");
        Writer w = response.getWriter();
        w.write(out);
        w.close();
    }
    
    public static boolean isDescendant(File parent, File child, File commonBase) throws IOException {
        if (child.equals(parent)) {
            return true;
        } else {
            // Start by checking canonicals
            String canonicalParent = parent.getAbsoluteFile().getCanonicalPath();
            String canonicalChild = child.getAbsoluteFile().getCanonicalPath();
            if (canonicalChild.startsWith(canonicalParent)) {
                return true;
            }
            
            // If canonicals don't match, we're dealing with symlinked files, so if we can
            // build a path from the parent to the child, 
            String childOCValue = constructOurCanonicalVersion(child, commonBase);
            String parentOCValue = constructOurCanonicalVersion(parent, commonBase);
            return childOCValue.startsWith(parentOCValue);
        }
    }
    
    public static String constructOurCanonicalVersion(File current, File stopPoint) {
        int backOnes = 0;
        StringBuffer ourCanonicalVersion = new StringBuffer();
        while ((current != null) && !current.equals(stopPoint)) {
            if (current.getName().equals("..")) {
                backOnes++;
            } else if (current.getName().equals(".")) {
                // skip - do nothing
            } else if (backOnes > 0) {
                backOnes--;
            } else {
                ourCanonicalVersion.insert(0, "/" + current.getName());
            }
            current = current.getParentFile();
        }
        return ourCanonicalVersion.toString();
    }
}
