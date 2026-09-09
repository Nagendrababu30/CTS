package com.cts.admin.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import com.cts.admin.model.User;
import com.cts.admin.service.UserService;
import com.cts.admin.service.UserServiceImpl;

public class LoginComposer
        extends GenericForwardComposer<Component> {


    private Textbox username;
    private Textbox password;
    private Label loginMessage;

    private UserService userService;

    @Override
    public void doAfterCompose(
            Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        userService = new UserServiceImpl();
    }

    public void onClick$loginButton() {

        String usernameValue =
                username.getValue();

        String passwordValue =
                password.getValue();

        User user =
                userService.authenticate(
                        usernameValue,
                        passwordValue);

        if (user == null) {

            loginMessage.setValue(
                    "Invalid username or password.");

            return;
        }

        Session session =
                Executions.getCurrent()
                        .getSession();

        session.setAttribute(
                "loggedInUser",
                user);

        session.setAttribute(
                "userId",
                user.getUserId());

        session.setAttribute(
                "username",
                user.getUsername());

        session.setAttribute(
                "roleId",
                user.getRoleId());

        session.setAttribute(
                "roleName",
                user.getRoleName());

        redirectUser(user);
    }

    private void redirectUser(User user) {

        String roleName =
                user.getRoleName();

        if (roleName == null
                || roleName.isBlank()) {

            loginMessage.setValue(
                    "User role is not configured.");

            return;
        }

        switch (roleName.toUpperCase()) {

            case "ADMIN":

                Executions.sendRedirect(
                        "/zul/admin/admin-dashboard.zul");

                break;

            case "INWARD_MAKER":

                Executions.sendRedirect(
                        "/zul/inward-maker/dashboard.zul");

                break;

            case "INWARD_CHECKER":

                Executions.sendRedirect(
                        "/zul/inward-checker/dashboard.zul");

                break;

            case "OUTWARD_MAKER":

                Executions.sendRedirect(
                        "/zul/outward/outward-maker/makerDashboard.zul");

                break;

            case "OUTWARD_CHECKER":

                Executions.sendRedirect(
                        "/zul/outward/outward-checker/checkerDashboard.zul");

                break;

            case "CAPTURE_OPERATOR":

                Executions.sendRedirect(
                        "/zul/outward/outward-maker/makerDashboard.zul");

                break;

            default:

                loginMessage.setValue(
                        "User role is not configured.");
        }
    }
}