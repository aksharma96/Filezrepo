package utils;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 
 * @author akshits
 * 
 */
public class RequestResponseFilter implements Filter {

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
    	HttpServletResponse res = (HttpServletResponse) response;
        MyResponseRequestWrapper responseWrapper = new MyResponseRequestWrapper(res);
        responseWrapper.addHeader("X-Frame-Options", "deny");
        responseWrapper.addHeader("Cache-Control", "no-cache, no-store");
        responseWrapper.addHeader("Expires", "0");
        responseWrapper.addHeader("Pragma", "no-cache");
       
        responseWrapper.addHeader("Content-Security-Policy", "default-src 'none'");
        chain.doFilter(request, responseWrapper); 
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

   
    
    
    public class MyResponseRequestWrapper extends HttpServletResponseWrapper{
        public MyResponseRequestWrapper(HttpServletResponse response) {
            super(response);
        }
    }
    /**
     * allow adding additional header entries to a request
     * 
     * @author akshits
     * 
     */
    public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        /**
         * construct a wrapper for this request
         * 
         * @param request
         */
        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

      
        }

}
