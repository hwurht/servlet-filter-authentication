package com.redhat.example.http.auth;

import static org.jboss.security.SecurityConstants.WEB_REQUEST_KEY;

import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;

import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;
import org.kie.api.task.UserGroupCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPHeaderLoginModule extends UsernamePasswordLoginModule {
    private static final Logger LOG = LoggerFactory.getLogger(HTTPHeaderLoginModule.class);

    public static final String PROPERTY_KEY_HEADER_USER_ID = "http.header.user.id";
    public static final String PROPERTY_KEY_HEADER_GROUPS = "http.header.groups";

    public static final String USER_ID_HEADER_NAME = System.getProperty(PROPERTY_KEY_HEADER_USER_ID, "SSO_USER_ID");
    public static final String GROUPS_HEADER_NAME = System.getProperty(PROPERTY_KEY_HEADER_GROUPS, "SSO_GROUPS");

    public static final String EXTRA_ROLES_USER_GROUP_CALLBACK_CLASS = "EXTRA_ROLES_USER_GROUP_CALLBACK_CLASS";

    private List<String> groupList = new ArrayList<>();
    private boolean hasValidHeaders = false;
    private String userId = null;
    private UserGroupCallback extraRolesClass = null;

    private static final String[] OPTIONS = { EXTRA_ROLES_USER_GROUP_CALLBACK_CLASS };

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        LOG.debug("---> [in war] initialize called");

        addValidOptions(OPTIONS);

        LOG.debug("---> added valid options");

        extraRolesClass = getExtraRolesClass(options);

        LOG.debug("---> got extra roles class");

        try {
            HttpServletRequest request = (HttpServletRequest) PolicyContext.getContext(WEB_REQUEST_KEY);
            printHeaders(request);

            if (hasValidHeaders(request)) {
                userId = request.getHeader(USER_ID_HEADER_NAME);
                LOG.debug("---> userId = " + userId);
                if (userId != null) {
                    LOG.debug("---> userId is not null");
                    sharedState.put("javax.security.auth.login.name", userId);
                    LOG.debug("---> added userId to shared state");
                    subject.getPrincipals().clear();
                    Principal principal = new SimplePrincipal(userId);
                    subject.getPrincipals().add(principal);
                    LOG.debug("---> added principal " + principal);

                    Enumeration<String> groups = request.getHeaders(GROUPS_HEADER_NAME);
                    LOG.debug("---> retrieve groups " + groups);
                    if (groups != null && groups.hasMoreElements()) {
                        groupList.addAll(Collections.list(groups));
                    }

                    hasValidHeaders = true;
                } else {
                    LOG.debug("---> user is null");
                    hasValidHeaders = false;
                }
            } else {
                LOG.debug("---> no valid headers found");
                hasValidHeaders = false;
            }
        } catch (PolicyContextException ex) {
            ex.printStackTrace();
        }

        super.initialize(subject, callbackHandler, sharedState, options);

        loginOk = hasValidHeaders;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        if (!hasValidHeaders) {
            return new Group[0];
        }

        SimpleGroup sr = new SimpleGroup("Roles");

        // AD roles
        if (groupList != null && !groupList.isEmpty()) {
            LOG.debug("---> has AD roles: " + groupList);

            for (String group : groupList) {
                LOG.debug("-----> adding AD group " + group);
                SimplePrincipal r = new SimplePrincipal(group);
                sr.addMember(r);
            }
        }

        // additional roles for usergroupcallback
        if (extraRolesClass != null) {
            List<String> additionalRoles = extraRolesClass.getGroupsForUser(userId);
            additionalRoles.forEach(role -> {
                LOG.debug("-----> adding additional role " + role);
                SimplePrincipal r = new SimplePrincipal(role);
                sr.addMember(r);
            });
        }

        Group[] roleSets = { sr };
        return roleSets;
    }

    private UserGroupCallback getExtraRolesClass(Map options) {
        LOG.debug("---> getExtraRolesClass called");

        String userGroupCallbackClass = (String) options.get(EXTRA_ROLES_USER_GROUP_CALLBACK_CLASS);
        
        LOG.debug("---> user group callback = " + userGroupCallbackClass);

        if (userGroupCallbackClass == null) {
            return null;
        }

        try {
            Class ugcbc = Class.forName(userGroupCallbackClass);
            LOG.debug("---> created class for " + userGroupCallbackClass);
            if (UserGroupCallback.class.isAssignableFrom(ugcbc)) {
                UserGroupCallback callback = (UserGroupCallback) ugcbc.newInstance();
                LOG.debug("---> extra roles class created for " + callback.getClass().getName());
                return callback;
            }
            LOG.debug("---> not assignable");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
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

    @Override
    protected String getUsersPassword() throws LoginException {
        return "";
    }

    @Override
    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        return hasValidHeaders;
    }
}
