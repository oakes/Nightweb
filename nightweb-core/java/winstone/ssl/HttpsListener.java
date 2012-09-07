/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import winstone.HostGroup;
import winstone.HttpListener;
import winstone.Logger;
import winstone.ObjectPool;
import winstone.WebAppConfiguration;
import winstone.WinstoneException;
import winstone.WinstoneRequest;
import winstone.WinstoneResourceBundle;

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpsListener.java,v 1.10 2007/06/13 15:27:35 rickknowles Exp $
 */
public class HttpsListener extends HttpListener {
    private static final WinstoneResourceBundle SSL_RESOURCES = new WinstoneResourceBundle("winstone.ssl.LocalStrings");
    private String keystore;
    private String password;
    private String keyManagerType;

    /**
     * Constructor
     */
    public HttpsListener(Map args, ObjectPool objectPool, HostGroup hostGroup) throws IOException {
        super(args, objectPool, hostGroup);
        this.keystore = WebAppConfiguration.stringArg(args, getConnectorName()
                + "KeyStore", "winstone.ks");
        this.password = WebAppConfiguration.stringArg(args, getConnectorName()
                + "KeyStorePassword", null);
        this.keyManagerType = WebAppConfiguration.stringArg(args, 
                getConnectorName() + "KeyManagerType", "SunX509");
    }

    /**
     * The default port to use - this is just so that we can override for the
     * SSL connector.
     */
    protected int getDefaultPort() {
        return -1; // https disabled by default
    }

    /**
     * The name to use when getting properties - this is just so that we can
     * override for the SSL connector.
     */
    protected String getConnectorScheme() {
        return "https";
    }

    /**
     * Gets a server socket - this gets as SSL socket instead of the standard
     * socket returned in the base class.
     */
    protected ServerSocket getServerSocket() throws IOException {
        // Just to make sure it's set before we start
        SSLContext context = getSSLContext(this.keystore, this.password);
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket ss = (SSLServerSocket) (this.listenAddress == null ? factory
                .createServerSocket(this.listenPort, BACKLOG_COUNT)
                : factory.createServerSocket(this.listenPort, BACKLOG_COUNT,
                        InetAddress.getByName(this.listenAddress)));
        ss.setEnableSessionCreation(true);
        ss.setWantClientAuth(true);
        return ss;
    }

    /**
     * Extracts the relevant socket stuff and adds it to the request object.
     * This method relies on the base class for everything other than SSL
     * related attributes
     */
    protected void parseSocketInfo(Socket socket, WinstoneRequest req)
            throws IOException {
        super.parseSocketInfo(socket, req);
        if (socket instanceof SSLSocket) {
            SSLSocket s = (SSLSocket) socket;
            SSLSession ss = s.getSession();
            if (ss != null) {
                Certificate certChain[] = null;
                try {
                    certChain = ss.getPeerCertificates();
                } catch (Throwable err) {/* do nothing */
                }

                if (certChain != null) {
                    req.setAttribute("javax.servlet.request.X509Certificate",
                            certChain);
                    req.setAttribute("javax.servlet.request.cipher_suite", ss
                            .getCipherSuite());
                    req.setAttribute("javax.servlet.request.ssl_session",
                            new String(ss.getId()));
                    req.setAttribute("javax.servlet.request.key_size",
                            getKeySize(ss.getCipherSuite()));
                }
            }
            req.setIsSecure(true);
        }
    }

    /**
     * Just a mapping of key sizes for cipher types. Taken indirectly from the
     * TLS specs.
     */
    private Integer getKeySize(String cipherSuite) {
        if (cipherSuite.indexOf("_WITH_NULL_") != -1)
            return new Integer(0);
        else if (cipherSuite.indexOf("_WITH_IDEA_CBC_") != -1)
            return new Integer(128);
        else if (cipherSuite.indexOf("_WITH_RC2_CBC_40_") != -1)
            return new Integer(40);
        else if (cipherSuite.indexOf("_WITH_RC4_40_") != -1)
            return new Integer(40);
        else if (cipherSuite.indexOf("_WITH_RC4_128_") != -1)
            return new Integer(128);
        else if (cipherSuite.indexOf("_WITH_DES40_CBC_") != -1)
            return new Integer(40);
        else if (cipherSuite.indexOf("_WITH_DES_CBC_") != -1)
            return new Integer(56);
        else if (cipherSuite.indexOf("_WITH_3DES_EDE_CBC_") != -1)
            return new Integer(168);
        else
            return null;
    }

    /**
     * Used to get the base ssl context in which to create the server socket.
     * This is basically just so we can have a custom location for key stores.
     */
    public SSLContext getSSLContext(String keyStoreName, String password)
            throws IOException {
        try {
            // Check the key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(this.keyManagerType);
            
            File ksFile = new File(keyStoreName);
            if (!ksFile.exists() || !ksFile.isFile())
                throw new WinstoneException(SSL_RESOURCES.getString(
                        "HttpsListener.KeyStoreNotFound", ksFile.getPath()));
            InputStream in = new FileInputStream(ksFile);
            char[] passwordChars = password == null ? null : password.toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(in, passwordChars);
            kmf.init(ks, passwordChars);
            Logger.log(Logger.FULL_DEBUG, SSL_RESOURCES,
                    "HttpsListener.KeyCount", ks.size() + "");
            for (Enumeration e = ks.aliases(); e.hasMoreElements();) {
                String alias = (String) e.nextElement();
                Logger.log(Logger.FULL_DEBUG, SSL_RESOURCES,
                        "HttpsListener.KeyFound", new String[] { alias,
                                ks.getCertificate(alias) + "" });
            }

            SSLContext context = SSLContext.getInstance("SSL");
            context.init(kmf.getKeyManagers(), null, null);
            Arrays.fill(passwordChars, 'x');
            return context;
        } catch (IOException err) {
            throw err;
        } catch (Throwable err) {
            throw new WinstoneException(SSL_RESOURCES
                    .getString("HttpsListener.ErrorGettingContext"), err);
        }
    }
}
