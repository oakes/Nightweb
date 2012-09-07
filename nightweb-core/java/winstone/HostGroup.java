/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Manages the references to individual hosts within the container. This object handles
 * the mapping of ip addresses and hostnames to groups of webapps, and init and 
 * shutdown of any hosts it manages.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostGroup.java,v 1.4 2006/03/24 17:24:21 rickknowles Exp $
 */
public class HostGroup {
    
    private final static String DEFAULT_HOSTNAME = "default";
    
//    private Map args;
    private Map hostConfigs;
    private String defaultHostName;
    
    public HostGroup(Cluster cluster,
            ObjectPool objectPool, ClassLoader commonLibCL, 
            File commonLibCLPaths[], Map args) throws IOException {
//        this.args = args;
        this.hostConfigs = new Hashtable();
        
        // Is this the single or multiple configuration ? Check args
        String hostDirName = (String) args.get("hostsDir");
        String webappsDirName = (String) args.get("webappsDir");

        // If host mode
        if (hostDirName == null) {
            initHost(webappsDirName, DEFAULT_HOSTNAME, cluster, objectPool, commonLibCL, 
                    commonLibCLPaths, args);
            this.defaultHostName = DEFAULT_HOSTNAME;
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostGroup.InitSingleComplete", 
                    new String[] {this.hostConfigs.size() + "", this.hostConfigs.keySet() + ""});
        }
        // Otherwise multi-webapp mode
        else {
            initMultiHostDir(hostDirName, cluster, objectPool, commonLibCL, 
                    commonLibCLPaths, args);
            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostGroup.InitMultiComplete", 
                    new String[] {this.hostConfigs.size() + "", this.hostConfigs.keySet() + ""});
        }
    }

    public HostConfiguration getHostByName(String hostname) {
        if ((hostname != null) && (this.hostConfigs.size() > 1)) {
            HostConfiguration host = (HostConfiguration) this.hostConfigs.get(hostname);
            if (host != null) {
                return host;
            }
        }
        return (HostConfiguration) this.hostConfigs.get(this.defaultHostName);
    }
    
    public void destroy() {
        Set hostnames = new HashSet(this.hostConfigs.keySet());
        for (Iterator i = hostnames.iterator(); i.hasNext(); ) {
            String hostname = (String) i.next();
            HostConfiguration host = (HostConfiguration) this.hostConfigs.get(hostname);
            host.destroy();
            this.hostConfigs.remove(hostname);
        }
        this.hostConfigs.clear();
    }
    
    protected void initHost(String webappsDirName, String hostname, Cluster cluster, 
            ObjectPool objectPool, ClassLoader commonLibCL, 
            File commonLibCLPaths[], Map args) throws IOException {
        Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostGroup.DeployingHost", hostname);
        HostConfiguration config = new HostConfiguration(hostname, cluster, objectPool, commonLibCL, 
                commonLibCLPaths, args, webappsDirName);
        this.hostConfigs.put(hostname, config);
    }
    
    protected void initMultiHostDir(String hostsDirName, Cluster cluster,
            ObjectPool objectPool, ClassLoader commonLibCL, 
            File commonLibCLPaths[], Map args) throws IOException {
        if (hostsDirName == null) {
            hostsDirName = "hosts";
        }
        File hostsDir = new File(hostsDirName);
        if (!hostsDir.exists()) {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostGroup.HostsDirNotFound", hostsDirName));
        } else if (!hostsDir.isDirectory()) {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostGroup.HostsDirIsNotDirectory", hostsDirName));
        } else {
            File children[] = hostsDir.listFiles();
            if ((children == null) || (children.length == 0)) {
                throw new WinstoneException(Launcher.RESOURCES.getString("HostGroup.HostsDirIsEmpty", hostsDirName));
            }
            for (int n = 0; n < children.length; n++) {
                String childName = children[n].getName();

                // Mount directories as host dirs
                if (children[n].isDirectory()) {
                    if (!this.hostConfigs.containsKey(childName)) {
                        initHost(children[n].getCanonicalPath(), childName, cluster, 
                                objectPool, commonLibCL, commonLibCLPaths, args);
                    }
                }
                if ((defaultHostName == null) || childName.equals(DEFAULT_HOSTNAME)) {
                    this.defaultHostName = childName;
                }
            }
        }
    }
}
