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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Http session implementation for Winstone.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneSession.java,v 1.10 2006/08/27 07:19:47 rickknowles Exp $
 */
public class WinstoneSession implements HttpSession, Serializable {
    public static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private String sessionId;
    private WebAppConfiguration webAppConfig;
    private Map sessionData;
    private long createTime;
    private long lastAccessedTime;
    private int maxInactivePeriod;
    private boolean isNew;
    private boolean isInvalidated;
    private HttpSessionAttributeListener sessionAttributeListeners[];
    private HttpSessionListener sessionListeners[];
    private HttpSessionActivationListener sessionActivationListeners[];
    private boolean distributable;
    private Object sessionMonitor = new Boolean(true);
    private Set requestsUsingMe;

    /**
     * Constructor
     */
    public WinstoneSession(String sessionId) {
        this.sessionId = sessionId;
        this.sessionData = new HashMap();
        this.requestsUsingMe = new HashSet();
        this.createTime = System.currentTimeMillis();
        this.isNew = true;
        this.isInvalidated = false;
    }

    public void setWebAppConfiguration(WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
        this.distributable = webAppConfig.isDistributable();
    }
    
    public void sendCreatedNotifies() {
        // Notify session listeners of new session
        for (int n = 0; n < this.sessionListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionListeners[n].sessionCreated(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void setSessionActivationListeners(
            HttpSessionActivationListener listeners[]) {
        this.sessionActivationListeners = listeners;
    }

    public void setSessionAttributeListeners(
            HttpSessionAttributeListener listeners[]) {
        this.sessionAttributeListeners = listeners;
    }

    public void setSessionListeners(HttpSessionListener listeners[]) {
        this.sessionListeners = listeners;
    }

    public void setLastAccessedDate(long time) {
        this.lastAccessedTime = time;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }
    
    public void addUsed(WinstoneRequest request) {
        this.requestsUsingMe.add(request);
    }
    
    public void removeUsed(WinstoneRequest request) {
        this.requestsUsingMe.remove(request);
    }
    
    public boolean isUnusedByRequests() {
        return this.requestsUsingMe.isEmpty();
    }
    
    public boolean isExpired() {
        // check if it's expired yet
        long nowDate = System.currentTimeMillis();
        long maxInactive = getMaxInactiveInterval() * 1000;
        return ((maxInactive > 0) && (nowDate - this.lastAccessedTime > maxInactive ));
    }

    // Implementation methods
    public Object getAttribute(String name) {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        Object att = null;
        synchronized (this.sessionMonitor) {
            att = this.sessionData.get(name);
        }
        return att;
    }

    public Enumeration getAttributeNames() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        Enumeration names = null;
        synchronized (this.sessionMonitor) {
            names = Collections.enumeration(this.sessionData.keySet());
        }
        return names;
    }

    public void setAttribute(String name, Object value) {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        // Check for serializability if distributable
        if (this.distributable && (value != null)
                && !(value instanceof java.io.Serializable))
            throw new IllegalArgumentException(Launcher.RESOURCES.getString(
                    "WinstoneSession.AttributeNotSerializable", new String[] {
                            name, value.getClass().getName() }));

        // valueBound must be before binding
        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueBound(new HttpSessionBindingEvent(this, name, value));
            Thread.currentThread().setContextClassLoader(cl);
        }

        Object oldValue = null;
        synchronized (this.sessionMonitor) {
            oldValue = this.sessionData.get(name);
            if (value == null) {
                this.sessionData.remove(name);
            } else {
                this.sessionData.put(name, value);
            }
        }

        // valueUnbound must be after unbinding
        if (oldValue instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) oldValue;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueUnbound(new HttpSessionBindingEvent(this, name, oldValue));
            Thread.currentThread().setContextClassLoader(cl);
        }

        // Notify other listeners
        if (oldValue != null)
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeReplaced(
                        new HttpSessionBindingEvent(this, name, oldValue));
                Thread.currentThread().setContextClassLoader(cl);
            }
                
        else
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeAdded(
                        new HttpSessionBindingEvent(this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
                
            }
    }

