package com.odin.multimedia.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(urlPatterns = "/*")
public class InvalidCharacterFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(InvalidCharacterFilter.class);

    // Regular expression for checking invalid characters in the URL or HTTP method
    private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("[^\\x20-\\x7E\\x09]");  // Non-printable ASCII characters

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Filter initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Check for invalid characters in the HTTP method (like spaces or control characters)
        System.out.println("request url : "+httpRequest.getRequestURL());
        System.out.println("request method : "+httpRequest.getMethod());
        String method = httpRequest.getMethod();
        if (INVALID_CHAR_PATTERN.matcher(method).find()) {
            logger.error("Invalid character found in HTTP method: {}", method);
        }

        // Check for invalid characters in the request URI (path and query)
        String uri = httpRequest.getRequestURI();
        if (INVALID_CHAR_PATTERN.matcher(uri).find()) {
            logger.error("Invalid character found in URI: {}", uri);
        }

        // Continue processing the request if no invalid characters are found
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
