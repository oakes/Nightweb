/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.ajp13;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Map;

import winstone.Logger;
import winstone.RequestHandlerThread;
import winstone.WinstoneException;

/**
 * Models a single incoming ajp13 packet.
 * 
 * Fixes by Cory Osborn 2007/4/3 - IIS related. Thanks
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Ajp13IncomingPacket.java,v 1.6 2007/04/03 01:23:19 rickknowles Exp $
 */
public class Ajp13IncomingPacket {
    // Server originated packet types
    byte SERVER_FORWARD_REQUEST = 0x02;

    // public static byte SERVER_SHUTDOWN = 0x07; //not implemented
    // public static byte SERVER_PING = 0x08; //not implemented
    // public static byte SERVER_CPING = 0x10; //not implemented

    private int packetLength;
    private byte packetBytes[];
    private byte packetType;
    private String method;
    private String protocol;
    private String uri;
    private String remoteAddr;
    private String remoteHost;
    private String serverName;
    private int serverPort;
    private boolean isSSL;
    private String headers[];
    private Map attributes;

    /**
     * Constructor
     */
    public Ajp13IncomingPacket(InputStream in, 
            RequestHandlerThread handler) throws IOException {
        // Get the incoming packet flag
        byte headerBuffer[] = new byte[4];
        int headerBytesRead = in.read(headerBuffer);
        handler.setRequestStartTime();
        if (headerBytesRead != 4)
            throw new WinstoneException(Ajp13Listener.AJP_RESOURCES
                    .getString("Ajp13IncomingPacket.InvalidHeader"));
        else if ((headerBuffer[0] != 0x12) || (headerBuffer[1] != 0x34))
            throw new WinstoneException(Ajp13Listener.AJP_RESOURCES
                    .getString("Ajp13IncomingPacket.InvalidHeader"));

        // Read in the whole packet
        packetLength = ((headerBuffer[2] & 0xFF) << 8)
                + (headerBuffer[3] & 0xFF);
        packetBytes = new byte[packetLength];
        int packetBytesRead = in.read(packetBytes);

        if (packetBytesRead < packetLength)
            throw new WinstoneException(Ajp13Listener.AJP_RESOURCES
                    .getString("Ajp13IncomingPacket.ShortPacket"));
        // Ajp13Listener.packetDump(packetBytes, packetBytesRead);
    }

    public byte parsePacket(String encoding) throws IOException {
        int position = 0;
        this.packetType = packetBytes[position++];

        if (this.packetType != SERVER_FORWARD_REQUEST)
            throw new WinstoneException(Ajp13Listener.AJP_RESOURCES.getString(
                    "Ajp13IncomingPacket.UnknownPacketType", this.packetType
                            + ""));

        // Check for terminator
        if (packetBytes[packetLength - 1] != (byte) 255)
            throw new WinstoneException(Ajp13Listener.AJP_RESOURCES
                    .getString("Ajp13IncomingPacket.InvalidTerminator"));

        this.method = decodeMethodType(packetBytes[position++]);
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.Method", method);

        // Protocol
        int protocolLength = readInteger(position, packetBytes, true);
        position += 2;
        this.protocol = (protocolLength > -1)
                ? readString(position, packetBytes, encoding, protocolLength)
                        : null;
        position += protocolLength + 1;
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.Protocol", protocol);

        // URI
        int uriLength = readInteger(position, packetBytes, true);
        position += 2;
        this.uri = (uriLength > -1)
                ? readString(position, packetBytes, encoding, uriLength)
                : null;
        position += uriLength + 1;
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.URI", uri);

        // Remote addr
        int remoteAddrLength = readInteger(position, packetBytes, true);
        position += 2;
        this.remoteAddr = (remoteAddrLength > -1)
                ? readString(position, packetBytes, encoding, remoteAddrLength)
                : null;
        position += remoteAddrLength + 1;
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.RemoteAddress", remoteAddr);

        // Remote host
        int remoteHostLength = readInteger(position, packetBytes, true);
        position += 2;
        this.remoteHost = (remoteHostLength > -1)
                ? readString(position, packetBytes, encoding, remoteHostLength)
                : null;    
        position += remoteHostLength + 1;
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.RemoteHost", remoteHost);

        // Server name
        int serverNameLength = readInteger(position, packetBytes, true);
        position += 2;
        this.serverName = (serverNameLength > -1)
                ? readString(position, packetBytes, encoding, serverNameLength)
                : null;
        position += serverNameLength + 1;
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.ServerName", serverName);

        this.serverPort = readInteger(position, packetBytes, false);
        position += 2;
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.ServerPort", "" + serverPort);

        this.isSSL = readBoolean(position++, packetBytes);
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.SSL", "" + isSSL);

        // Read headers
        int headerCount = readInteger(position, packetBytes, false);
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.HeaderCount", "" + headerCount);
        position += 2;
        this.headers = new String[headerCount];
        for (int n = 0; n < headerCount; n++) {
            // Header name
            int headerTypeOrLength = readInteger(position, packetBytes, false);
            position += 2;
            String headerName = null;
            if (packetBytes[position - 2] == (byte) 0xA0)
                headerName = decodeHeaderType(headerTypeOrLength);
            else {
                headerName = readString(position, packetBytes, encoding,
                        headerTypeOrLength);
                position += headerTypeOrLength + 1;
            }

            // Header value
            int headerValueLength = readInteger(position, packetBytes, true);
            position += 2;
            this.headers[n] = headerName
                    + ": "
                    + ((headerValueLength > -1)
                        ? readString(position, packetBytes, encoding, headerValueLength)
                        : "");
            position += headerValueLength + 1;
            Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                    "Ajp13IncomingPacket.Header", this.headers[n]);
        }

