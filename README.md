# Using Servlet Login for Red Hat Process Automation Manager's Process Server
## Overview
Out of the box the process server (kie-server) in RHPAM in conjuction with JBoss EAP will use container security directly.  The problem is this authentication happens before any servlet filters are invoked.  With SSO plugins that use the servlet filter mechanism for user verification, we need to use an alternative approach.  Here are the major changes of this approach.

1. Security constraints roles are turn off in the web.xml to disable triggling of RBAC.
2. Login config is disabled in the web.xml to disable the security mechanism.
3. Add appropriate SSO servlet filter as first filter in the chain and mapped to **`/services/rest/*`**.
4. Add custom login filter as second filter in the chain and mapped to **`/services/rest/*`**.
5. Add new **`security-domain`** to the JBoss configuration file to load the custom login module.
6. Custom login module validates the HTTP request headers added by the SSO filter and map the roles into the JACC PolicyContext.
7. Custom login module also load any passed in UserGroupCallback implementations to retrieve additional roles for user tasks.

There are 3 projects in this repository:

1. **custom-user-group-callback** is a custom implementation of the `org.kie.api.task.UserGroupCallback` interface.
2. **http-header-based-auth** is the login module implementation.
3. **servlet-filter** contains the servlet filters.

There are also two properties files for reference:

1. **custom-sso-headers.properties** contains the SSO headers to add to the HTTP request.  This is used by the mock SSO servlet filter.
2. **custom-user-call-back.properties** contains the users and roles for the custom user group callback implementation.

## Instructions

### 1. Build each project.
The easiest way is `mvn clean install`

### 2. Copy all 3 built jars into the kie-server lib folder
```
> cp custom-user-group-callback/target/custom-user-group-callback.jar $JBOSS_HOME/standalone/deployments/kie-server.war/WEB-INF/lib/.
> cp http-header-based-auth/target/http-header-based-auth.jar $JBOSS_HOME/standalone/deployments/kie-server.war/WEB-INF/lib/.
> cp servlet-filter/target/servlet-filter.jar $JBOSS_HOME/standalone/deployments/kie-server.war/WEB-INF/lib/.
```

### 3. Modify $JBOSS_HOME/standalone/deployments/kie-server.war/WEB-INF/web.xml


