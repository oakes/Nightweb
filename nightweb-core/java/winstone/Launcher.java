/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implements the main launcher daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Launcher.java,v 1.29 2007/04/23 02:55:35 rickknowles Exp $
 */
public class Launcher implements Runnable {
    
    static final String HTTP_LISTENER_CLASS = "winstone.HttpListener";
    static final String HTTPS_LISTENER_CLASS = "winstone.ssl.HttpsListener";
    static final String AJP_LISTENER_CLASS = "winstone.ajp13.Ajp13Listener";
    static final String CLUSTER_CLASS = "winstone.cluster.SimpleCluster";
    static final String DEFAULT_JNDI_MGR_CLASS = "winstone.jndi.ContainerJNDIManager";

    public static final byte SHUTDOWN_TYPE = (byte) '0';
    public static final byte RELOAD_TYPE = (byte) '4';
    
    private int CONTROL_TIMEOUT = 2000; // wait 2s for control connection
    private int DEFAULT_CONTROL_PORT = -1;
    
    private Thread controlThread;
    public final static WinstoneResourceBundle RESOURCES = new WinstoneResourceBundle("winstone.LocalStrings");
    private int controlPort;
    private HostGroup hostGroup;
    private ObjectPool objectPool;
    private List listeners;
    private Map args;
    private Cluster cluster;
    private JNDIManager globalJndiManager;
    
