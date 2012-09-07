/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

/**
 * Encapsulates the parsing of URL patterns, as well as the mapping of a 
 * url pattern to a servlet instance
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Mapping.java,v 1.9 2007/04/23 02:55:35 rickknowles Exp $
 */
public class Mapping implements java.util.Comparator {
    public static final int EXACT_PATTERN = 1;
    public static final int FOLDER_PATTERN = 2;
    public static final int EXTENSION_PATTERN = 3;
    public static final int DEFAULT_SERVLET = 4;

    public static final String STAR = "*";
    public static final String SLASH = "/";

    private String urlPattern;
    private String linkName; // used to map filters to a specific servlet by
                             // name
    private String mappedTo;
    private int patternType;
    private boolean isPatternFirst; // ie is this a blah* pattern, not *blah
                                    // (extensions only)

    protected Mapping(String mappedTo) {
        this.mappedTo = mappedTo;
    }

    /**
     * Factory constructor method - this parses the url pattern into pieces we can use to match
     * against incoming URLs.
     */
    public static Mapping createFromURL(String mappedTo, String pattern) {
        if ((pattern == null) || (mappedTo == null))
            throw new WinstoneException(Launcher.RESOURCES.getString(
                    "Mapping.InvalidMount", new String[] { mappedTo, pattern }));
        
        // Compatibility hacks - add a leading slash if one is not found and not 
        // an extension mapping
        if (!pattern.equals("") && !pattern.startsWith(STAR) && 
                !pattern.startsWith(SLASH)) {
            pattern = SLASH + pattern;
        } else if (pattern.equals(STAR)) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES, "Mapping.RewritingStarMount");
            pattern = SLASH + STAR;
        }
        
        Mapping me = new Mapping(mappedTo);

        int firstStarPos = pattern.indexOf(STAR);
        int lastStarPos = pattern.lastIndexOf(STAR);
        int patternLength = pattern.length();

        // check for default servlet, ie mapping = exactly /
        if (pattern.equals(SLASH)) {
            me.urlPattern = "";
            me.patternType = DEFAULT_SERVLET;
        }

        else if (firstStarPos == -1) {
            me.urlPattern = pattern;
            me.patternType = EXACT_PATTERN;
        }

        // > 1 star = error
        else if (firstStarPos != lastStarPos)
            throw new WinstoneException(Launcher.RESOURCES.getString(
                    "Mapping.InvalidMount", new String[] { mappedTo, pattern }));

        // check for folder style mapping (ends in /*)
        else  if (pattern.indexOf(SLASH + STAR) == (patternLength - (SLASH + STAR).length())) {
            me.urlPattern = pattern.substring(0, pattern.length()
                    - (SLASH + STAR).length());
            me.patternType = FOLDER_PATTERN;
        }
        
        // check for non-extension match
        else if (pattern.indexOf(SLASH) != -1)
            throw new WinstoneException(Launcher.RESOURCES.getString(
                    "Mapping.InvalidMount", new String[] { mappedTo, pattern }));

        // check for extension match at the beginning (eg *blah)
        else if (firstStarPos == 0) {
            me.urlPattern = pattern.substring(STAR.length());
            me.patternType = EXTENSION_PATTERN;
            me.isPatternFirst = false;
        }
        // check for extension match at the end (eg blah*)
        else if (firstStarPos == (patternLength - STAR.length())) {
            me.urlPattern = pattern.substring(0, patternLength - STAR.length());
            me.patternType = EXTENSION_PATTERN;
            me.isPatternFirst = true;
        } else
            throw new WinstoneException(Launcher.RESOURCES.getString(
                    "Mapping.InvalidMount", new String[] { mappedTo, pattern }));

        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "Mapping.MappedPattern",
                new String[] { mappedTo, pattern });
        return me;
    }

    /**
     * Factory constructor method - this turns a servlet name into a mapping element
     */
    public static Mapping createFromLink(String mappedTo, String linkName) {
        if ((linkName == null) || (mappedTo == null))
            throw new WinstoneException(Launcher.RESOURCES.getString(
                    "Mapping.InvalidLink", new String[] { mappedTo, linkName }));

        Mapping me = new Mapping(mappedTo);
        me.linkName = linkName;
        return me;
    }

    public int getPatternType() {
        return this.patternType;
    }

    public String getUrlPattern() {
        return this.urlPattern;
    }

    public String getMappedTo() {
        return this.mappedTo;
    }

    public String getLinkName() {
        return this.linkName;
    }

    /**
     * Try to match this pattern against the incoming url
     * 
     * @param inputPattern The URL we want to check for a match
     * @param servletPath An empty stringbuffer for the servletPath of a successful match
     * @param pathInfo An empty stringbuffer for the pathInfo of a successful match
     * @return true if the match is successful
     */
    public boolean match(String inputPattern, StringBuffer servletPath,
            StringBuffer pathInfo) {
        switch (this.patternType) {
        case FOLDER_PATTERN:
            if (inputPattern.startsWith(this.urlPattern + '/') || 
                    inputPattern.equals(this.urlPattern)) {
                if (servletPath != null)
                    servletPath.append(WinstoneRequest.decodeURLToken(this.urlPattern));
                if (pathInfo != null)
                    pathInfo.append(WinstoneRequest.decodeURLToken(inputPattern.substring(this.urlPattern.length())));
                return true;
            } else
                return false;

        case EXTENSION_PATTERN:
            // Strip down to the last item in the path
            int slashPos = inputPattern.lastIndexOf(SLASH);
            if ((slashPos == -1) || (slashPos == inputPattern.length() - 1))
                return false;
            String fileName = inputPattern.substring(slashPos + 1);
            if ((this.isPatternFirst && fileName.startsWith(this.urlPattern))
                    || (!this.isPatternFirst && fileName.endsWith(this.urlPattern))) {
                if (servletPath != null)
                    servletPath.append(WinstoneRequest.decodeURLToken(inputPattern));
                return true;
            } else
                return false;

        case EXACT_PATTERN:
            if (inputPattern.equals(this.urlPattern)) {
                if (servletPath != null)
                    servletPath.append(WinstoneRequest.decodeURLToken(inputPattern));
                return true;
            } else
                return false;

        case DEFAULT_SERVLET:
            if (servletPath != null)
                servletPath.append(WinstoneRequest.decodeURLToken(inputPattern));
            return true;

        default:
            return false;
        }
    }

    /**
     * Used to compare two url patterns. Always sorts so that lowest pattern
     * type then longest path come first.
     */
    public int compare(Object objOne, Object objTwo) {
        Mapping one = (Mapping) objOne;
        Mapping two = (Mapping) objTwo;

        Integer intOne = new Integer(one.getPatternType());
        Integer intTwo = new Integer(two.getPatternType());
        int order = -1 * intOne.compareTo(intTwo);
        if (order != 0) {
            return order;
        }
        if (one.getLinkName() != null) {
            // servlet name mapping - just alphabetical sort
            return one.getLinkName().compareTo(two.getLinkName());
        } else {
            return -1 * one.getUrlPattern().compareTo(two.getUrlPattern());
        }
    }

    public String toString() {
        return this.linkName != null ? "Link:" + this.linkName
                : "URLPattern:type=" + this.patternType + ",pattern="
                        + this.urlPattern;
    }
}
