/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.auth;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Node;

import winstone.AuthenticationPrincipal;
import winstone.AuthenticationRealm;
import winstone.Logger;
import winstone.WinstoneRequest;

/**
 * Handles HTTP basic authentication.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: BasicAuthenticationHandler.java,v 1.5 2007/04/11 13:14:26 rickknowles Exp $
 */
public class BasicAuthenticationHandler extends BaseAuthenticationHandler {
    public BasicAuthenticationHandler(Node loginConfigNode,
            List constraintNodes, Set rolesAllowed,
            AuthenticationRealm realm) {
        super(loginConfigNode, constraintNodes, rolesAllowed, realm);
        Logger.log(Logger.DEBUG, AUTH_RESOURCES,
                "BasicAuthenticationHandler.Initialised", realmName);
    }

    /**
     * Call this once we know that we need to authenticate
     */
    protected void requestAuthentication(HttpServletRequest request,
            HttpServletResponse response, String pathRequested)
            throws IOException {
        // Return unauthorized, and set the realm name
        response.setHeader("WWW-Authenticate", "Basic Realm=\""
                + this.realmName + "\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, AUTH_RESOURCES
                .getString("BasicAuthenticationHandler.UnauthorizedMessage"));
    }

    /**
     * Handling the (possible) response
     */
    protected boolean validatePossibleAuthenticationResponse(
            HttpServletRequest request, HttpServletResponse response,
            String pathRequested) throws IOException {
        String authorization = request.getHeader("Authorization");
        if ((authorization != null)
                && authorization.toLowerCase().startsWith("basic")) {
            
            char[] inBytes = authorization.substring(5).trim().toCharArray();
            byte[] outBytes = new byte[(int) (inBytes.length * 0.75f)]; // always mod 4 = 0
            int length = decodeBase64(inBytes, outBytes, 0, inBytes.length, 0);

            String decoded = new String(outBytes, 0, length);
            int delimPos = decoded.indexOf(':');
            if (delimPos != -1) {
                AuthenticationPrincipal principal = this.realm
                        .authenticateByUsernamePassword(decoded.substring(0,
                                delimPos).trim(), decoded.substring(
                                delimPos + 1).trim());
                if (principal != null) {
                    principal.setAuthType(HttpServletRequest.BASIC_AUTH);
                    if (request instanceof WinstoneRequest)
                        ((WinstoneRequest) request).setRemoteUser(principal);
                    else if (request instanceof HttpServletRequestWrapper) {
                        HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                        if (wrapper.getRequest() instanceof WinstoneRequest)
                            ((WinstoneRequest) wrapper.getRequest())
                                    .setRemoteUser(principal);
                        else
                            Logger.log(Logger.WARNING, AUTH_RESOURCES,
                                    "BasicAuthenticationHandler.CantSetUser",
                                    wrapper.getRequest().getClass().getName());
                    } else
                        Logger.log(Logger.WARNING, AUTH_RESOURCES,
                                "BasicAuthenticationHandler.CantSetUser",
                                request.getClass().getName());
                }
            }
        }
        return true;
    }

    /**
     * Decodes a byte array from base64
     */
    public static int decodeBase64(char[] input, byte[] output, 
            int inOffset, int inLength, int outOffset) {
        if (inLength == 0) {
            return 0;
        }

        int outIndex = outOffset;
        for (int inIndex = inOffset; inIndex < inLength; ) {
            // Decode four bytes
            int thisPassInBytes = Math.min(inLength - inIndex, 4);
            while ((thisPassInBytes > 1) && 
                    (input[inIndex + thisPassInBytes - 1] == '=')) {
                thisPassInBytes--;
            }

            if (thisPassInBytes == 2) {
                int outBuffer = ((B64_DECODE_ARRAY[input[inIndex]] & 0xFF) << 18)
                            | ((B64_DECODE_ARRAY[input[inIndex + 1]] & 0xFF) << 12);
                output[outIndex] = (byte) ((outBuffer >> 16) & 0xFF);
                outIndex += 1;
            } else if (thisPassInBytes == 3) {
                int outBuffer = ((B64_DECODE_ARRAY[input[inIndex]] & 0xFF) << 18)
                            | ((B64_DECODE_ARRAY[input[inIndex + 1]] & 0xFF) << 12)
                            | ((B64_DECODE_ARRAY[input[inIndex + 2]] & 0xFF) << 6);
                output[outIndex] = (byte) ((outBuffer >> 16) & 0xFF);
                output[outIndex + 1] = (byte) ((outBuffer >> 8) & 0xFF);
                outIndex += 2;
            } else if (thisPassInBytes == 4) {
                int outBuffer = ((B64_DECODE_ARRAY[input[inIndex]] & 0xFF) << 18)
                            | ((B64_DECODE_ARRAY[input[inIndex + 1]] & 0xFF) << 12)
                            | ((B64_DECODE_ARRAY[input[inIndex + 2]] & 0xFF) << 6)
                            | (B64_DECODE_ARRAY[input[inIndex + 3]] & 0xFF);
                output[outIndex] = (byte) ((outBuffer >> 16) & 0xFF);
                output[outIndex + 1] = (byte) ((outBuffer >> 8) & 0xFF);
                output[outIndex + 2] = (byte) (outBuffer & 0xFF);
                outIndex += 3;
            }
            inIndex += thisPassInBytes;
        }
        return outIndex;
    }
    
    private static byte B64_DECODE_ARRAY[] = new byte[] { -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, 62, // Plus sign
            -1, -1, -1, 63, // Slash
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers
            -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Large letters
            -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
            37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Small letters
            -1, -1, -1, -1 };
}
