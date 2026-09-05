package com.cts.admin.service;

import com.cts.admin.dao.UserDao;
import com.cts.admin.model.User;

public class UserService {

    private final UserDao userDao;

    public UserService() {
    	userDao = new UserDao();
    }

    public User authenticate(String username, String password) {

        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        if (password == null || password.isEmpty()) {
            return null;
        }

        return userDao.authenticate(
                username.trim(),
                password);
    }
}