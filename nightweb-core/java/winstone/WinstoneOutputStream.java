/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Stack;

import javax.servlet.http.Cookie;

/**
 * Matches the socket output stream to the servlet output.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneOutputStream.java,v 1.19 2007/10/14 14:48:14 rickknowles Exp $
 */
public class WinstoneOutputStream extends javax.servlet.ServletOutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final byte[] CR_LF = "\r\n".getBytes();
    protected OutputStream outStream;
    protected int bufferSize;
    protected int bufferPosition;
    protected int bytesCommitted;
    protected ByteArrayOutputStream buffer;
    protected boolean committed;
    protected boolean bodyOnly;
    protected WinstoneResponse owner;
    protected boolean disregardMode = false;
    protected boolean closed = false;
    protected Stack includeByteStreams;
    
    /**
     * Constructor
     */
    public WinstoneOutputStream(OutputStream out, boolean bodyOnlyForInclude) {
        this.outStream = out;
        this.bodyOnly = bodyOnlyForInclude;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.committed = false;
        // this.headersWritten = false;
        this.buffer = new ByteArrayOutputStream();
    }

    public void setResponse(WinstoneResponse response) {
        this.owner = response;
    }

    public int getBufferSize() {
        return this.bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        if (this.owner.isCommitted()) {
            throw new IllegalStateException(Launcher.RESOURCES.getString(
                    "WinstoneOutputStream.AlreadyCommitted"));
        }
        this.bufferSize = bufferSize;
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public int getOutputStreamLength() {
        return this.bytesCommitted + this.bufferPosition;
    }

    public int getBytesCommitted() {
        return this.bytesCommitted;
    }
    
    public void setDisregardMode(boolean disregard) {
        this.disregardMode = disregard;
    }
    
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void write(int oneChar) throws IOException {
        if (this.disregardMode || this.closed) {
            return;
        }
        String contentLengthHeader = this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER);
        if ((contentLengthHeader != null) && 
                (this.bytesCommitted >= Integer.parseInt(contentLengthHeader))) {
            return;
        }
//        System.out.println("Out: " + this.bufferPosition + " char=" + (char)oneChar);
        this.buffer.write(oneChar);
        this.bufferPosition++;
        // if (this.headersWritten)
        if (this.bufferPosition >= this.bufferSize) {
            commit();
        } else if ((contentLengthHeader != null) && 
                ((this.bufferPosition + this.bytesCommitted) 
                        >= Integer.parseInt(contentLengthHeader))) {
            commit();
        }
    }

    public void commit() throws IOException {
        this.buffer.flush();

        // If we haven't written the headers yet, write them out
        if (!this.committed && !this.bodyOnly) {
            this.owner.validateHeaders();
            this.committed = true;

            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneOutputStream.CommittingOutputStream");
            
            int statusCode = this.owner.getStatus();
            String reason = Launcher.RESOURCES.getString("WinstoneOutputStream.reasonPhrase." + statusCode);
            String statusLine = this.owner.getProtocol() + " " + statusCode + " " + 
                    (reason == null ? "No reason" : reason);
            this.outStream.write(statusLine.getBytes("8859_1"));
            this.outStream.write(CR_LF);
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "WinstoneOutputStream.ResponseStatus", statusLine);

            // Write headers and cookies
            for (Iterator i = this.owner.getHeaders().iterator(); i.hasNext();) {
                String header = (String) i.next();
                this.outStream.write(header.getBytes("8859_1"));
                this.outStream.write(CR_LF);
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "WinstoneOutputStream.Header", header);
            }

            if (!this.owner.getHeaders().isEmpty()) {
                for (Iterator i = this.owner.getCookies().iterator(); i.hasNext();) {
                    Cookie cookie = (Cookie) i.next();
                    String cookieText = this.owner.writeCookie(cookie);
                    this.outStream.write(cookieText.getBytes("8859_1"));
                    this.outStream.write(CR_LF);
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "WinstoneOutputStream.Header", cookieText);
                }
            }
            this.outStream.write(CR_LF);
            this.outStream.flush();
            // Logger.log(Logger.FULL_DEBUG,
            // Launcher.RESOURCES.getString("HttpProtocol.OutHeaders") + out.toString());
        }
        byte content[] = this.buffer.toByteArray();
//        winstone.ajp13.Ajp13Listener.packetDump(content, content.length);
//        this.buffer.writeTo(this.outStream);
        int commitLength = content.length;
        String contentLengthHeader = this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER);
        if (contentLengthHeader != null) {
            commitLength = Math.min(Integer.parseInt(contentLengthHeader)
                    - this.bytesCommitted, content.length);
        }
        if (commitLength > 0) {
            this.outStream.write(content, 0, commitLength);
        }
        this.outStream.flush();

        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                "WinstoneOutputStream.CommittedBytes", 
                "" + (this.bytesCommitted + commitLength));

        this.bytesCommitted += commitLength;
        this.buffer.reset();
        this.bufferPosition = 0;
    }

    public void reset() {
        if (isCommitted())
            throw new IllegalStateException(Launcher.RESOURCES
                    .getString("WinstoneOutputStream.AlreadyCommitted"));
        else {
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "WinstoneOutputStream.ResetBuffer", this.bufferPosition
                            + "");
            this.buffer.reset();
            this.bufferPosition = 0;
            this.bytesCommitted = 0;
        }
    }

    public void finishResponse() throws IOException {
        this.outStream.flush();
        this.outStream = null;
    }

    public void flush() throws IOException {
        if (this.disregardMode) {
            return;
        }
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneOutputStream.Flushing");
        this.buffer.flush();
        this.commit();
    }

    public void close() throws IOException {
        if (!isCommitted() && !this.disregardMode && !this.closed &&
                (this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER) == null)) {
            if ((this.owner != null) && !this.bodyOnly) {
                this.owner.setContentLength(getOutputStreamLength());
            }
        }
        flush();
    }

    // Include related buffering
    public boolean isIncluding() {
        return (this.includeByteStreams != null && !this.includeByteStreams.isEmpty());
    }
    
    public void startIncludeBuffer() {
        synchronized (this.buffer) {
            if (this.includeByteStreams == null) {
                this.includeByteStreams = new Stack();
            }
        }
        this.includeByteStreams.push(new ByteArrayOutputStream());
    }
    
    public void finishIncludeBuffer() throws IOException {
        if (isIncluding()) {
            ByteArrayOutputStream body = (ByteArrayOutputStream) this.includeByteStreams.pop();
            OutputStream topStream = this.outStream;
            if (!this.includeByteStreams.isEmpty()) {
                topStream = (OutputStream) this.includeByteStreams.peek();
            }
            byte bodyArr[] = body.toByteArray();
            if (bodyArr.length > 0) {
                topStream.write(bodyArr);
            }
            body.close();
        }
    }
    
    public void clearIncludeStackForForward() throws IOException {
        if (isIncluding()) {
            for (Iterator i = this.includeByteStreams.iterator(); i.hasNext(); ) {
                ((ByteArrayOutputStream) i.next()).close();
            }
            this.includeByteStreams.clear();
        }
    }
}
