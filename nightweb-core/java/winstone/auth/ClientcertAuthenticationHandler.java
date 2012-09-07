/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.auth;

import java.io.IOException;
import java.security.cert.X509Certificate;
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
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ClientcertAuthenticationHandler.java,v 1.3 2006/02/28 07:32:47 rickknowles Exp $
 */
public class ClientcertAuthenticationHandler extends BaseAuthenticationHandler {
    public ClientcertAuthenticationHandler(Node loginConfigNode,
            List constraintNodes, Set rolesAllowed,
            AuthenticationRealm realm) {
        super(loginConfigNode, constraintNodes, rolesAllowed, realm);
        Logger.log(Logger.DEBUG, AUTH_RESOURCES,
                "ClientcertAuthenticationHandler.Initialised", realmName);
    }

    /**
     * Call this once we know that we need to authenticate
     */
    protected void requestAuthentication(HttpServletRequest request,
            HttpServletResponse response, String pathRequested)
            throws IOException {
        // Return unauthorized, and set the realm name
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                AUTH_RESOURCES.getString("ClientcertAuthenticationHandler.UnauthorizedMessage"));
    }

    /**
     * Handling the (possible) response
     */
    protected boolean validatePossibleAuthenticationResponse(
            HttpServletRequest request, HttpServletResponse response,
            String pathRequested) throws IOException {
        // Check for certificates in the request attributes
        X509Certificate certificateArray[] = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");
        if ((certificateArray != null) && (certificateArray.length > 0)) {
            boolean failed = false;
            for (int n = 0; n < certificateArray.length; n++)
                try {
                    certificateArray[n].checkValidity();
                } catch (Throwable err) {
                    failed = true;
                }
            if (!failed) {
                AuthenticationPrincipal principal = this.realm
                        .retrieveUser(certificateArray[0].getSubjectDN()
                                .getName());
                if (principal != null) {
                    principal.setAuthType(HttpServletRequest.CLIENT_CERT_AUTH);
                    if (request instanceof WinstoneRequest)
                        ((WinstoneRequest) request).setRemoteUser(principal);
                    else if (request instanceof HttpServletRequestWrapper) {
                        HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                        if (wrapper.getRequest() instanceof WinstoneRequest)
                            ((WinstoneRequest) wrapper.getRequest())
                                    .setRemoteUser(principal);
                        else
                            Logger.log(Logger.WARNING, AUTH_RESOURCES,
                                    "ClientCertAuthenticationHandler.CantSetUser",
                                            wrapper.getRequest().getClass().getName());
                    } else
                        Logger.log(Logger.WARNING, AUTH_RESOURCES,
                                "ClientCertAuthenticationHandler.CantSetUser",
                                request.getClass().getName());
                }
            }
        }
        return true;
    }
}
