package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.cts.admin.model.Role;
import com.cts.inward.config.ConnectionPool;

public class RoleDAOImpl implements RoleDAO {

    @Override
    public List<Role> getAllRoles() {

        List<Role> roles = new ArrayList<>();

        /* CTS DB uses reserved word "role" — must be quoted */
        String sql =
                "SELECT role_id, role_name, description, status, created_at "
                + "FROM \"role\" "
                + "ORDER BY role_id";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                roles.add(mapRole(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return roles;
    }

    @Override
    public Role getRoleById(Long roleId) {

        if (roleId == null) return null;

        String sql =
                "SELECT role_id, role_name, description, status, created_at "
                + "FROM \"role\" "
                + "WHERE role_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, roleId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRole(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean createRole(Role role) {

        if (role == null) return false;

        String sql =
                "INSERT INTO \"role\" (role_name, description, status) "
                + "VALUES (?, ?, ?)";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, role.getRoleName());
            stmt.setString(2, role.getDescription());
            stmt.setString(3, role.getStatus());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateRole(Role role) {

        if (role == null || role.getRoleId() == null) return false;

        String sql =
                "UPDATE \"role\" "
                + "SET role_name = ?, description = ? "
                + "WHERE role_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, role.getRoleName());
            stmt.setString(2, role.getDescription());
            stmt.setLong(3, role.getRoleId());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateRoleStatus(Long roleId, String status) {

        if (roleId == null || status == null) return false;

        String sql =
                "UPDATE \"role\" SET status = ? WHERE role_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, status);
            stmt.setLong(2, roleId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* ------------------------------------------------------------------ */

    private Role mapRole(ResultSet rs) throws Exception {
        Role role = new Role();
        role.setRoleId(rs.getLong("role_id"));
        role.setRoleName(rs.getString("role_name"));
        role.setDescription(rs.getString("description"));
        role.setStatus(rs.getString("status"));
        role.setCreatedAt(rs.getTimestamp("created_at"));
        return role;
    }
}
