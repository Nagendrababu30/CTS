package com.iispl.cts.controller.outward;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.cts.model.outward.UserSession;
import com.iispl.cts.service.outward.LoginService;

public class LoginController
        extends SelectorComposer<org.zkoss.zk.ui.Component> {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_USER = "CTS_USER_SESSION";

    @Wire
    private Textbox username;

    @Wire
    private Textbox password;

    @Wire
    private Button loginButton;

    private LoginService loginService;

    @Override
    public void doAfterCompose(
            org.zkoss.zk.ui.Component comp) throws Exception {

        super.doAfterCompose(comp);

        loginService = new LoginService();

        UserSession existingSession =
                getCurrentUserSession();

        /*
         * If the user is already logged in,
         * directly open the appropriate role screen.
         */
        if (existingSession != null) {

            redirectByRole(
                    existingSession.getRoleId());
        }
    }

    /**
     * Login button event
     */
    @Listen("onClick=#loginButton")
    public void login() {

        String userNameValue =
                username.getValue();

        String passwordValue =
                password.getValue();

        if (userNameValue == null ||
                userNameValue.trim().isEmpty()) {

            Messagebox.show(
                    "Please enter username.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        if (passwordValue == null ||
                passwordValue.isEmpty()) {

            Messagebox.show(
                    "Please enter password.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        try {

            UserSession userSession =
                    loginService.authenticate(
                            userNameValue.trim(),
                            passwordValue);

            if (userSession == null) {

                Messagebox.show(
                        "Invalid username or password, "
                        + "or user is inactive.",
                        "Login Failed",
                        Messagebox.OK,
                        Messagebox.ERROR);

                password.setValue("");

                return;
            }

            /*
             * Store authenticated user in ZK session.
             */
            Sessions.getCurrent().setAttribute(
                    SESSION_USER,
                    userSession);

            System.out.println(
                    "LOGIN SUCCESS: "
                    + "userId="
                    + userSession.getUserId()
                    + ", username="
                    + userSession.getUsername()
                    + ", roleId="
                    + userSession.getRoleId());

            /*
             * Directly open the role-specific
             * centralized screen.
             */
            redirectByRole(
                    userSession.getRoleId());

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to login.\n\nError: "
                    + e.getMessage(),
                    "Login Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    /**
     * Redirect user according to role ID.
     *
     * Role 3 = Outward Maker
     * Role 4 = Outward Checker
     * Role 5 = Capture Operator
     */
    private void redirectByRole(int roleId) {

        switch (roleId) {

            case 3:

                // Outward Maker
                Executions.sendRedirect(
                        "/outward-maker-dashboard.zul");

                break;

            case 4:

                // Outward Checker
                Executions.sendRedirect(
                        "/outward/checker/dashboard.zul");

                break;

            case 5:

                // Capture Operator
                Executions.sendRedirect(
                        "/capture-operator-batch-capture.zul");

                break;

            default:

                Sessions.getCurrent()
                        .removeAttribute(SESSION_USER);

                Messagebox.show(
                        "This user role is not enabled "
                        + "for the current application.",
                        "Access Denied",
                        Messagebox.OK,
                        Messagebox.ERROR);

                break;
        }
    }

    /**
     * Get logged-in user.
     */
    public static UserSession getCurrentUserSession() {

        Object sessionObject =
                Sessions.getCurrent()
                        .getAttribute(SESSION_USER);

        if (sessionObject instanceof UserSession) {

            return (UserSession) sessionObject;
        }

        return null;
    }

    /**
     * Get logged-in user ID.
     */
    public static int getCurrentUserId() {

        UserSession user =
                getCurrentUserSession();

        return user == null
                ? 0
                : user.getUserId();
    }

    /**
     * Get logged-in role ID.
     */
    public static int getCurrentRoleId() {

        UserSession user =
                getCurrentUserSession();

        return user == null
                ? 0
                : user.getRoleId();
    }

    /**
     * Check login status.
     */
    public static boolean isLoggedIn() {

        return getCurrentUserSession() != null;
    }

    /**
     * Logout.
     */
    public static void logout() {

        Sessions.getCurrent()
                .removeAttribute(SESSION_USER);

        Executions.sendRedirect(
                "/login.zul");
    }
}