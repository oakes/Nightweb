/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;

/**
 * This class implements both the RequestDispatcher and FilterChain components. On 
 * the first call to include() or forward(), it starts the filter chain execution
 * if one exists. On the final doFilter() or if there is no chain, we call the include()
 * or forward() again, and the servlet is executed.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: RequestDispatcher.java,v 1.18 2007/04/23 02:55:35 rickknowles Exp $
 */
public class RequestDispatcher implements javax.servlet.RequestDispatcher,
        javax.servlet.FilterChain {
    
    static final String INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";
    static final String INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";
    static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
    static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
    static final String INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

    static final String FORWARD_REQUEST_URI = "javax.servlet.forward.request_uri";
    static final String FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";
    static final String FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
    static final String FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
    static final String FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";

    static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";
    static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
    static final String ERROR_MESSAGE = "javax.servlet.error.message";
    static final String ERROR_EXCEPTION = "javax.servlet.error.exception";
    static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
    static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";
    
    private WebAppConfiguration webAppConfig;
    private ServletConfiguration servletConfig;
    
    private String servletPath;
    private String pathInfo;
    private String queryString;
    private String requestURI;
    
    private Integer errorStatusCode;
    private Throwable errorException;
    private String errorSummaryMessage;
    
    private AuthenticationHandler authHandler;
    
    private Mapping forwardFilterPatterns[];
    private Mapping includeFilterPatterns[];
    private FilterConfiguration matchingFilters[];
    private int matchingFiltersEvaluated;
    
    private Boolean doInclude;
    private boolean isErrorDispatch;
    private boolean useRequestAttributes;
    
    private WebAppConfiguration includedWebAppConfig;
    private ServletConfiguration includedServletConfig;
    
    /**
     * Constructor. This initializes the filter chain and sets up the details
     * needed to handle a servlet excecution, such as security constraints,
     * filters, etc.
     */
    public RequestDispatcher(WebAppConfiguration webAppConfig, ServletConfiguration servletConfig) {
        this.servletConfig = servletConfig;
        this.webAppConfig = webAppConfig;

        this.matchingFiltersEvaluated = 0;
    }

    public void setForNamedDispatcher(Mapping forwardFilterPatterns[],
            Mapping includeFilterPatterns[]) {
        this.forwardFilterPatterns = forwardFilterPatterns;
        this.includeFilterPatterns = includeFilterPatterns;
        this.matchingFilters = null; // set after the call to forward or include
        this.useRequestAttributes = false;
        this.isErrorDispatch = false;
    }

    public void setForURLDispatcher(String servletPath, String pathInfo,
            String queryString, String requestURIInsideWebapp,
            Mapping forwardFilterPatterns[], Mapping includeFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.requestURI = requestURIInsideWebapp;

        this.forwardFilterPatterns = forwardFilterPatterns;
        this.includeFilterPatterns = includeFilterPatterns;
        this.matchingFilters = null; // set after the call to forward or include
        this.useRequestAttributes = true;
        this.isErrorDispatch = false;
    }

    public void setForErrorDispatcher(String servletPath, String pathInfo,
            String queryString, int statusCode, String summaryMessage, 
            Throwable exception, String errorHandlerURI, 
            Mapping errorFilterPatterns[]) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.requestURI = errorHandlerURI;

        this.errorStatusCode = new Integer(statusCode);
        this.errorException = exception;
        this.errorSummaryMessage = summaryMessage;
        this.matchingFilters = getMatchingFilters(errorFilterPatterns, this.webAppConfig, 
                servletPath + (pathInfo == null ? "" : pathInfo), 
                getName(), "ERROR", (servletPath != null));
        this.useRequestAttributes = true;
        this.isErrorDispatch = true;
    }

    public void setForInitialDispatcher(String servletPath, String pathInfo, 
            String queryString, String requestURIInsideWebapp, Mapping requestFilterPatterns[],
            AuthenticationHandler authHandler) {
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
        this.requestURI = requestURIInsideWebapp;
        this.authHandler = authHandler;
        this.matchingFilters = getMatchingFilters(requestFilterPatterns, this.webAppConfig, 
                servletPath + (pathInfo == null ? "" : pathInfo), 
                getName(), "REQUEST", (servletPath != null));
        this.useRequestAttributes = false;
        this.isErrorDispatch = false;
    }

    public String getName() {
        return this.servletConfig.getServletName();
    }

    /**
     * Includes the execution of a servlet into the current request
     * 
     * Note this method enters itself twice: once with the initial call, and once again 
     * when all the filters have completed.
     */
    public void include(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {

        // On the first call, log and initialise the filter chain
        if (this.doInclude == null) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                    "RequestDispatcher.IncludeMessage", new String[] {
                            getName(), this.requestURI });
            
            WinstoneRequest wr = getUnwrappedRequest(request);
            // Add the query string to the included query string stack
            wr.addIncludeQueryParameters(this.queryString);
            
            // Set request attributes
            if (useRequestAttributes) {
                wr.addIncludeAttributes(this.webAppConfig.getContextPath() + this.requestURI, 
                        this.webAppConfig.getContextPath(), this.servletPath, this.pathInfo, this.queryString);
            }
            // Add another include buffer to the response stack
            WinstoneResponse wresp = getUnwrappedResponse(response);
            wresp.startIncludeBuffer();
            
            this.includedServletConfig = wr.getServletConfig();
            this.includedWebAppConfig = wr.getWebAppConfig();
            wr.setServletConfig(this.servletConfig);
            wr.setWebAppConfig(this.webAppConfig);
            wresp.setWebAppConfig(this.webAppConfig);
            
            this.doInclude = Boolean.TRUE;
        }
        
        if (this.matchingFilters == null) {
            this.matchingFilters = getMatchingFilters(this.includeFilterPatterns, this.webAppConfig, 
                    this.servletPath + (this.pathInfo == null ? "" : this.pathInfo), 
                    getName(), "INCLUDE", (this.servletPath != null));
        }
        try {
            // Make sure the filter chain is exhausted first
            if (this.matchingFiltersEvaluated < this.matchingFilters.length) {
                doFilter(request, response);
                finishInclude(request, response);
            } else {
                try {
                    this.servletConfig.execute(request, response, 
                            this.webAppConfig.getContextPath() + this.requestURI);
                } finally {
                    if (this.matchingFilters.length == 0) {
                        finishInclude(request, response);
                    }
                }
            }
        } catch (Throwable err) {
            finishInclude(request, response);
            if (err instanceof ServletException) {
                throw (ServletException) err;
            } else if (err instanceof IOException) {
                throw (IOException) err;
            } else if (err instanceof Error) {
                throw (Error) err;
            } else {
                throw (RuntimeException) err;
            }
        }
    }

    private void finishInclude(ServletRequest request, ServletResponse response) 
            throws IOException {
        WinstoneRequest wr = getUnwrappedRequest(request);
        wr.removeIncludeQueryString();
        
        // Set request attributes
        if (useRequestAttributes) {
            wr.removeIncludeAttributes();
        }
        // Remove the include buffer from the response stack
        WinstoneResponse wresp = getUnwrappedResponse(response);
        wresp.finishIncludeBuffer();
        
        if (this.includedServletConfig != null) {
            wr.setServletConfig(this.includedServletConfig);
            this.includedServletConfig = null;
        }
        
        if (this.includedWebAppConfig != null) {
            wr.setWebAppConfig(this.includedWebAppConfig);
            wresp.setWebAppConfig(this.includedWebAppConfig);
            this.includedWebAppConfig = null;
        }
    }
    
    /**
     * Forwards to another servlet, and when it's finished executing that other
     * servlet, cut off execution.
     * 
     * Note this method enters itself twice: once with the initial call, and once again 
     * when all the filters have completed.
     */
    public void forward(ServletRequest request, ServletResponse response) 
            throws ServletException, IOException {

        // Only on the first call to forward, we should set any forwarding attributes
        if (this.doInclude == null) {
            Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                    "RequestDispatcher.ForwardMessage", new String[] {
                    getName(), this.requestURI });
            if (response.isCommitted()) {
                throw new IllegalStateException(Launcher.RESOURCES.getString(
                        "RequestDispatcher.ForwardCommitted"));
            }
            
            WinstoneRequest req = getUnwrappedRequest(request);
            WinstoneResponse rsp = getUnwrappedResponse(response);
            
            // Clear the include stack if one has been accumulated
            rsp.resetBuffer();
            req.clearIncludeStackForForward();
            rsp.clearIncludeStackForForward();
            
            // Set request attributes (because it's the first step in the filter chain of a forward or error)
            if (useRequestAttributes) {
                req.setAttribute(FORWARD_REQUEST_URI, req.getRequestURI());
                req.setAttribute(FORWARD_CONTEXT_PATH, req.getContextPath());
                req.setAttribute(FORWARD_SERVLET_PATH, req.getServletPath());
                req.setAttribute(FORWARD_PATH_INFO, req.getPathInfo());
                req.setAttribute(FORWARD_QUERY_STRING, req.getQueryString());
                
                if (this.isErrorDispatch) {
                    req.setAttribute(ERROR_REQUEST_URI, req.getRequestURI());
                    req.setAttribute(ERROR_STATUS_CODE, this.errorStatusCode);
                    req.setAttribute(ERROR_MESSAGE, 
                            errorSummaryMessage != null ? errorSummaryMessage : "");
                    if (req.getServletConfig() != null) {
                        req.setAttribute(ERROR_SERVLET_NAME, req.getServletConfig().getServletName());
                    }
                    
                    if (this.errorException != null) {
                        req.setAttribute(ERROR_EXCEPTION_TYPE, this.errorException.getClass());
                        req.setAttribute(ERROR_EXCEPTION, this.errorException);
                    }
                    
                    // Revert back to the original request and response
                    rsp.setErrorStatusCode(this.errorStatusCode.intValue());
                    request = req;
                    response = rsp;
                }
            }
            
            req.setServletPath(this.servletPath);
            req.setPathInfo(this.pathInfo);
            req.setRequestURI(this.webAppConfig.getContextPath() + this.requestURI);
            req.setForwardQueryString(this.queryString);
            req.setWebAppConfig(this.webAppConfig);
            req.setServletConfig(this.servletConfig);
            req.setRequestAttributeListeners(this.webAppConfig.getRequestAttributeListeners());
            
            rsp.setWebAppConfig(this.webAppConfig);

            // Forwards haven't set up the filter pattern set yet
            if (this.matchingFilters == null) {
                this.matchingFilters = getMatchingFilters(this.forwardFilterPatterns, this.webAppConfig, 
                        this.servletPath + (this.pathInfo == null ? "" : this.pathInfo), 
                        getName(), "FORWARD", (this.servletPath != null));
            }
            
            // Otherwise we are an initial or error dispatcher, so check security if initial -
            // if we should not continue, return
            else if (!this.isErrorDispatch && !continueAfterSecurityCheck(request, response)) {
                return;
            }
            
            this.doInclude = Boolean.FALSE;
        }

        // Make sure the filter chain is exhausted first
        boolean outsideFilter = (this.matchingFiltersEvaluated == 0);
        if (this.matchingFiltersEvaluated < this.matchingFilters.length) {
            doFilter(request, response);
        } else {
            this.servletConfig.execute(request, response, this.webAppConfig.getContextPath() + this.requestURI);
        }
        // Stop any output after the final filter has been executed (e.g. from forwarding servlet)
        if (outsideFilter) {
            WinstoneResponse rsp = getUnwrappedResponse(response);
            rsp.flushBuffer();
            rsp.getWinstoneOutputStream().setClosed(true);
        }
    }

    private boolean continueAfterSecurityCheck(ServletRequest request,
            ServletResponse response) throws IOException, ServletException {
        // Evaluate security constraints
        if (this.authHandler != null) {
            return this.authHandler.processAuthentication(request, response,
                    this.servletPath + (this.pathInfo == null ? "" : this.pathInfo));
        } else {
            return true;
        }
    }

    /**
     * Handles the processing of the chain of filters, so that we process them
     * all, then pass on to the main servlet
     */
    public void doFilter(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        // Loop through the filter mappings until we hit the end
        while (this.matchingFiltersEvaluated < this.matchingFilters.length) {
            
            FilterConfiguration filter = this.matchingFilters[this.matchingFiltersEvaluated++]; 
            Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                    "RequestDispatcher.ExecutingFilter", filter.getFilterName());
            filter.execute(request, response, this);
            return;
        }

        // Forward / include as requested in the beginning
        if (this.doInclude == null)
            return; // will never happen, because we can't call doFilter before forward/include
        else if (this.doInclude.booleanValue())
            include(request, response);
        else
            forward(request, response);
    }

    /**
     * Caches the filter matching, so that if the same URL is requested twice, we don't recalculate the
     * filter matching every time. 
     */
    private static FilterConfiguration[] getMatchingFilters(Mapping filterPatterns[], 
            WebAppConfiguration webAppConfig, String fullPath, String servletName,
            String filterChainType, boolean isURLBasedMatch) {
        
        String cacheKey = null;
        if (isURLBasedMatch) {
            cacheKey = filterChainType + ":URI:" + fullPath;
        } else {
            cacheKey = filterChainType + ":Servlet:" + servletName;
        }
        FilterConfiguration matchingFilters[] = null;
        Map cache = webAppConfig.getFilterMatchCache();
        synchronized (cache) {
            matchingFilters = (FilterConfiguration []) cache.get(cacheKey); 
            if (matchingFilters == null) {
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, 
                        "RequestDispatcher.CalcFilterChain", cacheKey);
                List outFilters = new ArrayList();
                for (int n = 0; n < filterPatterns.length; n++) {
                    // Get the pattern and eval it, bumping up the eval'd count
                    Mapping filterPattern = filterPatterns[n];

                    // If the servlet name matches this name, execute it
                    if ((filterPattern.getLinkName() != null)
                            && (filterPattern.getLinkName().equals(servletName) ||
                                    filterPattern.getLinkName().equals("*"))) {
                        outFilters.add(webAppConfig.getFilters().get(filterPattern.getMappedTo()));
                    }
                    // If the url path matches this filters mappings
                    else if ((filterPattern.getLinkName() == null) && isURLBasedMatch
                            && filterPattern.match(fullPath, null, null)) {
                        outFilters.add(webAppConfig.getFilters().get(filterPattern.getMappedTo()));
                    }
                }
                matchingFilters = (FilterConfiguration []) outFilters.toArray(new FilterConfiguration[0]);
                cache.put(cacheKey, matchingFilters);
            } else {
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, 
                        "RequestDispatcher.UseCachedFilterChain", cacheKey);
            }
        }
        return matchingFilters;
    }
    
    /**
     * Unwrap back to the original container allocated request object
     */
    protected WinstoneRequest getUnwrappedRequest(ServletRequest request) {
        ServletRequest workingRequest = request;
        while (workingRequest instanceof ServletRequestWrapper) {
            workingRequest = ((ServletRequestWrapper) workingRequest).getRequest();
        }
        return (WinstoneRequest) workingRequest;
    }

    /**
     * Unwrap back to the original container allocated response object
     */
    protected WinstoneResponse getUnwrappedResponse(ServletResponse response) {
        ServletResponse workingResponse = response;
        while (workingResponse instanceof ServletResponseWrapper) {
            workingResponse = ((ServletResponseWrapper) workingResponse).getResponse();
        }
        return (WinstoneResponse) workingResponse;
    }
}