        // Attribute parsing
        this.attributes = new Hashtable();
        while (position < packetLength - 2) {
            String attName = decodeAttributeType(packetBytes[position++]);
            int attValueLength = readInteger(position, packetBytes, true);
            position += 2;
            String attValue = (attValueLength > -1)
                      ? readString(position, packetBytes, encoding, attValueLength)
                      : null;
            position += attValueLength + 1;

            this.attributes.put(attName, attValue);
            Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                    "Ajp13IncomingPacket.Attribute", new String[] { attName,
                            attValue });
        }
        Logger.log(Logger.FULL_DEBUG, Ajp13Listener.AJP_RESOURCES,
                "Ajp13IncomingPacket.SuccessfullyReadRequest", ""
                        + packetLength);
        return this.packetType;
    }

    public int getPacketLength() {
        return this.packetLength;
    }

    public String getMethod() {
        return this.method;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getURI() {
        return this.uri;
    }

    public String getRemoteAddress() {
        return this.remoteAddr;
    }

    public String getRemoteHost() {
        return this.remoteHost;
    }

    public String getServerName() {
        return this.serverName;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public boolean isSSL() {
        return this.isSSL;
    }

    public String[] getHeaders() {
        return this.headers;
    }

    public Map getAttributes() {
        return this.attributes;
    }

    /**
     * Read a single integer from the stream
     */
    private int readInteger(int position, byte packet[], boolean forStringLength) {
        if (forStringLength && (packet[position] == (byte) 0xFF)
                && (packet[position + 1] == (byte) 0xFF))
            return -1;
        else
            return ((packet[position] & 0xFF) << 8)
                    + (packet[position + 1] & 0xFF);
    }

    /**
     * Read a single boolean from the stream
     */
    private boolean readBoolean(int position, byte packet[]) {
        return (packet[position] == (byte) 1);
    }

    /**
     * Read a single string from the stream
     */
    private String readString(int position, byte packet[], String encoding,
            int length) throws UnsupportedEncodingException {
//        System.out.println("Reading string length: " + length + 
//                " position=" + position + " packetLength=" + packet.length);
        return length == 0 ? ""
                : new String(packet, position, length, encoding);
    }

    /**
     * Decodes the method types into Winstone HTTP method strings
     */
    private String decodeMethodType(byte methodType) {
        switch (methodType) {
        case 1:
            return "OPTIONS";
        case 2:
            return "GET";
        case 3:
            return "HEAD";
        case 4:
            return "POST";
        case 5:
            return "PUT";
        case 6:
            return "DELETE";
        case 7:
            return "TRACE";
        case 8:
            return "PROPFIND";
        case 9:
            return "PROPPATCH";
        case 10:
            return "MKCOL";
        case 11:
            return "COPY";
        case 12:
            return "MOVE";
        case 13:
            return "LOCK";
        case 14:
            return "UNLOCK";
        case 15:
            return "ACL";
        case 16:
            return "REPORT";
        case 17:
            return "VERSION-CONTROL";
        case 18:
            return "CHECKIN";
        case 19:
            return "CHECKOUT";
        case 20:
            return "UNCHECKOUT";
        case 21:
            return "SEARCH";
        case 22:
            return "MKWORKSPACE";
        case 23:
            return "UPDATE";
        case 24:
            return "LABEL";
        case 25:
            return "MERGE";
        case 26:
            return "BASELINE_CONTROL";
        case 27:
            return "MKACTIVITY";
        default:
            return "UNKNOWN";
        }
    }

    /**
     * Decodes the header types into Winstone HTTP header strings
     */
    private String decodeHeaderType(int headerType) {
        switch (headerType) {
        case 0xA001:
            return "Accept";
        case 0xA002:
            return "Accept-Charset";
        case 0xA003:
            return "Accept-Encoding";
        case 0xA004:
            return "Accept-Language";
        case 0xA005:
            return "Authorization";
        case 0xA006:
            return "Connection";
        case 0xA007:
            return "Content-Type";
        case 0xA008:
            return "Content-Length";
        case 0xA009:
            return "Cookie";
        case 0xA00A:
            return "Cookie2";
        case 0xA00B:
            return "Host";
        case 0xA00C:
            return "Pragma";
        case 0xA00D:
            return "Referer";
        case 0xA00E:
            return "User-Agent";
        default:
            return null;
        }
    }

    /**
     * Decodes the header types into Winstone HTTP header strings
     */
    private String decodeAttributeType(byte attributeType) {
        switch (attributeType) {
        case 0x01:
            return "context";
        case 0x02:
            return "servlet_path";
        case 0x03:
            return "remote_user";
        case 0x04:
            return "auth_type";
        case 0x05:
            return "query_string";
        case 0x06:
            return "jvm_route";
        case 0x07:
            return "ssl_cert";
        case 0x08:
            return "ssl_cipher";
        case 0x09:
            return "ssl_session";
        case 0x0A:
            return "req_attribute";
        default:
            return null;
        }
    }
}
