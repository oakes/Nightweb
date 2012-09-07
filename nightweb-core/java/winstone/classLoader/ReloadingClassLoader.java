/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.classLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import winstone.Logger;
import winstone.WebAppConfiguration;
import winstone.WinstoneResourceBundle;

/**
 * This subclass of WinstoneClassLoader is the reloading version. It runs a
 * monitoring thread in the background that checks for updates to any files in
 * the class path.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ReloadingClassLoader.java,v 1.11 2007/02/17 01:55:12 rickknowles Exp $
 */
public class ReloadingClassLoader extends WebappClassLoader implements ServletContextListener, Runnable {
    private static final int RELOAD_SEARCH_SLEEP = 10;
    private static final WinstoneResourceBundle CL_RESOURCES = new WinstoneResourceBundle("winstone.classLoader.LocalStrings");
    private boolean interrupted;
    private WebAppConfiguration webAppConfig;
    private Set loadedClasses;
    private File classPaths[];
    private int classPathsLength;

    public ReloadingClassLoader(URL urls[], ClassLoader parent) {
        super(urls, parent);
        this.loadedClasses = new HashSet();
        if (urls != null) {
            this.classPaths = new File[urls.length];
            for (int n = 0 ; n < urls.length; n++) {
                this.classPaths[this.classPathsLength++] = new File(urls[n].getFile());
            }
        }
    }
    
    protected void addURL(URL url) {
        super.addURL(url);
        synchronized (this.loadedClasses) {
            if (this.classPaths == null) {
                this.classPaths = new File[10];
                this.classPathsLength = 0;
            } else if (this.classPathsLength == (this.classPaths.length - 1)) {
                File temp[] = this.classPaths;
                this.classPaths = new File[(int) (this.classPathsLength * 1.75)];
                System.arraycopy(temp, 0, this.classPaths, 0, this.classPathsLength);
            }
            this.classPaths[this.classPathsLength++] = new File(url.getFile());
        }
    }

    public void contextInitialized(ServletContextEvent sce) {
        this.webAppConfig = (WebAppConfiguration) sce.getServletContext();
        this.interrupted = false;
        synchronized (this) {
            this.loadedClasses.clear();
        }
        Thread thread = new Thread(this, CL_RESOURCES
                .getString("ReloadingClassLoader.ThreadName"));
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public void contextDestroyed(ServletContextEvent sce) {
        this.interrupted = true;
        this.webAppConfig = null;
        synchronized (this) {
            this.loadedClasses.clear();
        }
    }

    /**
     * The maintenance thread. This makes sure that any changes in the files in
     * the classpath trigger a classLoader self destruct and recreate.
     */
    public void run() {
        Logger.log(Logger.FULL_DEBUG, CL_RESOURCES,
                "ReloadingClassLoader.MaintenanceThreadStarted");

        Map classDateTable = new HashMap();
        Map classLocationTable = new HashMap();
        Set lostClasses = new HashSet();
        while (!interrupted) {
            try {
                String loadedClassesCopy[] = null;
                synchronized (this) {
                    loadedClassesCopy = (String []) this.loadedClasses.toArray(new String[0]);
                }

                for (int n = 0; (n < loadedClassesCopy.length) && !interrupted; n++) {
                    Thread.sleep(RELOAD_SEARCH_SLEEP);
                    String className = transformToFileFormat(loadedClassesCopy[n]);
                    File location = (File) classLocationTable.get(className);
                    Long classDate = null;
                    if ((location == null) || !location.exists()) {
                        for (int j = 0; (j < this.classPaths.length) && (classDate == null); j++) {
                            File path = this.classPaths[j];
                            if (!path.exists()) {
                                continue;
                            } else if (path.isDirectory()) {
                                File classLocation = new File(path, className);
                                if (classLocation.exists()) {
                                    classDate = new Long(classLocation.lastModified());
                                    classLocationTable.put(className, classLocation);
                                }
                            } else if (path.isFile()) {
                                classDate = searchJarPath(className, path);
                                if (classDate != null)
                                    classLocationTable.put(className, path);
                            }
                        }
                    } else if (location.exists())
                        classDate = new Long(location.lastModified());

                    // Has class vanished ? Leave a note and skip over it
                    if (classDate == null) {
                        if (!lostClasses.contains(className)) {
                            lostClasses.add(className);
                            Logger.log(Logger.DEBUG, CL_RESOURCES,
                                    "ReloadingClassLoader.ClassLost", className);
                        }
                        continue;
                    }
                    if ((classDate != null) && lostClasses.contains(className)) {
                        lostClasses.remove(className);
                    }

                    // Stash date of loaded files, and compare with last
                    // iteration
                    Long oldClassDate = (Long) classDateTable.get(className);
                    if (oldClassDate == null) {
                        classDateTable.put(className, classDate);
                    } else if (oldClassDate.compareTo(classDate) != 0) {
                        // Trigger reset of webAppConfig
                        Logger.log(Logger.INFO, CL_RESOURCES, 
                                "ReloadingClassLoader.ReloadRequired",
                                new String[] {className, 
                                        "" + new Date(classDate.longValue()),
                                        "" + new Date(oldClassDate.longValue()) });
                        this.webAppConfig.resetClassLoader();
                    }
                }
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, CL_RESOURCES,
                        "ReloadingClassLoader.MaintenanceThreadError", err);
            }
        }
        Logger.log(Logger.FULL_DEBUG, CL_RESOURCES,
                "ReloadingClassLoader.MaintenanceThreadFinished");
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        synchronized (this) {
            this.loadedClasses.add("Class:" + name);
        }
        return super.findClass(name);
    }

    public URL findResource(String name) {
        synchronized (this) {
            this.loadedClasses.add(name);
        }
        return super.findResource(name);
    }

    /**
     * Iterates through a jar file searching for a class. If found, it returns that classes date
     */
    private Long searchJarPath(String classResourceName, File path)
            throws IOException, InterruptedException {
        JarFile jar = new JarFile(path);
        for (Enumeration e = jar.entries(); e.hasMoreElements() && !interrupted;) {
            JarEntry entry = (JarEntry) e.nextElement();
            if (entry.getName().equals(classResourceName))
                return new Long(path.lastModified());
        }
        return null;
    }

    private static String transformToFileFormat(String name) {
        if (!name.startsWith("Class:"))
            return name;
        else
            return WinstoneResourceBundle.globalReplace(name.substring(6), ".", "/") + ".class";
    }
}
