/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Represents a cluster implementation, which is basically the communication
 * mechanism between a group of winstone containers.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Cluster.java,v 1.5 2006/02/28 07:32:47 rickknowles Exp $
 */
public interface Cluster {
    /**
     * Destroy the maintenance thread if there is one. Prepare for shutdown
     */
    public void destroy();

    /**
     * Check if the other nodes in this cluster have a session for this
     * sessionId.
     * 
     * @param sessionId The id of the session to check for
     * @param webAppConfig The web app that owns the session we want
     * @return A valid session instance
     */
    public WinstoneSession askClusterForSession(String sessionId,
            WebAppConfiguration webAppConfig);

    /**
     * Accept a control socket request related to the cluster functions and
     * process the request.
     * 
     * @param requestType A byte indicating the request type
     * @param in Socket input stream
     * @param outSocket output stream
     * @param hostConfig The collection of all local webapps
     * @throws IOException
     */
    public void clusterRequest(byte requestType, InputStream in,
            OutputStream out, Socket socket, HostGroup hostGroup)
            throws IOException;
}
