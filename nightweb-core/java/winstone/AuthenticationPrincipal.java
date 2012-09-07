/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;

/**
 * Implements the principal method - basically just a way of identifying an
 * authenticated user.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: AuthenticationPrincipal.java,v 1.2 2006/02/28 07:32:47 rickknowles Exp $
 */
public class AuthenticationPrincipal implements Principal, Serializable {
    private String userName;
    private String password;
    private List roles;
    private String authenticationType;

    /**
     * Constructor
     */
    public AuthenticationPrincipal(String userName, String password, List roles) {
        this.userName = userName;
        this.password = password;
        this.roles = roles;
    }

    public String getName() {
        return this.userName;
    }

    public String getPassword() {
        return this.password;
    }

    public String getAuthType() {
        return this.authenticationType;
    }

    public void setAuthType(String authType) {
        this.authenticationType = authType;
    }

    /**
     * Searches for the requested role in this user's roleset.
     */
    public boolean isUserIsInRole(String role) {
        if (this.roles == null)
            return false;
        else if (role == null)
            return false;
        else
            return this.roles.contains(role);
    }
}
