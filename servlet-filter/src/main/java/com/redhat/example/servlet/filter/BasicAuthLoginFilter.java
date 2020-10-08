package com.redhat.example.servlet.filter;

import java.io.IOException;
import java.security.Principal;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAuthLoginFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(BasicAuthLoginFilter.class);

    public static final String BASIC_AUTH_HEADER_NAME = "Authorization";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
        LOG.debug("----> init called");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest servletRequest = (HttpServletRequest) request;

        printHeaders(servletRequest);

        boolean loggedIn = false;

        String auth = servletRequest.getHeader("Authorization");
        if (auth != null) {
            LOG.debug("---> basic auth header = " + auth);
            String[] parts = auth.split(" ");
            if (parts.length == 2) {
                String userPass = new String(Base64.getDecoder().decode(parts[1]));
                LOG.debug("-----> user:pass = " + userPass);
                String[] userPassParts = userPass.split(":");
                if (userPassParts.length == 2) {
                    String userId = userPassParts[0];
                    String password = userPassParts[1];
                    Principal user = servletRequest.getUserPrincipal();
                    LOG.debug("----> user principal is " + user);
                    if (user == null || !userId.equals(user.getName())) {
                        LOG.debug("----> calling login for " + userId);
                        try {
                            servletRequest.login(userId, password);
                            loggedIn = true;
                        } catch (ServletException e) {
                            LOG.warn(e.getMessage());
                            loggedIn = false;
                        }
                    } else {
                        loggedIn = true;
                    }
                }
            }
        }
        else {
            LOG.debug("---> no basic auth header found");
        }
        
        chain.doFilter(request, response);

//        if (!loggedIn) {
//            // continue the chain if we cannot authenticate using basic auth header
//            chain.doFilter(request, response);
//        } else {
////            HttpServletResponse servletResponse = (HttpServletResponse) response;
//            LOG.debug("---> authenticated");
////            servletResponse.setStatus(HttpServletResponse.SC_OK);
//        }
    }

    private void printHeaders(HttpServletRequest servletRequest) {
        if (LOG.isDebugEnabled()) {
            List<String> headers = Collections.list(servletRequest.getHeaderNames());
            headers.forEach(header -> {
                List<String> values = Collections.list(servletRequest.getHeaders(header));
                values.forEach(value -> LOG.debug("---> header: " + header + " = " + value));
            });
        }
    }
}
