/*
 * Copyright (C) 2008 All rights reserved
 * VPRO The Netherlands
 * Creation date 1 nov 2008.
 */
package nl.vpro.domain.users;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Arne
 * @author roekoe
 * @TODO WTF
 */
public class Editor extends User {

    private Set<UserGroup> groups = new HashSet<>();

    private Set<Role> roles = new HashSet<>();

    /**
     * @return the groups
     */
    public Set<UserGroup> getGroups() {
        return groups;
    }
    /**
     * @param groups the groups to set
     */
    public void setGroups(Set<UserGroup> groups) {
        this.groups = groups;
    }
    /**
     * @return the roles
     */
    public Set<Role> getRoles() {
        return roles;
    }
    /**
     * @param roles the roles to set
     */
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    /**
     * @param testRole the role to test
     * @return true if the user has the desired role (either direct or via usergroups.)
     */
    public boolean hasRole(Role testRole) {
        boolean result = hasRole(testRole, getRoles());
        if (!result) {
            for (UserGroup group : getGroups()) {
                result = hasRole(testRole, group.getRoles());
                if (result) {
                    break;
                }
            }
        }
        return result;
    }

    private boolean hasRole(Role testRole, Collection<Role> testRoles) {
        boolean result = false;
        for (Role role : testRoles) {
            if (role == testRole) {
                result = true;
                break;
            }
        }
        return result;
    }
}
