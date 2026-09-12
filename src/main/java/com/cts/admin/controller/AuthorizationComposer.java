package com.cts.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;

import com.cts.admin.model.User;

public class AuthorizationComposer
        extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	private static final Map<String, String> PAGE_PERMISSIONS = new HashMap<>();

	static {

		// =========================================================
		// ADMIN
		// =========================================================

		PAGE_PERMISSIONS.put("/zul/admin/admin-dashboard.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/admin-roles.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/admin-user-management.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/admin-batch-monitoring.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/admin-session-management.zul", "ADMIN");

		PAGE_PERMISSIONS.put("/zul/admin/admin-audit-logs.zul", "ADMIN");

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
		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-dashboard.zul", "OUTWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-micr-repair.zul", "OUTWARD_MAKER");
		PAGE_PERMISSIONS.put(
			    "/zul/outward/outward-maker/outward-maker-data-entry-detail.zul",
			    "OUTWARD_MAKER"
			);
		//CAPTURE OPERATOR 
		
		PAGE_PERMISSIONS.put(
			    "/zul/outward/outward-maker/capture-operator-batch-capture.zul",
			    "CAPTURE_OPERATOR"
			);

			PAGE_PERMISSIONS.put(
			    "/zul/outward/outward-maker/capture-operator-captured-batches.zul",
			    "CAPTURE_OPERATOR"
			);

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-micr-repair-detail.zul", "OUTWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-data-entry.zul", "OUTWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-data-entry-repair.zul", "OUTWARD_MAKER");

		PAGE_PERMISSIONS.put("/zul/outward/outward-maker/outward-maker-send-to-checker.zul", "OUTWARD_MAKER");
		// =========================================================
		// OUTWARD CHECKER
		// =========================================================

		// ============================================================
		// OUTWARD CHECKER PAGES
		// ============================================================

		PAGE_PERMISSIONS.put(
		        "/zul/outward/outward-checker/dashboard.zul",
		        "OUTWARD_CHECKER"
		);

		PAGE_PERMISSIONS.put(
		        "/zul/outward/outward-checker/batchesQueue.zul",
		        "OUTWARD_CHECKER"
		);

		PAGE_PERMISSIONS.put(
		        "/zul/outward/outward-checker/chequeVerification.zul",
		        "OUTWARD_CHECKER"
		);

		PAGE_PERMISSIONS.put(
		        "/zul/outward/outward-checker/reports.zul",
		        "OUTWARD_CHECKER"
		);

		PAGE_PERMISSIONS.put(
		        "/zul/outward/outward-checker/sendToNPCI.zul",
		        "OUTWARD_CHECKER"
		);
		PAGE_PERMISSIONS.put(
		        "/zul/outward/outward-checker/processing.zul",
		        "OUTWARD_CHECKER"
		);
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