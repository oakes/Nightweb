/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.accesslog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import winstone.AccessLogger;
import winstone.Logger;
import winstone.WebAppConfiguration;
import winstone.WinstoneRequest;
import winstone.WinstoneResourceBundle;
import winstone.WinstoneResponse;

/**
 * Simulates an apache "combined" style logger, which logs User-Agent, Referer, etc
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: SimpleAccessLogger.java,v 1.5 2006/03/24 17:24:19 rickknowles Exp $
 */
public class SimpleAccessLogger implements AccessLogger {

    public static final WinstoneResourceBundle ACCESSLOG_RESOURCES = 
            new WinstoneResourceBundle("winstone.accesslog.LocalStrings");
    
    private static final DateFormat DF = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
    private static final String COMMON = "###ip### - ###user### ###time### \"###uriLine###\" ###status### ###size###";
    private static final String COMBINED = COMMON + " \"###referer###\" \"###userAgent###\"";
    private static final String RESIN = COMMON + " \"###userAgent###\"";
    
//    private WebAppConfiguration webAppConfig;
    private OutputStream outStream;
    private PrintWriter outWriter;
    private String pattern;
    private String fileName;
    
    public SimpleAccessLogger(WebAppConfiguration webAppConfig, Map startupArgs) 
            throws IOException {
//        this.webAppConfig = webAppConfig;
        
        // Get pattern
        String patternType = WebAppConfiguration.stringArg(startupArgs, "simpleAccessLogger.format", "combined");
        if (patternType.equalsIgnoreCase("combined")) {
            this.pattern = COMBINED;
        } else if (patternType.equalsIgnoreCase("common")) {
            this.pattern = COMMON;
        } else if (patternType.equalsIgnoreCase("resin")) {
            this.pattern = RESIN; 
        } else {
            this.pattern = patternType;
        }
        
        // Get filename
        String filePattern = WebAppConfiguration.stringArg(startupArgs, "simpleAccessLogger.file", 
                "logs/###host###/###webapp###_access.log");
        this.fileName = WinstoneResourceBundle.globalReplace(filePattern, 
                new String [][] {{"###host###", webAppConfig.getOwnerHostname()},
                    {"###webapp###", webAppConfig.getContextName()}});
        
        File file = new File(this.fileName);
        file.getParentFile().mkdirs();
        this.outStream = new FileOutputStream(file, true);
        this.outWriter = new PrintWriter(this.outStream, true);
        
        Logger.log(Logger.DEBUG, ACCESSLOG_RESOURCES, "SimpleAccessLogger.Init", 
                new String[] {this.fileName, patternType});
    }
    
    public void log(String originalURL, WinstoneRequest request, WinstoneResponse response) {
        String uriLine = request.getMethod() + " " + originalURL + " " + request.getProtocol();
        int status = response.getErrorStatusCode() == null ? response.getStatus() 
                : response.getErrorStatusCode().intValue();
        int size = response.getWinstoneOutputStream().getBytesCommitted();
        String date = null;
        synchronized (DF) {
            date = DF.format(new Date());
        }
        String logLine = WinstoneResourceBundle.globalReplace(this.pattern, new String[][] {
                {"###ip###", request.getRemoteHost()},
                {"###user###", nvl(request.getRemoteUser())},
                {"###time###", "[" + date + "]"},
                {"###uriLine###", uriLine},
                {"###status###", "" + status},
                {"###size###", "" + size},
                {"###referer###", nvl(request.getHeader("Referer"))},
                {"###userAgent###", nvl(request.getHeader("User-Agent"))}
        });
        this.outWriter.println(logLine);
    }

    private static String nvl(String input) {
        return input == null ? "-" : input;
    }
    
    public void destroy() {
        Logger.log(Logger.DEBUG, ACCESSLOG_RESOURCES, "SimpleAccessLogger.Close", this.fileName);
        if (this.outWriter != null) {
            this.outWriter.flush();
            this.outWriter.close();
            this.outWriter = null;
        }
        if (this.outStream != null) {
            try {
                this.outStream.close();
            } catch (IOException err) {}
            this.outStream = null;
        }
        this.fileName = null;
//        this.webAppConfig = null;
    }
}
