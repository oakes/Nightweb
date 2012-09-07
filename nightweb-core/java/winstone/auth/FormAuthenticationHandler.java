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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Node;

import winstone.AuthenticationPrincipal;
import winstone.AuthenticationRealm;
import winstone.Logger;
import winstone.WebAppConfiguration;
import winstone.WinstoneRequest;

/**
 * Handles FORM based authentication configurations. Fairly simple ... it just
 * redirects any unauthorized requests to the login page, and any bad logins to
 * the error page. The auth values are stored in the session in a special slot.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: FormAuthenticationHandler.java,v 1.7 2006/12/13 14:07:43 rickknowles Exp $
 */
public class FormAuthenticationHandler extends BaseAuthenticationHandler {
    private static final String ELEM_FORM_LOGIN_CONFIG = "form-login-config";
    private static final String ELEM_FORM_LOGIN_PAGE = "form-login-page";
    private static final String ELEM_FORM_ERROR_PAGE = "form-error-page";
    private static final String FORM_ACTION = "j_security_check";
    private static final String FORM_USER = "j_username";
    private static final String FORM_PASS = "j_password";
    private static final String AUTHENTICATED_USER = "winstone.auth.FormAuthenticationHandler.AUTHENTICATED_USER";
    private static final String CACHED_REQUEST = "winstone.auth.FormAuthenticationHandler.CACHED_REQUEST";
    
    private String loginPage;
    private String errorPage;

