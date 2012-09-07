/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * The web.xml parsing logic. This is used by more than one launcher, so it's shared from here. 
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WebXmlParser.java,v 1.9 2006/12/08 04:08:44 rickknowles Exp $
 */
public class WebXmlParser implements EntityResolver, ErrorHandler {

    private ClassLoader commonLoader;
    private boolean rethrowValidationExceptions;

    public WebXmlParser(ClassLoader commonCL) {
        this.commonLoader = commonCL;
        this.rethrowValidationExceptions = true;
    }
    
    private final static String SCHEMA_SOURCE_PROPERTY = "http://java.sun.com/xml/jaxp/properties/schemaSource"; 
    
    /**
     * Get a parsed XML DOM from the given inputstream. Used to process the
     * web.xml application deployment descriptors. Returns null if the parse fails,
     * so the effect is as if there was no web.xml file available.
     */
    protected Document parseStreamToXML(File webXmlFile) {
        DocumentBuilderFactory factory = getBaseDBF();
        
        URL localXSD25 = this.commonLoader.getResource(LOCAL_ENTITY_TABLE[3][2]);
        URL localXSD24 = this.commonLoader.getResource(LOCAL_ENTITY_TABLE[2][2]);
        
        // Test for XSD compliance
        try {
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                    "http://www.w3.org/2001/XMLSchema");
            if (localXSD25 != null) {
                factory.setAttribute(SCHEMA_SOURCE_PROPERTY, localXSD25.toString());
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WebXmlParser.Local25XSDEnabled");
            } else if (localXSD24 != null) {
                factory.setAttribute(SCHEMA_SOURCE_PROPERTY, localXSD24.toString());
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WebXmlParser.Local24XSDEnabled");
            } else {
                Logger.log(Logger.WARNING, Launcher.RESOURCES, "WebXmlParser.2524XSDNotFound");
            }
        } catch (Throwable err) {
            // if non-compliant parser, then parse as non-XSD compliant
            Logger.log(Logger.WARNING, Launcher.RESOURCES, "WebXmlParser.NonXSDParser");
            try {
                this.rethrowValidationExceptions = false;
                return parseAsV23Webapp(webXmlFile);
            } catch (Throwable v23Err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebXmlParser.WebXML23ParseError", v23Err);
                return null;
            }
        }
        
