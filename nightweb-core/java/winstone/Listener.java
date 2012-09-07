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
import java.net.SocketException;

/**
 * Interface that defines the necessary methods for being a connection listener
 * within winstone.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface Listener {
    /**
     * Interrupts the listener thread. This will trigger a listener shutdown
     * once the so timeout has passed.
     */
    public void destroy();
    
    /**
     * After the listener is loaded and initialized, this starts the thread
     */
    public boolean start();

    /**
     * Called by the request handler thread, because it needs specific setup
     * code for this connection's protocol (ie construction of request/response
     * objects, in/out streams, etc). The iAmFirst variable identifies whether or 
     * not this is the initial request on on this socket (ie a keep alive or 
     * a first-time accept)
     */
    public void allocateRequestResponse(Socket socket, InputStream inSocket,
            OutputStream outSocket, RequestHandlerThread handler,
            boolean iAmFirst) throws SocketException, IOException;

    /**
     * Called by the request handler thread, because it needs specific shutdown
     * code for this connection's protocol (ie releasing input/output streams,
     * etc).
     */
    public void deallocateRequestResponse(RequestHandlerThread handler,
            WinstoneRequest req, WinstoneResponse rsp,
            WinstoneInputStream inData, WinstoneOutputStream outData)
            throws IOException;

    /**
     * Called by the request handler thread, because it needs specific shutdown
     * code for this connection's protocol if the keep-alive period expires (ie
     * closing sockets, etc).The iAmFirst variable identifies whether or 
     * not this is the initial request on on this socket (ie a keep alive or 
     * a first-time accept)
     */
    public String parseURI(RequestHandlerThread handler, WinstoneRequest req,
            WinstoneResponse rsp, WinstoneInputStream inData, Socket socket,
            boolean iAmFirst) throws IOException;

    /**
     * Called by the request handler thread, because it needs specific shutdown
     * code for this connection's protocol if the keep-alive period expires (ie
     * closing sockets, etc).
     */
    public void releaseSocket(Socket socket, InputStream inSocket,
            OutputStream outSocket) throws IOException;

    /**
     * Tries to wait for extra requests on the same socket. If any are found
     * before the timeout expires, it exits with a true, indicating a new
     * request is waiting. If the timeout expires, return a false, instructing
     * the handler thread to begin shutting down the socket and relase itself.
     */
    public boolean processKeepAlive(WinstoneRequest request,
            WinstoneResponse response, InputStream inSocket)
            throws IOException, InterruptedException;
}