    /**
     * Constructor for the FORM authenticator
     * 
     * @param realm
     *            The realm against which we are authenticating
     * @param constraints
     *            The array of security constraints that might apply
     * @param resources
     *            The list of resource strings for messages
     * @param realmName
     *            The name of the realm this handler claims
     */
    public FormAuthenticationHandler(Node loginConfigNode,
            List constraintNodes, Set rolesAllowed,
            AuthenticationRealm realm) {
        super(loginConfigNode, constraintNodes, rolesAllowed, realm);

        for (int n = 0; n < loginConfigNode.getChildNodes().getLength(); n++) {
            Node loginElm = loginConfigNode.getChildNodes().item(n);
            if (loginElm.getNodeName().equals(ELEM_FORM_LOGIN_CONFIG)) {
                for (int k = 0; k < loginElm.getChildNodes().getLength(); k++) {
                    Node formElm = loginElm.getChildNodes().item(k);
                    if (formElm.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                    else if (formElm.getNodeName().equals(ELEM_FORM_LOGIN_PAGE))
                        loginPage = WebAppConfiguration.getTextFromNode(formElm);
                    else if (formElm.getNodeName().equals(ELEM_FORM_ERROR_PAGE))
                        errorPage = WebAppConfiguration.getTextFromNode(formElm);
                }
            }
        }
        Logger.log(Logger.DEBUG, AUTH_RESOURCES,
                "FormAuthenticationHandler.Initialised", realmName);
    }

    /**
     * Evaluates any authentication constraints, intercepting if auth is
     * required. The relevant authentication handler subclass's logic is used to
     * actually authenticate.
     * 
     * @return A boolean indicating whether to continue after this request
     */
    public boolean processAuthentication(ServletRequest request,
            ServletResponse response, String pathRequested) throws IOException,
            ServletException {
        if (pathRequested.equals(this.loginPage)
                || pathRequested.equals(this.errorPage)) {
            return true;
        } else {
            return super.processAuthentication(request, response, pathRequested);
        }
    }

    /**
     * Call this once we know that we need to authenticate
     */
    protected void requestAuthentication(HttpServletRequest request,
            HttpServletResponse response, String pathRequested)
            throws ServletException, IOException {
        // Save the critical details of the request into the session map
        ServletRequest unwrapped = request;
        while (unwrapped instanceof HttpServletRequestWrapper) {
            unwrapped = ((HttpServletRequestWrapper) unwrapped).getRequest();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(CACHED_REQUEST, new RetryRequestParams(unwrapped));

        // Forward on to the login page
        Logger.log(Logger.FULL_DEBUG, AUTH_RESOURCES,
                "FormAuthenticationHandler.GoToLoginPage");
        javax.servlet.RequestDispatcher rdLogin = request
                .getRequestDispatcher(this.loginPage);
        setNoCache(response);
        rdLogin.forward(request, response);
    }

    /**
     * Check the response - is it a response to the login page ?
     * 
     * @return A boolean indicating whether to continue with the request or not
     */
    protected boolean validatePossibleAuthenticationResponse(
            HttpServletRequest request, HttpServletResponse response,
            String pathRequested) throws ServletException, IOException {
        // Check if this is a j_security_check uri
        if (pathRequested.endsWith(FORM_ACTION)) {
            String username = request.getParameter(FORM_USER);
            String password = request.getParameter(FORM_PASS);

            // Send to error page if invalid
            AuthenticationPrincipal principal = this.realm
                    .authenticateByUsernamePassword(username, password);
            if (principal == null) {
                javax.servlet.RequestDispatcher rdError = request
                        .getRequestDispatcher(this.errorPage);
                rdError.forward(request, response);
            }

            // Send to stashed request
            else {
                // Iterate back as far as we can
                ServletRequest wrapperCheck = request;
                while (wrapperCheck instanceof HttpServletRequestWrapper) {
                    wrapperCheck = ((HttpServletRequestWrapper) wrapperCheck).getRequest();
                }
                
                // Get the stashed request
                WinstoneRequest actualRequest = null;
                if (wrapperCheck instanceof WinstoneRequest) {
                    actualRequest = (WinstoneRequest) wrapperCheck;
                    actualRequest.setRemoteUser(principal);
                } else {
                    Logger.log(Logger.WARNING, AUTH_RESOURCES,
                            "FormAuthenticationHandler.CantSetUser",
                            wrapperCheck.getClass().getName());
                }
                HttpSession session = request.getSession(true);
                String previousLocation = this.loginPage;
                RetryRequestParams cachedRequest = (RetryRequestParams) 
                        session.getAttribute(CACHED_REQUEST);
                if ((cachedRequest != null) && (actualRequest != null)) {
                    // Repopulate this request from the params we saved
                    request = new RetryRequestWrapper(request, cachedRequest);
                    previousLocation = 
                        (request.getServletPath() == null ? "" : request.getServletPath()) + 
                        (request.getPathInfo() == null ? "" : request.getPathInfo());
                } else {
                    Logger.log(Logger.DEBUG, AUTH_RESOURCES,
                            "FormAuthenticationHandler.NoCachedRequest");
                }
                
                // do role check, since we don't know that this user has permission
                if (doRoleCheck(request, response, previousLocation)) {
                    principal.setAuthType(HttpServletRequest.FORM_AUTH);
                    session.setAttribute(AUTHENTICATED_USER, principal);
                    javax.servlet.RequestDispatcher rdPrevious = request
                            .getRequestDispatcher(previousLocation);
                    rdPrevious.forward(request, response);
                } else {
                    javax.servlet.RequestDispatcher rdError = request
                            .getRequestDispatcher(this.errorPage);
                    rdError.forward(request, response);
                }
            }
            return false;
        }
        // If it's not a login, get the session, and look up the auth user variable
        else {
            WinstoneRequest actualRequest = null;
            if (request instanceof WinstoneRequest) {
                actualRequest = (WinstoneRequest) request;
            } else if (request instanceof HttpServletRequestWrapper) { 
                HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
                if (wrapper.getRequest() instanceof WinstoneRequest) {
                    actualRequest = (WinstoneRequest) wrapper.getRequest();
                } else {
                    Logger.log(Logger.WARNING, AUTH_RESOURCES,
                            "FormAuthenticationHandler.CantSetUser", wrapper
                                    .getRequest().getClass().getName());
                }
            } else {
                Logger.log(Logger.WARNING, AUTH_RESOURCES,
                        "FormAuthenticationHandler.CantSetUser", request
                                .getClass().getName());
            }

            HttpSession session = actualRequest.getSession(false);
            if (session != null) {
                AuthenticationPrincipal authenticatedUser = (AuthenticationPrincipal) 
                        session.getAttribute(AUTHENTICATED_USER); 
                if (authenticatedUser != null) {
                    actualRequest.setRemoteUser(authenticatedUser);
                    Logger.log(Logger.FULL_DEBUG, AUTH_RESOURCES,
                            "FormAuthenticationHandler.GotUserFromSession");
                }
            }
            return true;
        }
    }
}
