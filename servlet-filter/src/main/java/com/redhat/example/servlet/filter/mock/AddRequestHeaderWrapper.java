package com.redhat.example.servlet.filter.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class AddRequestHeaderWrapper extends HttpServletRequestWrapper {
    private Map<String, List<String>> headerMap = new HashMap<>();

    public AddRequestHeaderWrapper(HttpServletRequest request) {
        super(request);
    }

    public void addHeader(String headerName, String value) {
        if (headerMap.containsKey(headerName)) {
            headerMap.get(headerName).add(value);
        } else {
            List<String> valueList = new ArrayList<>();
            valueList.add(value);
            headerMap.put(headerName, valueList);
        }
    }

    @Override
    public String getHeader(String name) {
        if (headerMap.containsKey(name)) {
            return headerMap.get(name).get(0);
        } else {
            return super.getHeader(name);
        }
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        headerMap.keySet().forEach(k -> {
            if (!names.contains(k)) {
                names.add(k);
            }
        });
        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (headerMap.containsKey(name)) {
            return Collections.enumeration(headerMap.get(name));

        }
        return super.getHeaders(name);
    }

}
