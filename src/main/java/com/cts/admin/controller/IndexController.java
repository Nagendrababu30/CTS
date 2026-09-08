package com.cts.admin.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;

public class IndexController extends GenericForwardComposer<Component> {


    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        Session session =
                Executions.getCurrent().getSession();

        if (session != null) {
            session.invalidate();
        }

        Executions.sendRedirect("/login.zul");
    }
}