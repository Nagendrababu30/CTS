package com.cts.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;

import com.cts.admin.model.User;

public class AuthorizationComposer extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	private static final Map<String, String> PAGE_PERMISSIONS = new HashMap<>();

	static {

		// =========================================================
		// ADMIN
		// =========================================================

		PAGE_PERMISSIONS.put("/zul/admin/adminDashboard.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/roleManagement.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/userManagement.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/batchMonitoring.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/sessionManagement.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/auditLogs.zul", "ADMIN");

		// =========================================================
		// INWARD MAKER
		// =========================================================

		PAGE_PERMISSIONS.put("/zul/inward-maker/dashboard.zul", "INWARD_MAKER");

		/*
		 * MICR Repair queue page.
		 *
		 * This was missing previously and caused:
		 *
		 * /zul/inward-maker/micr-repair-list.zul ↓ Access Denied
		 */
		PAGE_PERMISSIONS.put("/zul/inward-maker/micr-repair-list.zul", "INWARD_MAKER");

		/*
		 * MICR Repair detail page.
		 */

		PAGE_PERMISSIONS.put("/zul/inward-maker/micr-repair-list.zul", "INWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/inward-maker/micr-repair.zul", "INWARD_MAKER");

		/*
		 * Data Entry page.
		 */
		PAGE_PERMISSIONS.put("/zul/inward-maker/data-entry.zul", "INWARD_MAKER");

		/*
		 * These pages can remain protected if they still exist, but they are no longer
		 * displayed in the Maker sidebar.
		 */
		PAGE_PERMISSIONS.put("/zul/inward-maker/send-to-checker.zul", "INWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/inward-maker/reports.zul", "INWARD_MAKER");

		// =========================================================
		// INWARD CHECKER
		// =========================================================

		PAGE_PERMISSIONS.put("/zul/inward-checker/dashboard.zul", "INWARD_CHECKER");

		PAGE_PERMISSIONS.put("/zul/inward-checker/verification.zul", "INWARD_CHECKER");

		PAGE_PERMISSIONS.put("/zul/inward-checker/batch-details.zul", "INWARD_CHECKER");
		
		PAGE_PERMISSIONS.put("/zul/inward-checker/reports.zul", "INWARD_CHECKER");
		
		

		// =========================================================
		// OUTWARD MAKER
		// =========================================================

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/makerDashboard.zul", "OUTWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/dataEntry.zul", "OUTWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/MicrRepair.zul", "OUTWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/amountAndAccount.zul", "OUTWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/sendToChecker.zul", "OUTWARD_MAKER");

		// =========================================================
		// OUTWARD CHECKER
		// =========================================================

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/checkerDashboard.zul", "OUTWARD_CHECKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/batchQueue.zul", "OUTWARD_CHECKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/checkerReports.zul", "OUTWARD_CHECKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-checker/sendToNpci.zul", "OUTWARD_CHECKER");
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