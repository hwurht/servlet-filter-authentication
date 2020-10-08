package com.redhat.custom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.kie.api.task.UserGroupCallback;

public class PropertyBasedUserGroupCallback implements UserGroupCallback {
    private static final String PROPERTY_FILE_KEY = "custom.user.group.callback.file";

    private Map<String, List<String>> userGroupMap = new HashMap<>();

    public PropertyBasedUserGroupCallback() {
    }

    @Override
    public boolean existsUser(String userId) {
        System.out.println("====> existsUser() -> userId = " + userId);
        loadUsers();
        return userGroupMap.containsKey(userId);
    }

    @Override
    public boolean existsGroup(String groupId) {
        System.out.println("====> existsGroup() -> groupId = " + groupId);
        if ("Administrators".equals(groupId)) {
            return true;
        }
        loadUsers();
        for (List<String> groups : userGroupMap.values()) {
            for (String group : groups) {
                if (group.equals(groupId)) {
                    System.out.println("---> found group " + group);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> getGroupsForUser(String userId) {
        System.out.println("====> getGroupsForUser() -> userId = " + userId);

        loadUsers();

        System.out.println("---> user = " + userId);

        List<String> list = new ArrayList<>();
        if (userGroupMap.containsKey(userId)) {
            list = userGroupMap.get(userId);
        }

        System.out.println("====> getGroupsForUser() -> groups = " + list);

        return list;
    }

    private void loadUsers() {
        String file = System.getProperty(PROPERTY_FILE_KEY);
        System.out.println("====> loading users and groups from " + file);
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(new File(file))) {
            props.load(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        userGroupMap.clear();
        
        for (Object key : props.keySet()) {
            String user = key.toString();
            System.out.println("*******> found user " + user);
            Object value = props.get(key);
            if (value != null) {
                String groups = value.toString();
                System.out.println("*********> found groups " + groups);
                String[] parts = groups.split(",");
                userGroupMap.put(user, Arrays.asList(parts));
            }
        }
        System.out.println("********> final map = " + userGroupMap);
    }
}
