package com.cts.admin.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Popup;

public class LogoutComposer extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private Div userWrapper;
    private Popup logoutPopup;

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        System.out.println("LogoutComposer initialized.");
    }

    public void onClick$userWrapper() {

        System.out.println("User icon clicked.");

        if (logoutPopup != null && !logoutPopup.isVisible()) {

            logoutPopup.open(
                    userWrapper,
                    "after_end"
            );

        } else if (logoutPopup != null) {

            logoutPopup.close();
        }
    }

    public void onClick$logoutButton() {

        System.out.println("Logout button clicked.");

        Session session =
                Executions.getCurrent().getSession();

        if (session != null) {

            System.out.println(
                    "Invalidating session."
            );

            session.invalidate();
        }

        Executions.sendRedirect(
                "/login.zul"
        );
    }
}