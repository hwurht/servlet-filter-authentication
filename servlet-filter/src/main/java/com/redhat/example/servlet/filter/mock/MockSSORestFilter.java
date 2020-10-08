package com.redhat.example.servlet.filter.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockSSORestFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(MockSSORestFilter.class);
    private static final String PROPERTY_FILE_KEY = "custom.sso.request.header.file";
    
    private Map<String, List<String>> headerValueMap = new HashMap<>();
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
        LOG.debug("----> init called");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        loadHeaders();
        LOG.debug("----> doFilter called");
        HttpServletRequest servletRequest = (HttpServletRequest)request;
        AddRequestHeaderWrapper wrapper = new AddRequestHeaderWrapper(servletRequest);
        headerValueMap.forEach((k, v) -> v.forEach(vv -> wrapper.addHeader(k, vv)));
        LOG.debug("----> added headers");
        chain.doFilter(wrapper, response);
    }
    
    private void loadHeaders() {
        String file = System.getProperty(PROPERTY_FILE_KEY);
        LOG.debug("----> loading request header values from " + file);
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(new File(file))) {
            props.load(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        headerValueMap.clear();
        
        for (Object key : props.keySet()) {
            String header = key.toString();
            LOG.debug("----> found header " + header);
            Object value = props.get(key);
            if (value != null) {
                String values = value.toString();
                LOG.debug("-------> found values " + values);
                String[] parts = values.split(",");
                headerValueMap.put(header, Arrays.asList(parts));
            }
        }
        LOG.debug("----> final map = " + headerValueMap);
    }

}
