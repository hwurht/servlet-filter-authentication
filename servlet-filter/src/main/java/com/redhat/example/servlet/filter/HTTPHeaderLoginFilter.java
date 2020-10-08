package com.redhat.example.servlet.filter;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class HTTPHeaderLoginFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(HTTPHeaderLoginFilter.class);

    public static final String PROPERTY_KEY_HEADER_USER_ID = "http.header.user.id";
    public static final String PROPERTY_KEY_HEADER_GROUPS = "http.header.groups";

    public static final String USER_ID_HEADER_NAME = System.getProperty(PROPERTY_KEY_HEADER_USER_ID, "SSO_USER_ID");
    public static final String GROUPS_HEADER_NAME = System.getProperty(PROPERTY_KEY_HEADER_GROUPS, "SSO_GROUPS");

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

        if (hasValidHeaders(servletRequest)) {
            LOG.debug("----> has valid headers");
            String userId = servletRequest.getHeader(USER_ID_HEADER_NAME);
            if (userId != null) {
                Principal user = servletRequest.getUserPrincipal();
                LOG.debug("----> user principal is " + user);
                if (user == null || !userId.equals(user.getName())) {
                    LOG.debug("----> calling login for " + userId);
                    try {
                        servletRequest.login(userId, "NONE");
                        loggedIn = true;
                    } catch (ServletException e) {
                        LOG.warn(e.getMessage());
                        loggedIn = false;
                    }
                }
                else {
                    loggedIn = true;
                }
            }
        } 

        if (loggedIn) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse servletResponse = (HttpServletResponse) response;
            LOG.debug("---> not authenticated");
            servletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
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

    private boolean hasValidHeaders(HttpServletRequest servletRequest) {
        List<String> headers = Collections.list(servletRequest.getHeaderNames());
        Map<String, Boolean> validHeaderMap = new HashMap<>();
        validHeaderMap.put(USER_ID_HEADER_NAME, false);
        validHeaderMap.put(GROUPS_HEADER_NAME, false);

        headers.forEach(header -> {
            if (validHeaderMap.containsKey(header)) {
                validHeaderMap.put(header, true);
            }
        });

        return !validHeaderMap.containsValue(false);
    }
}
