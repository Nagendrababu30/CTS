package com.cts.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zk.ui.util.Composer;

import com.cts.admin.model.User;

public class AuthorizationComposer extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	private static final Map<String, String> PAGE_PERMISSIONS = new HashMap<>();

	static {

		// ADMIN

		PAGE_PERMISSIONS.put("/zul/admin/admin-dashboard.zul", "Admin");

		PAGE_PERMISSIONS.put("/zul/admin/admin-roles.zul", "Admin");

		PAGE_PERMISSIONS.put("/zul/admin/admin-user-management.zul", "Admin");

		PAGE_PERMISSIONS.put("/zul/admin/admin-batch-monitoring.zul", "Admin");

		PAGE_PERMISSIONS.put("/zul/admin/admin-session-management.zul", "Admin");

		PAGE_PERMISSIONS.put("/zul/admin/admin-audit-logs.zul", "Admin");

		// INWARD MAKER

		PAGE_PERMISSIONS.put("/zul/inward-maker/dashboard.zul", "Inward Maker");

		// MICR Repair queue page.		 *
		
		PAGE_PERMISSIONS.put("/zul/inward-maker/micr-repair-l+ist.zul", "Inward Maker");

		 // MICR Repair detail page.

		PAGE_PERMISSIONS.put("/zul/inward-maker/micr-repair-list.zul", "Inward Maker");

		PAGE_PERMISSIONS.put("/zul/inward-maker/micr-repair.zul", "Inward Maker");

		 // Data Entry page.
		
		PAGE_PERMISSIONS.put("/zul/inward-maker/data-entry.zul", "Inward Maker");
		
		PAGE_PERMISSIONS.put("/zul/inward-maker/data-entryform.zul", "Inward Maker");
		 
		PAGE_PERMISSIONS.put("/zul/inward-maker/send-to-checker.zul", "Inward Maker");

		PAGE_PERMISSIONS.put("/zul/inward-maker/reports.zul", "Inward Maker");

		// INWARD CHECKER

		PAGE_PERMISSIONS.put("/zul/inward-checker/dashboard.zul", "Inward Checker");

		PAGE_PERMISSIONS.put("/zul/inward-checker/verification.zul", "Inward Checker");

		PAGE_PERMISSIONS.put("/zul/inward-checker/batch-details.zul", "Inward Checker");

		PAGE_PERMISSIONS.put("/zul/inward-checker/reports.zul", "Inward Checker");

		// OUTWARD MAKER

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-dashboard.zul", "Outward Maker");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-micr-repair.zul", "Outward Maker");
		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-data-entry-detail.zul", "Outward Maker");
		PAGE_PERMISSIONS.put(
			    "/zul/outward/outward-maker/outward-clearing-session-wait.zul",
			    "Outward Maker"
			);

			PAGE_PERMISSIONS.put(
			    "/zul/outward/outward-maker/outward-clearing-session-wait.zul",
			    "Outward Checker"
			);

			PAGE_PERMISSIONS.put(
			    "/zul/outward/outward-maker/outward-clearing-session-wait.zul",
			    "Capture Operator"
			);
			
		// CAPTURE OPERATOR

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/capture-operator-batch-capture.zul", "Capture Operator");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/capture-operator-captured-batches.zul", "Capture Operator");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/capture-operator-reports.zul", "Capture Operator");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-micr-repair-detail.zul", "Outward Maker");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-data-entry.zul", "Outward Maker");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-data-entry-repair.zul", "Outward Maker");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-send-to-checker.zul", "Outward Maker");


		// OUTWARD CHECKER PAGES

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/dashboard.zul", "Outward Checker");

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/batchesQueue.zul", "Outward Checker");

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/chequeVerification.zul", "Outward Checker");

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/reports.zul", "Outward Checker");

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/sendToNPCI.zul", "Outward Checker");
		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/processing.zul", "Outward Checker");
	}

	@Override
	public void doBeforeComposeChildren(Component comp) throws Exception {

		super.doBeforeComposeChildren(comp);

		populateSessionRoleIfAbsent();
	}

	private void populateSessionRoleIfAbsent() {

		Execution execution = Executions.getCurrent();

		if (execution == null || execution.getAttribute("roleName") != null) {

			return;
		}

		Session session = execution.getSession();

		if (session == null) {
			return;
		}

		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null && loggedInUser.getRoleName() != null) {

			execution.setAttribute("roleName", loggedInUser.getRoleName());
		}
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		checkAuthorization();
	}

	private void checkAuthorization() {

		Execution execution = Executions.getCurrent();

		if (execution == null) {
			Executions.sendRedirect("/login.zul");
			return;
		}

		Session session = execution.getSession();

		if (session == null) {
			Executions.sendRedirect("/login.zul");
			return;
		}

		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser == null) {

			Executions.sendRedirect("/login.zul");

			return;
		}

		String currentPage = execution.getDesktop().getRequestPath();

		String requiredRole = PAGE_PERMISSIONS.get(currentPage);

		if (requiredRole == null) {

			System.out.println("Authorization warning: " + "Page is not registered: " + currentPage);

			Executions.sendRedirect("/accessDenied.zul");

			return;
		}

		String actualRole = loggedInUser.getRoleName();

		if (actualRole == null || !requiredRole.equalsIgnoreCase(actualRole)) {

			System.out.println("Unauthorized access attempt.");

			System.out.println("User: " + loggedInUser.getUsername());

			System.out.println("Required role: " + requiredRole);

			System.out.println("Actual role: " + actualRole);

			System.out.println("Requested page: " + currentPage);

			Executions.sendRedirect("/accessDenied.zul");

			return;
		}

		System.out.println("Authorization successful: " + loggedInUser.getUsername() + " -> " + currentPage);
	}
}