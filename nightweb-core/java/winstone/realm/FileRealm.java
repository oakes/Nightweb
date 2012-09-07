/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.realm;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import winstone.AuthenticationPrincipal;
import winstone.AuthenticationRealm;
import winstone.Logger;
import winstone.WinstoneException;
import winstone.WinstoneResourceBundle;

/**
 * @author rickk
 * @version $Id: FileRealm.java,v 1.4 2006/08/30 04:07:52 rickknowles Exp $
 */
public class FileRealm implements AuthenticationRealm {
    private static final WinstoneResourceBundle REALM_RESOURCES = new WinstoneResourceBundle("winstone.realm.LocalStrings");
    
    final String FILE_NAME_ARGUMENT = "fileRealm.configFile";
    final String DEFAULT_FILE_NAME = "users.xml";
    final String ELEM_USER = "user";
    final String ATT_USERNAME = "username";
    final String ATT_PASSWORD = "password";
    final String ATT_ROLELIST = "roles";
    private Map passwords;
    private Map roles;

    /**
     * Constructor - this sets up an authentication realm, using the file
     * supplied on the command line as a source of userNames/passwords/roles.
     */
    public FileRealm(Set rolesAllowed, Map args) {
        this.passwords = new Hashtable();
        this.roles = new Hashtable();

        // Get the filename and parse the xml doc
        String realmFileName = args.get(FILE_NAME_ARGUMENT) == null ? DEFAULT_FILE_NAME
                : (String) args.get(FILE_NAME_ARGUMENT);
        File realmFile = new File(realmFileName);
        if (!realmFile.exists())
            throw new WinstoneException(REALM_RESOURCES.getString(
                    "FileRealm.FileNotFound", realmFile.getPath()));
        try {
            InputStream inFile = new FileInputStream(realmFile);
            Document doc = this.parseStreamToXML(inFile);
            inFile.close();
            Node rootElm = doc.getDocumentElement();
            for (int n = 0; n < rootElm.getChildNodes().getLength(); n++) {
                Node child = rootElm.getChildNodes().item(n);

                if ((child.getNodeType() == Node.ELEMENT_NODE)
                        && (child.getNodeName().equals(ELEM_USER))) {
                    String userName = null;
                    String password = null;
                    String roleList = null;
                    // Loop through for attributes
                    for (int j = 0; j < child.getAttributes().getLength(); j++) {
                        Node thisAtt = child.getAttributes().item(j);
                        if (thisAtt.getNodeName().equals(ATT_USERNAME))
                            userName = thisAtt.getNodeValue();
                        else if (thisAtt.getNodeName().equals(ATT_PASSWORD))
                            password = thisAtt.getNodeValue();
                        else if (thisAtt.getNodeName().equals(ATT_ROLELIST))
                            roleList = thisAtt.getNodeValue();
                    }

                    if ((userName == null) || (password == null)
                            || (roleList == null))
                        Logger.log(Logger.FULL_DEBUG, REALM_RESOURCES,
                                "FileRealm.SkippingUser", userName);
                    else {
                        // Parse the role list into an array and sort it
                        StringTokenizer st = new StringTokenizer(roleList, ",");
                        List rl = new ArrayList();
                        for (; st.hasMoreTokens();) {
                            String currentRole = st.nextToken();
                            if (rolesAllowed.contains(currentRole))
                                rl.add(currentRole);
                        }
                        Object roleArray[] = rl.toArray();
                        Arrays.sort(roleArray);
                        this.passwords.put(userName, password);
                        this.roles.put(userName, Arrays.asList(roleArray));
                    }
                }
            }
            Logger.log(Logger.DEBUG, REALM_RESOURCES, "FileRealm.Initialised",
                    "" + this.passwords.size());
        } catch (java.io.IOException err) {
            throw new WinstoneException(REALM_RESOURCES
                    .getString("FileRealm.ErrorLoading"), err);
        }
    }

    /**
     * Get a parsed XML DOM from the given inputstream. Used to process the
     * web.xml application deployment descriptors.
     */
    private Document parseStreamToXML(InputStream in) {
        try {
            // Use JAXP to create a document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setCoalescing(true);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (Throwable errParser) {
            throw new WinstoneException(REALM_RESOURCES
                    .getString("FileRealm.XMLParseError"), errParser);
        }
    }

    /**
     * Authenticate the user - do we know them ? Return a principal once we know
     * them
     */
    public AuthenticationPrincipal authenticateByUsernamePassword(
            String userName, String password) {
        if ((userName == null) || (password == null))
            return null;

        String realPassword = (String) this.passwords.get(userName);
        if (realPassword == null)
            return null;
        else if (!realPassword.equals(password))
            return null;
        else
            return new AuthenticationPrincipal(userName, password,
                    (List) this.roles.get(userName));
    }

    /**
     * Retrieve an authenticated user
     */
    public AuthenticationPrincipal retrieveUser(String userName) {
        return new AuthenticationPrincipal(userName, (String) this.passwords
                .get(userName), (List) this.roles.get(userName));
    }
}