        // XSD compliant parser available, so parse as 2.5
        try {
            if (localXSD25 != null) {
                factory.setAttribute(SCHEMA_SOURCE_PROPERTY, localXSD25.toString());
            } else {
                factory.setAttribute(SCHEMA_SOURCE_PROPERTY, null);
            }
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(this);
            builder.setErrorHandler(this);
            this.rethrowValidationExceptions = true;
            return builder.parse(webXmlFile);
        } catch (Throwable errV25) {
            try {
                // Try as 2.4
                if (localXSD24 != null) {
                    factory.setAttribute(SCHEMA_SOURCE_PROPERTY, localXSD24.toString());
                } else {
                    factory.setAttribute(SCHEMA_SOURCE_PROPERTY, null);
                }
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.setEntityResolver(this);
                builder.setErrorHandler(this);
                this.rethrowValidationExceptions = true;
                return builder.parse(webXmlFile);
            } catch (Throwable errV24) {
                // Try parsing as a v2.3 spec webapp, and if another error happens, report 2.3, 2.4, 2.5
                try {
                    this.rethrowValidationExceptions = false;
                    return parseAsV23Webapp(webXmlFile);
                } catch (Throwable errV23) {
                    Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebXmlParser.WebXMLBothErrors");
                    Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebXmlParser.WebXML25ParseError", errV25);
                    Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebXmlParser.WebXML24ParseError", errV24);
                    Logger.log(Logger.ERROR, Launcher.RESOURCES, "WebXmlParser.WebXML23ParseError", errV23);
                    return null;
                }
            }
        }
    }
    
    private Document parseAsV23Webapp(File webXmlFile) throws ParserConfigurationException, 
            SAXException, IOException {
        DocumentBuilderFactory factory = getBaseDBF();
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(this);
        builder.setErrorHandler(this);
        return builder.parse(webXmlFile);
    }
    
    private DocumentBuilderFactory getBaseDBF() {
        // Use JAXP to create a document builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        factory.setIgnoringComments(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        return factory;
    }

    /**
     * Table mapping public doctypes and system ids against local classloader paths. This
     * is used to resolve local entities where possible. 
     * Column 0 = public doctype
     * Column 1 = system id
     * Column 2 = local path
     */
    private static final String LOCAL_ENTITY_TABLE[][] = {
        {"-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN", null, "javax/servlet/resources/web-app_2_2.dtd"},
        {"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN", null, "javax/servlet/resources/web-app_2_3.dtd"},
        {null, "http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd", "javax/servlet/resources/web-app_2_4.xsd"},
        {null, "http://java.sun.com/xml/ns/j2ee/web-app_2_5.xsd", "javax/servlet/resources/web-app_2_5.xsd"},
        {null, "http://www.w3.org/2001/xml.xsd", "javax/servlet/resources/xml.xsd"},
        {"-//W3C//DTD XMLSCHEMA 200102//EN", null, "javax/servlet/resources/XMLSchema.dtd"},
        {null, "http://www.w3.org/2001/datatypes.dtd", "javax/servlet/resources/datatypes.dtd"},
        {null, "http://java.sun.com/xml/ns/j2ee/j2ee_1_4.xsd", "javax/servlet/resources/j2ee_1_4.xsd"},
        {null, "http://java.sun.com/xml/ns/j2ee/javaee_5.xsd", "javax/servlet/resources/javaee_5.xsd"},
        {null, "http://java.sun.com/xml/ns/j2ee/jsp_2_0.xsd", "javax/servlet/resources/jsp_2_0.xsd"},
        {null, "http://java.sun.com/xml/ns/j2ee/jsp_2_1.xsd", "javax/servlet/resources/jsp_2_1.xsd"},
        {null, "http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_1.xsd", "javax/servlet/resources/j2ee_web_services_client_1_1.xsd"},
        {null, "http://www.ibm.com/webservices/xsd/j2ee_web_services_client_1_2.xsd", "javax/servlet/resources/javaee_web_services_client_1_2.xsd"}
    };
    
    /**
     * Implements the EntityResolver interface. This allows us to redirect any
     * requests by the parser for webapp DTDs to local copies. It's faster and
     * it means you can run winstone without being web-connected.
     */
    public InputSource resolveEntity(String publicName, String url)
            throws SAXException, IOException {
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WebXmlParser.ResolvingEntity",
                new String[] { publicName, url });
        for (int n = 0; n < LOCAL_ENTITY_TABLE.length; n++) {
            if (((LOCAL_ENTITY_TABLE[n][0] != null) && (publicName != null) && 
                        publicName.equals(LOCAL_ENTITY_TABLE[n][0])) ||
                    ((LOCAL_ENTITY_TABLE[n][1] != null) && (url != null) && 
                            url.equals(LOCAL_ENTITY_TABLE[n][1]))) {
                if (this.commonLoader.getResource(LOCAL_ENTITY_TABLE[n][2]) != null) {
                    return getLocalResource(url, LOCAL_ENTITY_TABLE[n][2]);
                }
            }
        }
        if ((url != null) && url.startsWith("jar:")) {
            return getLocalResource(url, url.substring(url.indexOf("!/") + 2));
        } else if ((url != null) && url.startsWith("file:")) {
            return new InputSource(url);
        } else {
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "WebXmlParser.NoLocalResource", url);
            return new InputSource(url);
        }
    }

    private InputSource getLocalResource(String url, String local) {
        if (this.commonLoader.getResource(local) == null)
            return new InputSource(url);
        InputSource is = new InputSource(this.commonLoader.getResourceAsStream(local));
        is.setSystemId(url);
        return is;
    }

    public void error(SAXParseException exception) throws SAXException {
        if (this.rethrowValidationExceptions) {
            throw exception;
        } else {
            Logger.log(Logger.WARNING, Launcher.RESOURCES, "WebXmlParser.XMLParseError",
                    new String[] { exception.getLineNumber() + "",
                            exception.getMessage() });
        }
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        error(exception);
    }

    public void warning(SAXParseException exception) throws SAXException {
        Logger.log(Logger.WARNING, Launcher.RESOURCES, "WebXmlParser.XMLParseError",
                new String[] { exception.getLineNumber() + "",
                        exception.getMessage() });
    }
}
