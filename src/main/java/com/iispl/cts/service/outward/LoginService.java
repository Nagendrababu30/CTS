package com.iispl.cts.service.outward;

import com.iispl.cts.dao.outward.LoginDAO;
import com.iispl.cts.model.outward.UserSession;

public class LoginService {

    private final LoginDAO loginDAO;

    public LoginService() {

        this.loginDAO = new LoginDAO();

    }

    public UserSession authenticate(
            String username,
            String password) throws Exception {

        if (username == null ||
                username.trim().isEmpty()) {

            return null;

        }

        if (password == null ||
                password.trim().isEmpty()) {

            return null;

        }

        UserSession userSession =
                loginDAO.authenticate(
                        username.trim(),
                        password);

        if (userSession == null) {

            return null;

        }

        if (!"ACTIVE".equalsIgnoreCase(
                userSession.getStatus())) {

            return null;

        }

        /*
         * Update last login in database
         */

        boolean updated =
                loginDAO.updateLastLogin(
                        userSession.getUserId());

        if (!updated) {

            System.out.println(
                    "WARNING: Unable to update last_login "
                    + "for user ID: "
                    + userSession.getUserId());

        }

        /*
         * Keep the login/session information
         * that was retrieved from the database.
         */

        return userSession;

    }

}