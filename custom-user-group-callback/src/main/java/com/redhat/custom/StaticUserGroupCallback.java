package com.redhat.custom;

import java.util.Arrays;
import java.util.List;

import org.kie.api.task.UserGroupCallback;

public class StaticUserGroupCallback implements UserGroupCallback {

    @Override
    public boolean existsUser(String userId) {
        System.out.println("====> existsUser() -> " + userId);
        return true;
    }

    @Override
    public boolean existsGroup(String groupId) {
        System.out.println("====> existsGroup() -> " + groupId);
        return true;
    }

    @Override
    public List<String> getGroupsForUser(String userId) {
        List<String> groups = Arrays.asList("Administrators", "Group1", "Group2");
        System.out.println("====> getGroupsForUser() -> " + userId);
        System.out.println("====> getGroupsForUser() -> " + groups);
        return groups;
    }

}