    public void removeAttribute(String name) {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        Object value = null;
        synchronized (this.sessionMonitor) {
            value = this.sessionData.get(name);
            this.sessionData.remove(name);
        }

        // Notify listeners
        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener hsbl = (HttpSessionBindingListener) value;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            hsbl.valueUnbound(new HttpSessionBindingEvent(this, name));
            Thread.currentThread().setContextClassLoader(cl);
        }
        if (value != null)
            for (int n = 0; n < this.sessionAttributeListeners.length; n++) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
                this.sessionAttributeListeners[n].attributeRemoved(
                        new HttpSessionBindingEvent(this, name, value));
                Thread.currentThread().setContextClassLoader(cl);
            }
    }

    public long getCreationTime() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        return this.createTime;
    }

    public long getLastAccessedTime() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        return this.lastAccessedTime;
    }

    public String getId() {
        return this.sessionId;
    }

    public int getMaxInactiveInterval() {
        return this.maxInactivePeriod;
    }

    public void setMaxInactiveInterval(int interval) {
        this.maxInactivePeriod = interval;
    }

    public boolean isNew() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        return this.isNew;
    }

    public ServletContext getServletContext() {
        return this.webAppConfig;
    }

    public void invalidate() {
        if (this.isInvalidated) {
            throw new IllegalStateException(Launcher.RESOURCES.getString("WinstoneSession.InvalidatedSession"));
        }
        // Notify session listeners of invalidated session -- backwards
        for (int n = this.sessionListeners.length - 1; n >= 0; n--) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionListeners[n].sessionDestroyed(new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }

        List keys = new ArrayList(this.sessionData.keySet());
        for (Iterator i = keys.iterator(); i.hasNext();)
            removeAttribute((String) i.next());
        synchronized (this.sessionMonitor) {
            this.sessionData.clear();
        }
        this.isInvalidated = true;
        this.webAppConfig.removeSessionById(this.sessionId);
    }

    /**
     * Called after the session has been serialized to another server.
     */
    public void passivate() {
        // Notify session listeners of invalidated session
        for (int n = 0; n < this.sessionActivationListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionActivationListeners[n].sessionWillPassivate(
                    new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }

        // Question: Is passivation equivalent to invalidation ? Should all
        // entries be removed ?
        // List keys = new ArrayList(this.sessionData.keySet());
        // for (Iterator i = keys.iterator(); i.hasNext(); )
        // removeAttribute((String) i.next());
        synchronized (this.sessionMonitor) {
            this.sessionData.clear();
        }
        this.webAppConfig.removeSessionById(this.sessionId);
    }

    /**
     * Called after the session has been deserialized from another server.
     */
    public void activate(WebAppConfiguration webAppConfig) {
        this.webAppConfig = webAppConfig;
        webAppConfig.setSessionListeners(this);

        // Notify session listeners of invalidated session
        for (int n = 0; n < this.sessionActivationListeners.length; n++) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.webAppConfig.getLoader());
            this.sessionActivationListeners[n].sessionDidActivate(
                    new HttpSessionEvent(this));
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
    
    /**
     * Save this session to the temp dir defined for this webapp
     */
    public void saveToTemp() {
        File toDir = getSessionTempDir(this.webAppConfig);
        synchronized (this.sessionMonitor) {
            OutputStream out = null;
            ObjectOutputStream objOut = null;
            try {
                File toFile = new File(toDir, this.sessionId + ".ser");
                out = new FileOutputStream(toFile, false);
                objOut = new ObjectOutputStream(out);
                objOut.writeObject(this);
            } catch (IOException err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, 
                        "WinstoneSession.ErrorSavingSession", err);
            } finally {
                if (objOut != null) {
                    try {objOut.close();} catch (IOException err) {}
                }
                if (out != null) {
                    try {out.close();} catch (IOException err) {}
                }
            }
        }
    }
    
    public static File getSessionTempDir(WebAppConfiguration webAppConfig) {
        File tmpDir = (File) webAppConfig.getAttribute("javax.servlet.context.tempdir");
        File sessionsDir = new File(tmpDir, "WEB-INF" + File.separator + "winstoneSessions");
        if (!sessionsDir.exists()) {
            sessionsDir.mkdirs();
        }
        return sessionsDir;
    }
    
    public static void loadSessions(WebAppConfiguration webAppConfig) {
        int expiredCount = 0;
        // Iterate through the files in the dir, instantiate and then add to the sessions set
        File tempDir = getSessionTempDir(webAppConfig);
        File possibleSessionFiles[] = tempDir.listFiles();
        for (int n = 0; n < possibleSessionFiles.length; n++) {
            if (possibleSessionFiles[n].getName().endsWith(".ser")) {
                InputStream in = null;
                ObjectInputStream objIn = null;
                try {
                    in = new FileInputStream(possibleSessionFiles[n]);
                    objIn = new ObjectInputStream(in);
                    WinstoneSession session = (WinstoneSession) objIn.readObject();
                    session.setWebAppConfiguration(webAppConfig);
                    webAppConfig.setSessionListeners(session);
                    if (session.isExpired()) {
                        session.invalidate();
                        expiredCount++;
                    } else {
                        webAppConfig.addSession(session.getId(), session);
                        Logger.log(Logger.DEBUG, Launcher.RESOURCES, 
                                "WinstoneSession.RestoredSession", session.getId());
                    }
                } catch (Throwable err) {
                    Logger.log(Logger.ERROR, Launcher.RESOURCES, 
                            "WinstoneSession.ErrorLoadingSession", err);
                } finally {
                    if (objIn != null) {
                        try {objIn.close();} catch (IOException err) {}
                    }
                    if (in != null) {
                        try {in.close();} catch (IOException err) {}
                    }
                    possibleSessionFiles[n].delete();
                }
            }
        }
        if (expiredCount > 0) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                    "WebAppConfig.InvalidatedSessions", expiredCount + "");
        }
    }

    /**
     * Serialization implementation. This makes sure to only serialize the parts
     * we want to send to another server.
     * 
     * @param out
     *            The stream to write the contents to
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(sessionId);
        out.writeLong(createTime);
        out.writeLong(lastAccessedTime);
        out.writeInt(maxInactivePeriod);
        out.writeBoolean(isNew);
        out.writeBoolean(distributable);

        // Write the map, but first remove non-serializables
        Map copy = new HashMap(sessionData);
        Set keys = new HashSet(copy.keySet());
        for (Iterator i = keys.iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (!(copy.get(key) instanceof Serializable)) {
                Logger.log(Logger.WARNING, Launcher.RESOURCES,
                                "WinstoneSession.SkippingNonSerializable",
                                new String[] { key,
                                        copy.get(key).getClass().getName() });
            }
            copy.remove(key);
        }
        out.writeInt(copy.size());
        for (Iterator i = copy.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            out.writeUTF(key);
            out.writeObject(copy.get(key));
        }
    }

    /**
     * Deserialization implementation
     * 
     * @param in
     *            The source of stream data
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        this.sessionId = in.readUTF();
        this.createTime = in.readLong();
        this.lastAccessedTime = in.readLong();
        this.maxInactivePeriod = in.readInt();
        this.isNew = in.readBoolean();
        this.distributable = in.readBoolean();

        // Read the map
        this.sessionData = new Hashtable();
        this.requestsUsingMe = new HashSet();
        int entryCount = in.readInt();
        for (int n = 0; n < entryCount; n++) {
            String key = in.readUTF();
            Object variable = in.readObject();
            this.sessionData.put(key, variable);
        }
        this.sessionMonitor = new Boolean(true);
    }

    /**
     * @deprecated
     */
    public Object getValue(String name) {
        return getAttribute(name);
    }

    /**
     * @deprecated
     */
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    /**
     * @deprecated
     */
    public void removeValue(String name) {
        removeAttribute(name);
    }

    /**
     * @deprecated
     */
    public String[] getValueNames() {
        return (String[]) this.sessionData.keySet().toArray(new String[0]);
    }

    /**
     * @deprecated
     */
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return null;
    } // deprecated
}
