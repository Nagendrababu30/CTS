package com.cts.admin.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import com.cts.admin.model.Role;
import com.cts.admin.service.RoleService;
import com.cts.admin.service.RoleServiceImpl;

public class RoleController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	private Listbox roleListbox;
	private Button createRoleBtn;
	private RoleService roleService;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		roleService = new RoleServiceImpl();

		createRoleBtn.addEventListener("onClick", new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				openCreateRoleModal();
			}
		});

		loadRoles();
	}

	private void loadRoles() {

		try {

			List<Role> roles = roleService.getAllRoles();

			roleListbox.getItems().clear();

			if (roles == null || roles.isEmpty())
				return;

			for (Role role : roles) {

				Listitem item = new Listitem();

				/* ── Role Name ── */
				Listcell nameCell = new Listcell();
				Label nameLabel = new Label(role.getRoleName());
				nameLabel.setSclass("role-name-label");
				nameCell.appendChild(nameLabel);
				item.appendChild(nameCell);

				/* ── Description ── */
				Listcell descCell = new Listcell();
				Label descLabel = new Label(role.getDescription() == null ? "-" : role.getDescription());
				descLabel.setSclass("role-description-label");
				descCell.appendChild(descLabel);
				item.appendChild(descCell);

				/* ── Status badge ── */
				Listcell statusCell = new Listcell();
				Hbox badge = new Hbox();
				boolean isActive = "ACTIVE".equalsIgnoreCase(role.getStatus());
				badge.setSclass(
						isActive ? "role-status-badge role-status-active" : "role-status-badge role-status-inactive");
				String displayStatus = isActive ? "Active" : "Inactive";
				Label statusLabel = new Label(displayStatus);
				statusLabel.setSclass("role-status-label");
				badge.appendChild(statusLabel);
				statusCell.appendChild(badge);
				item.appendChild(statusCell);

				/* ── Actions ── */
				Listcell actionCell = new Listcell();
				actionCell.setSclass("role-action-cell");

				Button toggleBtn = new Button(isActive ? "Deactivate" : "Activate");
				toggleBtn.setSclass(isActive ? "role-action-button role-deactivate-button"
						: "role-action-button role-activate-button");

				final Long roleId = role.getRoleId();
				final boolean currentStatus = isActive;

				toggleBtn.addEventListener("onClick", new EventListener<Event>() {
					@Override
					public void onEvent(Event event) throws Exception {
						toggleRoleStatus(roleId, currentStatus);
					}
				});

				actionCell.appendChild(toggleBtn);
				item.appendChild(actionCell);

				item.setValue(role);
				roleListbox.appendChild(item);
			}

		} catch (Exception e) {
			e.printStackTrace();
			Messagebox.show("Unable to load system roles.", "Roles", Messagebox.OK, Messagebox.ERROR);
		}
	}

	private void openCreateRoleModal() {

		Window window = new Window();
		window.setTitle("");
		window.setWidth("480px");
		window.setBorder("none");
		window.setClosable(false);
		window.setSizable(false);
		window.setSclass("role-modal-window");

		Vbox mainBox = new Vbox();
		mainBox.setSpacing("0");
		mainBox.setHflex("1");
		mainBox.setSclass("role-modal-main-box");

		/* ── Header ── */
		Div headerDiv = new Div();
		headerDiv.setHflex("1");
		headerDiv.setSclass("role-modal-header");

		Div headerLeftDiv = new Div();
		headerLeftDiv.setSclass("role-modal-header-left");

		Div iconDiv = new Div();
		iconDiv.setSclass("role-modal-icon-box");
		Label iconLabel = new Label("➕");
		iconLabel.setSclass("role-modal-icon-label");
		iconDiv.appendChild(iconLabel);

		Div titleTextDiv = new Div();
		titleTextDiv.setSclass("role-modal-title-text");
		Label titleLabel = new Label("Create New Role");
		titleLabel.setSclass("role-modal-title");
		Label subtitleLabel = new Label("Fill in the role name to create a new role");
		subtitleLabel.setSclass("role-modal-subtitle");
		titleTextDiv.appendChild(titleLabel);
		titleTextDiv.appendChild(subtitleLabel);
		headerLeftDiv.appendChild(iconDiv);
		headerLeftDiv.appendChild(titleTextDiv);

		/* Close button */
		Button closeBtn = new Button("✕");
		closeBtn.setSclass("role-modal-close-button");
		closeBtn.addEventListener("onClick", new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});

		headerDiv.appendChild(headerLeftDiv);
		headerDiv.appendChild(closeBtn);
		mainBox.appendChild(headerDiv);

		/* ── Form ── */
		Vbox formBox = new Vbox();
		formBox.setSpacing("16px");
		formBox.setHflex("1");
		formBox.setSclass("role-form-box");

		/* Role Name Field */
		Label roleNameLabelField = new Label("Role Name");
		roleNameLabelField.setSclass("role-form-label");

		Textbox roleNameInput = new Textbox();
		roleNameInput.setHflex("1");
		roleNameInput.setSclass("role-form-input");
		roleNameInput.setPlaceholder("e.g., ROLE_SUPERVISOR");

		Vbox roleNameFieldBox = new Vbox();
		roleNameFieldBox.setSpacing("6px");
		roleNameFieldBox.setHflex("1");
		roleNameFieldBox.appendChild(roleNameLabelField);
		roleNameFieldBox.appendChild(roleNameInput);
		formBox.appendChild(roleNameFieldBox);

		/* Description Field */
		Label descriptionLabelField = new Label("Description");
		descriptionLabelField.setSclass("role-form-label");

		Textbox descriptionInput = new Textbox();
		descriptionInput.setHflex("1");
		descriptionInput.setHeight("80px");
		descriptionInput.setSclass("role-form-input role-form-textarea");
		descriptionInput.setPlaceholder("Enter a description for this role (optional)");
		descriptionInput.setMultiline(true);

		Vbox descriptionFieldBox = new Vbox();
		descriptionFieldBox.setSpacing("6px");
		descriptionFieldBox.setHflex("1");
		descriptionFieldBox.appendChild(descriptionLabelField);
		descriptionFieldBox.appendChild(descriptionInput);
		formBox.appendChild(descriptionFieldBox);

		mainBox.appendChild(formBox);

		/* ── Footer ── */
		Div footerDiv = new Div();
		footerDiv.setSclass("role-modal-footer");

		Button cancelBtn = new Button("Cancel");
		cancelBtn.setSclass("role-modal-cancel-button");
		cancelBtn.addEventListener("onClick", new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});

		Button saveBtn = new Button("Create Role");
		saveBtn.setSclass("role-modal-save-button");
		saveBtn.addEventListener("onClick", new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				saveRole(roleNameInput.getValue(), descriptionInput.getValue(), window);
			}
		});

		footerDiv.appendChild(cancelBtn);
		footerDiv.appendChild(saveBtn);
		mainBox.appendChild(footerDiv);

		window.appendChild(mainBox);
		window.setPage(roleListbox.getPage()); // Use setPage instead of setParent
		window.doModal();
	}

	private void saveRole(String roleName, String description, Window window) {

		if (roleName == null || roleName.trim().isEmpty()) {
			Messagebox.show("Please enter a role name.", "Validation", Messagebox.OK, Messagebox.INFORMATION);
			return;
		}

		try {
			Role newRole = new Role();
			newRole.setRoleName(roleName.trim());
			newRole.setDescription(description != null ? description.trim() : ""); // Use user input or empty
			newRole.setStatus("ACTIVE"); // Auto-populated ACTIVE status

			if (roleService.createRole(newRole)) {
				Messagebox.show("Role created successfully.", "Success", Messagebox.OK, Messagebox.INFORMATION);
				window.detach();
				loadRoles(); // Reload the role list
			} else {
				Messagebox.show("Failed to create role.", "Error", Messagebox.OK, Messagebox.ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Messagebox.show("Error creating role: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
		}
	}

	private void toggleRoleStatus(Long roleId, boolean currentlyActive) {

		try {
			String newStatus = currentlyActive ? "INACTIVE" : "ACTIVE";

			if (roleService.updateRoleStatus(roleId, newStatus)) {
				Messagebox.show("Role status updated to " + newStatus + ".", "Success", Messagebox.OK,
						Messagebox.INFORMATION);
				loadRoles(); // Reload the role list
			} else {
				Messagebox.show("Failed to update role status.", "Error", Messagebox.OK, Messagebox.ERROR);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Messagebox.show("Error updating role status: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
		}
	}

}
