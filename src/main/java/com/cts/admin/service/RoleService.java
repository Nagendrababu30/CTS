package com.cts.admin.service;

import java.util.List;

import com.cts.admin.model.Role;

public interface RoleService {

    List<Role> getAllRoles();

    Role getRoleById(Long roleId);

    boolean createRole(Role role);

    boolean updateRole(Role role);

    boolean updateRoleStatus(Long roleId, String status);
}
