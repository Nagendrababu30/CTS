package com.cts.admin.service;

import java.util.List;

import com.cts.admin.dao.UserDao;
import com.cts.admin.model.User;

public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl() {
        userDao = new UserDao();
    }

    // ================================================================
    // AUTH
    // ================================================================

    @Override
    public User authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty()) return null;
        if (password == null || password.trim().isEmpty()) return null;
        return userDao.authenticate(username, password);
    }

    // ================================================================
    // CRUD
    // ================================================================

    @Override
    public List<User> getUsers(int limit, int offset,
            String searchText, Long roleId, String status) {
        return userDao.getUsers(limit, offset, searchText, roleId, status);
    }

    @Override
    public int getUserCount() {
        return userDao.getUserCount();
    }

    @Override
    public User getUserById(Long userId) {
        return userDao.getUserById(userId);
    }

    @Override
    public boolean usernameExists(String username) {
        return userDao.usernameExists(username);
    }

    @Override
    public boolean createUser(User user) {

        if (user == null) return false;
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) return false;
        if (user.getRole() == null || user.getRole().getRoleId() == null) return false;
        if (usernameExists(user.getUsername().trim())) return false;

        user.setUsername(user.getUsername().trim());

        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("ACTIVE");
        }

        return userDao.createUser(user);
    }

    @Override
    public boolean updateUser(User user) {

        if (user == null || user.getUserId() == null) return false;
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) return false;
        if (user.getRole() == null || user.getRole().getRoleId() == null) return false;

        user.setUsername(user.getUsername().trim());

        return userDao.updateUser(user);
    }

    @Override
    public boolean updateUserStatus(Long userId, String status) {
        if (userId == null || status == null || status.trim().isEmpty()) return false;
        return userDao.updateUserStatus(userId, status);
    }

    @Override
    public void deleteUser(Long userId) {

        if (userId == null) throw new IllegalArgumentException("User ID cannot be null.");

        User user = userDao.getUserById(userId);

        if (user == null) throw new IllegalArgumentException("User not found.");

        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException(
                    "Active user cannot be deleted. Deactivate the user first.");
        }

        userDao.deleteUser(userId);
    }
}