    /**
     * Constructor - initialises the web app, object pools, control port and the
     * available protocol listeners.
     */
    public Launcher(Map args) throws IOException {
        
        boolean useJNDI = WebAppConfiguration.booleanArg(args, "useJNDI", false);
        
        // Set jndi resource handler if not set (workaround for JamVM bug)
        if (useJNDI) try {
            Class ctxFactoryClass = Class.forName("winstone.jndi.java.javaURLContextFactory");
            if (System.getProperty("java.naming.factory.initial") == null) {
                System.setProperty("java.naming.factory.initial", ctxFactoryClass.getName());
            }
            if (System.getProperty("java.naming.factory.url.pkgs") == null) {
                System.setProperty("java.naming.factory.url.pkgs", "winstone.jndi");
            }
        } catch (ClassNotFoundException err) {}
        
        Logger.log(Logger.MAX, RESOURCES, "Launcher.StartupArgs", args + "");
        
        this.args = args;
        this.controlPort = (args.get("controlPort") == null ? DEFAULT_CONTROL_PORT
                : Integer.parseInt((String) args.get("controlPort")));

        // Check for java home
        List jars = new ArrayList();
        List commonLibCLPaths = new ArrayList();
        String defaultJavaHome = System.getProperty("java.home"); 
        String javaHome = WebAppConfiguration.stringArg(args, "javaHome", defaultJavaHome);
        Logger.log(Logger.DEBUG, RESOURCES, "Launcher.UsingJavaHome", javaHome);
        String toolsJarLocation = WebAppConfiguration.stringArg(args, "toolsJar", null);
        File toolsJar = null;
        if (toolsJarLocation == null) {
            toolsJar = new File(javaHome, "lib/tools.jar");

            // first try - if it doesn't exist, try up one dir since we might have 
            // the JRE home by mistake
            if (!toolsJar.exists()) {
                File javaHome2 = new File(javaHome).getParentFile();
                File toolsJar2 = new File(javaHome2, "lib/tools.jar");
                if (toolsJar2.exists()) {
                    javaHome = javaHome2.getCanonicalPath();
                    toolsJar = toolsJar2;
                }
            }
        } else {
            toolsJar = new File(toolsJarLocation);
        }

        // Add tools jar to classloader path
        if (toolsJar.exists()) {
            jars.add(toolsJar.toURL());
            commonLibCLPaths.add(toolsJar);
            Logger.log(Logger.DEBUG, RESOURCES, "Launcher.AddedCommonLibJar",
                    toolsJar.getName());
        } else if (WebAppConfiguration.booleanArg(args, "useJasper", false))
            Logger.log(Logger.WARNING, RESOURCES, "Launcher.ToolsJarNotFound");

        // Set up common lib class loader
        String commonLibCLFolder = WebAppConfiguration.stringArg(args,
                "commonLibFolder", "lib");
        File libFolder = new File(commonLibCLFolder);
        if (libFolder.exists() && libFolder.isDirectory()) {
            Logger.log(Logger.DEBUG, RESOURCES, "Launcher.UsingCommonLib",
                    libFolder.getCanonicalPath());
            File children[] = libFolder.listFiles();
            for (int n = 0; n < children.length; n++)
                if (children[n].getName().endsWith(".jar")
                        || children[n].getName().endsWith(".zip")) {
                    jars.add(children[n].toURL());
                    commonLibCLPaths.add(children[n]);
                    Logger.log(Logger.DEBUG, RESOURCES, "Launcher.AddedCommonLibJar", 
                            children[n].getName());
                }
        } else {
            Logger.log(Logger.DEBUG, RESOURCES, "Launcher.NoCommonLib");
        }
        ClassLoader commonLibCL = new URLClassLoader((URL[]) jars.toArray(new URL[jars.size()]), 
                getClass().getClassLoader());
        
        Logger.log(Logger.MAX, RESOURCES, "Launcher.CLClassLoader",
                commonLibCL.toString());
        Logger.log(Logger.MAX, RESOURCES, "Launcher.CLClassLoader",
                commonLibCLPaths.toString());
                                        
        this.objectPool = new ObjectPool(args);

        // Optionally set up clustering if enabled and libraries are available
        String useCluster = (String) args.get("useCluster");
        boolean switchOnCluster = (useCluster != null)
                && (useCluster.equalsIgnoreCase("true") || useCluster
                        .equalsIgnoreCase("yes"));
        if (switchOnCluster) {
            if (this.controlPort < 0) {
                Logger.log(Logger.INFO, RESOURCES,
                        "Launcher.ClusterOffNoControlPort");
            } else {
                String clusterClassName = WebAppConfiguration.stringArg(args, "clusterClassName",
                        CLUSTER_CLASS).trim();
                try {
                    Class clusterClass = Class.forName(clusterClassName);
                    Constructor clusterConstructor = clusterClass
                            .getConstructor(new Class[] { Map.class, Integer.class });
                    this.cluster = (Cluster) clusterConstructor
                            .newInstance(new Object[] { args, new Integer(this.controlPort) });
                } catch (ClassNotFoundException err) {
                    Logger.log(Logger.DEBUG, RESOURCES, "Launcher.ClusterNotFound");
                } catch (Throwable err) {
                    Logger.log(Logger.WARNING, RESOURCES, "Launcher.ClusterStartupError", err);
                }
            }
        }
        
        // If jndi is enabled, run the container wide jndi populator
        if (useJNDI) {
            String jndiMgrClassName = WebAppConfiguration.stringArg(args, "containerJndiClassName",
                    DEFAULT_JNDI_MGR_CLASS).trim();
            try {
                // Build the realm
                Class jndiMgrClass = Class.forName(jndiMgrClassName, true, commonLibCL);
                Constructor jndiMgrConstr = jndiMgrClass.getConstructor(new Class[] { 
                        Map.class, List.class, ClassLoader.class });
                this.globalJndiManager = (JNDIManager) jndiMgrConstr.newInstance(new Object[] { 
                        args, null, commonLibCL });
                this.globalJndiManager.setup();
            } catch (ClassNotFoundException err) {
                Logger.log(Logger.DEBUG, RESOURCES,
                        "Launcher.JNDIDisabled");
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, RESOURCES,
                        "Launcher.JNDIError", jndiMgrClassName, err);
            }
        }
        
        // Open the web apps
        this.hostGroup = new HostGroup(this.cluster, this.objectPool, commonLibCL, 
                (File []) commonLibCLPaths.toArray(new File[0]), args);

