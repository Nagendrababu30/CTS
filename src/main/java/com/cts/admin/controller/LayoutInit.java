package com.cts.admin.controller;

import java.util.Map;


import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.Initiator;

import com.cts.admin.model.User;

public class LayoutInit implements Initiator {

	@Override
	public void doInit(Page page, Map<String, Object> args) throws Exception {

		Execution execution = Executions.getCurrent();

		Session session = execution.getSession();

		User user = (User) session.getAttribute("loggedInUser");

		if (user == null) {

			Executions.sendRedirect("/login.zul");

			return;
		}

		execution.setAttribute("userId", user.getUserId());

		execution.setAttribute("roleId", user.getRoleId());

		execution.setAttribute("roleName", user.getRoleName());

		execution.setAttribute("username", user.getUsername());

		for (Map.Entry<String, Object> entry : args.entrySet()) {

			execution.setAttribute(entry.getKey(), entry.getValue());
		}
	}
}