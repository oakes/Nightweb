/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.tools;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

import winstone.Launcher;
import winstone.Logger;
import winstone.WebAppConfiguration;
import winstone.WinstoneResourceBundle;

/**
 * Included so that we can control winstone from the command line a little more
 * easily.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneControl.java,v 1.6 2006/03/13 15:37:29 rickknowles Exp $
 */
public class WinstoneControl {
    private final static WinstoneResourceBundle TOOLS_RESOURCES = new WinstoneResourceBundle("winstone.tools.LocalStrings");
    
    final static String OPERATION_SHUTDOWN = "shutdown";
    final static String OPERATION_RELOAD = "reload:";
    static int TIMEOUT = 10000;

    /**
     * Parses command line parameters, and calls the appropriate method for
     * executing the winstone operation required.
     */
    public static void main(String argv[]) throws Exception {

        // Load args from the config file
        Map options = Launcher.loadArgsFromCommandLineAndConfig(argv, "operation");
        String operation = (String) options.get("operation");
        if (options.containsKey("controlPort") && !options.containsKey("port")) {
            options.put("port", options.get("controlPort"));
        }

        if (operation.equals("")) {
            printUsage();
            return;
        }

        Logger.setCurrentDebugLevel(Integer.parseInt(WebAppConfiguration
                .stringArg(options, "debug", "5")));

        String host = WebAppConfiguration.stringArg(options, "host", "localhost");
        String port = WebAppConfiguration.stringArg(options, "port", "8081");

        Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.UsingHostPort",
                new String[] { host, port });

        // Check for shutdown
        if (operation.equalsIgnoreCase(OPERATION_SHUTDOWN)) {
            Socket socket = new Socket(host, Integer.parseInt(port));
            socket.setSoTimeout(TIMEOUT);
            OutputStream out = socket.getOutputStream();
            out.write(Launcher.SHUTDOWN_TYPE);
            out.close();
            Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.ShutdownOK",
                    new String[] { host, port });
        }

        // check for reload
        else if (operation.toLowerCase().startsWith(OPERATION_RELOAD.toLowerCase())) {
            String webappName = operation.substring(OPERATION_RELOAD.length());
            Socket socket = new Socket(host, Integer.parseInt(port));
            socket.setSoTimeout(TIMEOUT);
            OutputStream out = socket.getOutputStream();
            out.write(Launcher.RELOAD_TYPE);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeUTF(host);
            objOut.writeUTF(webappName);
            objOut.close();
            out.close();
            Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.ReloadOK",
                    new String[] { host, port });
        }
        else {
            printUsage();
        }
    }

    /**
     * Displays the usage message
     */
    private static void printUsage() throws IOException {
        System.out.println(TOOLS_RESOURCES.getString("WinstoneControl.Usage"));
    }
}