        // Create connectors (http, https and ajp)
        this.listeners = new ArrayList();
        spawnListener(HTTP_LISTENER_CLASS);
        spawnListener(AJP_LISTENER_CLASS);
        try {
            Class.forName("javax.net.ServerSocketFactory");
            spawnListener(HTTPS_LISTENER_CLASS);
        } catch (ClassNotFoundException err) {
            Logger.log(Logger.DEBUG, RESOURCES, 
                    "Launcher.NeedsJDK14", HTTPS_LISTENER_CLASS);
        }

        this.controlThread = new Thread(this, RESOURCES.getString(
                "Launcher.ThreadName", "" + this.controlPort));
        this.controlThread.setDaemon(false);
        this.controlThread.start();

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

    }

    /**
     * Instantiates listeners. Note that an exception thrown in the 
     * constructor is interpreted as the listener being disabled, so 
     * don't do anything too adventurous in the constructor, or if you do, 
     * catch and log any errors locally before rethrowing.
     */
    protected void spawnListener(String listenerClassName) {
        try {
            Class listenerClass = Class.forName(listenerClassName);
            Constructor listenerConstructor = listenerClass
                    .getConstructor(new Class[] { Map.class,
                            ObjectPool.class, HostGroup.class});
            Listener listener = (Listener) listenerConstructor
                    .newInstance(new Object[] { args, this.objectPool, 
                            this.hostGroup });
            if (listener.start()) {
                this.listeners.add(listener);
            }
        } catch (ClassNotFoundException err) {
            Logger.log(Logger.INFO, RESOURCES, 
                    "Launcher.ListenerNotFound", listenerClassName);
        } catch (Throwable err) {
            Logger.log(Logger.ERROR, RESOURCES, 
                    "Launcher.ListenerStartupError", listenerClassName, err);
        }
    }

    /**
     * The main run method. This handles the normal thread processing.
     */
    public void run() {
        boolean interrupted = false;
        try {
            ServerSocket controlSocket = null;

            if (this.controlPort > 0) {
                controlSocket = new ServerSocket(this.controlPort);
                controlSocket.setSoTimeout(CONTROL_TIMEOUT);
            }

            Logger.log(Logger.INFO, RESOURCES, "Launcher.StartupOK",
                    new String[] {RESOURCES.getString("ServerVersion"),
                                    (this.controlPort > 0 ? "" + this.controlPort
                                            : RESOURCES.getString("Launcher.ControlDisabled"))});

            // Enter the main loop
            while (!interrupted) {
//                this.objectPool.removeUnusedRequestHandlers();
//                this.hostGroup.invalidateExpiredSessions();

                // Check for control request
                Socket accepted = null;
                try {
                    if (controlSocket != null) {
                        accepted = controlSocket.accept();
                        if (accepted != null) {
                            handleControlRequest(accepted);
                        }
                    } else {
                        Thread.sleep(CONTROL_TIMEOUT);
                    }
                } catch (InterruptedIOException err) {
                } catch (InterruptedException err) {
                    interrupted = true;
                } catch (Throwable err) {
                    Logger.log(Logger.ERROR, RESOURCES,
                            "Launcher.ShutdownError", err);
                } finally {
                    if (accepted != null) {
                        try {accepted.close();} catch (IOException err) {}
                    }
                    if (Thread.interrupted()) {
                        interrupted = true;
                    }
                }
            }

            // Close server socket
            if (controlSocket != null) {
                controlSocket.close();
            }
        } catch (Throwable err) {
            Logger.log(Logger.ERROR, RESOURCES, "Launcher.ShutdownError", err);
        }
        Logger.log(Logger.INFO, RESOURCES, "Launcher.ControlThreadShutdownOK");
    }

    protected void handleControlRequest(Socket csAccepted) throws IOException {
        InputStream inSocket = null;
        OutputStream outSocket = null;
        ObjectInputStream inControl = null;
        try {
            inSocket = csAccepted.getInputStream();
            int reqType = inSocket.read();
            if ((byte) reqType == SHUTDOWN_TYPE) {
                Logger.log(Logger.INFO, RESOURCES,
                        "Launcher.ShutdownRequestReceived");
                shutdown();
            } else if ((byte) reqType == RELOAD_TYPE) {
                inControl = new ObjectInputStream(inSocket);
                String host = inControl.readUTF();
                String prefix = inControl.readUTF();
                Logger.log(Logger.INFO, RESOURCES, "Launcher.ReloadRequestReceived", host + prefix);
                HostConfiguration hostConfig = this.hostGroup.getHostByName(host);
                hostConfig.reloadWebApp(prefix);
            } else if (this.cluster != null) {
                outSocket = csAccepted.getOutputStream();
                this.cluster.clusterRequest((byte) reqType,
                        inSocket, outSocket, csAccepted,
                        this.hostGroup);
            }
        } finally {
            if (inControl != null) {
                try {inControl.close();} catch (IOException err) {}
            }
            if (inSocket != null) {
                try {inSocket.close();} catch (IOException err) {}
            }
            if (outSocket != null) {
                try {outSocket.close();} catch (IOException err) {}
            }
        }
    }
    
    public void shutdown() {
        // Release all listeners/pools/webapps
        for (Iterator i = this.listeners.iterator(); i.hasNext();)
            ((Listener) i.next()).destroy();
        this.objectPool.destroy();
        if (this.cluster != null)
            this.cluster.destroy();
        this.hostGroup.destroy();
        if (this.globalJndiManager != null) {
            this.globalJndiManager.tearDown();
        }

        if (this.controlThread != null) {
            this.controlThread.interrupt();
        }
        Thread.yield();

        Logger.log(Logger.INFO, RESOURCES, "Launcher.ShutdownOK");
    }

    public boolean isRunning() {
        return (this.controlThread != null) && this.controlThread.isAlive();
    }
    
    /**
     * Main method. This basically just accepts a few args, then initialises the
     * listener thread. For now, just shut it down with a control-C.
     */
    public static void main(String argv[]) throws IOException {
        Map args = getArgsFromCommandLine(argv);
        
        if (args.containsKey("usage") || args.containsKey("help")) {
            printUsage();
            return;
        }

        // Check for embedded war
        deployEmbeddedWarfile(args);
        
        // Check for embedded warfile
        if (!args.containsKey("webroot") && !args.containsKey("warfile") 
                && !args.containsKey("webappsDir")&& !args.containsKey("hostsDir")) {
            printUsage();
            return;
        }
        // Launch
        try {
            new Launcher(args);
        } catch (Throwable err) {
            Logger.log(Logger.ERROR, RESOURCES, "Launcher.ContainerStartupError", err);
        }
    }
    
    public static Map getArgsFromCommandLine(String argv[]) throws IOException {
        Map args = loadArgsFromCommandLineAndConfig(argv, "nonSwitch");
        
        // Small hack to allow re-use of the command line parsing inside the control tool
        String firstNonSwitchArgument = (String) args.get("nonSwitch");
        args.remove("nonSwitch");
        
        // Check if the non-switch arg is a file or folder, and overwrite the config
        if (firstNonSwitchArgument != null) {
            File webapp = new File(firstNonSwitchArgument);
            if (webapp.exists()) {
                if (webapp.isDirectory()) {
                    args.put("webroot", firstNonSwitchArgument);
                } else if (webapp.isFile()) {
                    args.put("warfile", firstNonSwitchArgument);
                }
            }
        }
        return args;
    }

    public static Map loadArgsFromCommandLineAndConfig(String argv[], String nonSwitchArgName) 
            throws IOException {
        Map args = new HashMap();
        
        // Load embedded properties file 
        String embeddedPropertiesFilename = RESOURCES.getString(
                "Launcher.EmbeddedPropertiesFile");
        
        InputStream embeddedPropsStream = Launcher.class.getResourceAsStream(
                embeddedPropertiesFilename);
        if (embeddedPropsStream != null) {
            loadPropsFromStream(embeddedPropsStream, args);
            embeddedPropsStream.close();
        }
        
        // Get command line args
        String configFilename = RESOURCES.getString("Launcher.DefaultPropertyFile");
        for (int n = 0; n < argv.length; n++) {
            String option = argv[n];
            if (option.startsWith("--")) {
                int equalPos = option.indexOf('=');
                String paramName = option.substring(2, 
                        equalPos == -1 ? option.length() : equalPos);
                if (equalPos != -1) {
                    args.put(paramName, option.substring(equalPos + 1));
                } else {
                    args.put(paramName, "true");
                }
                if (paramName.equals("config")) {
                    configFilename = (String) args.get(paramName);
                }
            } else {
                args.put(nonSwitchArgName, option);
            }
        }

        // Load default props if available
        File configFile = new File(configFilename);
        if (configFile.exists() && configFile.isFile()) {
            InputStream inConfig = new FileInputStream(configFile);
            loadPropsFromStream(inConfig, args);
            inConfig.close();
            initLogger(args);
            Logger.log(Logger.DEBUG, RESOURCES, "Launcher.UsingPropertyFile",
                    configFilename);
        } else {
            initLogger(args);
        }
        return args;
    }
    
    protected static void deployEmbeddedWarfile(Map args) throws IOException {
        String embeddedWarfileName = RESOURCES.getString("Launcher.EmbeddedWarFile");
        InputStream embeddedWarfile = Launcher.class.getResourceAsStream(
                embeddedWarfileName);
        if (embeddedWarfile != null) {
            File tempWarfile = File.createTempFile("embedded", ".war").getAbsoluteFile();
            tempWarfile.getParentFile().mkdirs();
            tempWarfile.deleteOnExit();

            String embeddedWebroot = RESOURCES.getString("Launcher.EmbeddedWebroot");
            File tempWebroot = new File(tempWarfile.getParentFile(), embeddedWebroot);
            tempWebroot.mkdirs();
            
            Logger.log(Logger.DEBUG, RESOURCES, "Launcher.CopyingEmbeddedWarfile",
                    tempWarfile.getAbsolutePath());
            OutputStream out = new FileOutputStream(tempWarfile, true);
            int read = 0;
            byte buffer[] = new byte[2048];
            while ((read = embeddedWarfile.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.close();
            embeddedWarfile.close();
            
            args.put("warfile", tempWarfile.getAbsolutePath());
            args.put("webroot", tempWebroot.getAbsolutePath());
            args.remove("webappsDir");
            args.remove("hostsDir");
        }
    }
    
    protected static void loadPropsFromStream(InputStream inConfig, Map args) throws IOException {
        Properties props = new Properties();
        props.load(inConfig);
        for (Iterator i = props.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            if (!args.containsKey(key.trim())) {
                args.put(key.trim(), props.getProperty(key).trim());
            }
        }
        props.clear();
    }
    
    public static void initLogger(Map args) throws IOException {
        // Reset the log level
        int logLevel = WebAppConfiguration.intArg(args, "debug", Logger.INFO);
//        boolean showThrowingLineNo = WebAppConfiguration.booleanArg(args, "logThrowingLineNo", false);
        boolean showThrowingThread = WebAppConfiguration.booleanArg(args, "logThrowingThread", false);
        OutputStream logStream = null;
        if (args.get("logfile") != null) {
            logStream = new FileOutputStream((String) args.get("logfile"));
        } else if (WebAppConfiguration.booleanArg(args, "logToStdErr", false)) {
            logStream = System.err;
        } else {
            logStream = System.out;
        }
//        Logger.init(logLevel, logStream, showThrowingLineNo, showThrowingThread);
        Logger.init(logLevel, logStream, showThrowingThread);
    }

    protected static void printUsage() {
        System.out.println(RESOURCES.getString("Launcher.UsageInstructions",
                RESOURCES.getString("ServerVersion")));
    }
}
