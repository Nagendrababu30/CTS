package com.cts.admin.service;

import java.util.List;

import com.cts.admin.dao.RoleDAO;
import com.cts.admin.dao.RoleDAOImpl;
import com.cts.admin.model.Role;

public class RoleServiceImpl implements RoleService {

    private final RoleDAO roleDAO;

    public RoleServiceImpl() {
        roleDAO = new RoleDAOImpl();
    }

    @Override
    public List<Role> getAllRoles() {
        return roleDAO.getAllRoles();
    }

    @Override
    public Role getRoleById(Long roleId) {
        if (roleId == null) return null;
        return roleDAO.getRoleById(roleId);
    }

    @Override
    public boolean createRole(Role role) {
        if (role == null) return false;
        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) return false;
        role.setRoleName(role.getRoleName().trim());
        if (role.getStatus() == null || role.getStatus().trim().isEmpty()) role.setStatus("ACTIVE");
        return roleDAO.createRole(role);
    }

    @Override
    public boolean updateRole(Role role) {
        if (role == null || role.getRoleId() == null) return false;
        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) return false;
        role.setRoleName(role.getRoleName().trim());
        return roleDAO.updateRole(role);
    }

    @Override
    public boolean updateRoleStatus(Long roleId, String status) {
        if (roleId == null || status == null || status.trim().isEmpty()) return false;
        return roleDAO.updateRoleStatus(roleId, status.trim());
    }
}
