package com.cts.admin.service;

import java.util.List;

import com.cts.admin.model.User;

public interface UserService {

    User authenticate(String username, String password);

    List<User> getUsers(int limit, int offset, String searchText, Long roleId, String status);

    User getUserById(Long userId);

    boolean usernameExists(String username);

    boolean createUser(User user);

    boolean updateUser(User user);

    boolean updateUserStatus(Long userId, String status);

    void deleteUser(Long userId);

    int getUserCount();
}
