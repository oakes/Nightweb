/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.classLoader;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

import winstone.Logger;
import winstone.WinstoneResourceBundle;

/**
 * Implements the servlet spec model (v2.3 section 9.7.2) for classloading, which
 * is different to the standard JDK model in that it delegates *after* checking
 * local repositories. This has the effect of isolating copies of classes that exist
 * in 2 webapps from each other.
 * 
 * Thanks to James Berry for the changes to use the system classloader to prevent 
 * loading servlet spec or system classpath classes again.  
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WebappClassLoader.java,v 1.3 2007/12/29 03:32:54 rickknowles Exp $
 */
public class WebappClassLoader extends URLClassLoader {
    private static final WinstoneResourceBundle CL_RESOURCES = new WinstoneResourceBundle("winstone.classLoader.LocalStrings");
    
    protected ClassLoader system = getSystemClassLoader();

    public WebappClassLoader(URL[] urls) {
        super(urls);
    }

    public WebappClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    public WebappClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class c = findLoadedClass(name);
        
        // Try the system loader first, to ensure that system classes are not
        // overridden by webapps. Note that this includes any classes in winstone,
        // including the javax.servlet classes
        if (c == null) {
            try {
                c = system.loadClass(name);
                if (c != null) {
                    Logger.log(Logger.MAX, CL_RESOURCES, "WebappClassLoader.LoadedBySystemCL", name);
                }
            } catch (ClassNotFoundException e) {
                c = null;
            }
        }
        
        // If an allowed class, load it locally first
        if (c == null) {
            try {
                // If still not found, then invoke findClass in order to find the class.
                c = findClass(name);
                if (c != null) {
                    Logger.log(Logger.MAX, CL_RESOURCES, "WebappClassLoader.LoadedByThisCL", name);
                }
            } catch (ClassNotFoundException e) {
                c = null;
            }
        }
        
        // otherwise, and only if we have a parent, delegate to our parent
        // Note that within winstone, the only difference between this and the system
        // class loader we've already tried is that our parent might include the common/shared lib.
        if (c == null) {
            ClassLoader parent = getParent();
            if (parent != null) {
                c = parent.loadClass(name);
                if (c != null) {
                    Logger.log(Logger.MAX, CL_RESOURCES, "WebappClassLoader.LoadedByParentCL", name);
                }
            } else {
                // We have no other hope for loading the class, so throw the class not found exception
                throw new ClassNotFoundException(name);
            }
        }
        
        if (resolve && (c != null)) {
            resolveClass(c);
        }
        return c;
    }

    public InputStream getResourceAsStream(String name) {
        if ((name != null) && name.startsWith("/")) {
            name = name.substring(1);
        }
        return super.getResourceAsStream(name);
    }
}
