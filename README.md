# Using Servlet Login for Red Hat Process Automation Manager's Process Server
## Overview
Out of the box the process server (kie-server) in RHPAM in conjuction with JBoss EAP will use container security directly.  The problem is this authentication happens before any servlet filters are invoked.  With SSO plugins that use the servlet filter mechanism for user verification, we need to use an alternative approach.  Here are the major changes of this approach.

1. Security constraints roles are turn off in the web.xml to disable triggering of RBAC.
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

1. Add filter entries and their mapping counterparts.  Make sure the login filter is defined after the mock filter.

```
<filter>
    <filter-name>mock</filter-name>
    <filter-class>com.redhat.example.servlet.filter.mock.MockSSORestFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>mock</filter-name>
    <url-pattern>/services/rest/*</url-pattern>
</filter-mapping>
<filter>
    <filter-name>login</filter-name>
    <filter-class>com.redhat.example.servlet.filter.HTTPHeaderLoginFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>login</filter-name>
    <url-pattern>/services/rest/*</url-pattern>
</filter-mapping>
```
2. Comment out or delete the auth-constraint for the servlet resources

```
  <security-constraint>
    <web-resource-collection>
      <web-resource-name>REST web resources</web-resource-name>
      <url-pattern>/services/rest/*</url-pattern>
      <http-method>GET</http-method>
      <http-method>PUT</http-method>
      <http-method>POST</http-method>
      <http-method>DELETE</http-method>
    </web-resource-collection>
    <!--
    <auth-constraint>
      <role-name>kie-server</role-name>
      <role-name>user</role-name>
    </auth-constraint>
    -->
  </security-constraint>
```
3. Comment out or delete the login-config section

```
  <!--
  <login-config>
    <auth-method>BASIC</auth-method>
    <realm-name>KIE Server</realm-name>
  </login-config>
  -->
```
### 4. Modify the $JBOSS_HOME/standalone/deployments/kie-server.war/WEB-INF/jboss-web.xml file

Change the security-domain to **kie-server**

```
<jboss-web>
  <security-domain>kie-server</security-domain>
</jboss-web>
```

### 5. Modify the $JBOSS_HOME/standlone/configuration/standalone-full.xml file

Add a new security-domain call **kie-server** under the security subsystem.

```
                <security-domain name="kie-server" cache-type="default">
                    <authentication>
                        <login-module code="com.redhat.example.http.auth.HTTPHeaderLoginModule" flag="required" module="deployment.kie-server.war">
                            <module-option name="password-stacking" value="useFirstPass"/>
                            <module-option name="EXTRA_ROLES_USER_GROUP_CALLBACK_CLASS" value="com.redhat.custom.PropertyBasedUserGroupCallback"/>
                        </login-module>
                    </authentication>
                </security-domain>
```

The `module="deployment.kie-server.war"` attribute tells JBoss to look in the kie-server.war deployment for the Java class.  The `EXTRA_ROLES_USER_GROUP_CALLBACK_CLASS` module option is used by the HTTPHeaderLoginModule class to load additional roles for user tasks.

### 6. Define properties to use custom files

1. Define a `custom.user.group.callback.file` system property to point to the **custom-user-call-back.properties** file so the PropertyBasedUserGroupCallback class can find additional roles.

2. Define a `custom.sso.request.header.file` system property to point to the **custom-sso-headers.properties** file so the MockSSORestFilter class can find the HTTP headers to add to the request.
