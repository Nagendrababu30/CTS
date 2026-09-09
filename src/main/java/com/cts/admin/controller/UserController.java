package com.cts.admin.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;
import org.zkoss.zul.event.PagingEvent;

import com.cts.admin.model.Role;
import com.cts.admin.model.User;
import com.cts.admin.service.RoleService;
import com.cts.admin.service.RoleServiceImpl;
import com.cts.admin.service.UserService;
import com.cts.admin.service.UserServiceImpl;

public class UserController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final int PAGE_SIZE = 10;

    // ================================================================
    // ZUL COMPONENTS
    // ================================================================

    private Listbox userListbox;
    private Paging  userPaging;

    private Button  createUserButton;
    private Button  searchUserButton;
    private Button  clearUserFilterButton;

    private Textbox  userSearchTextbox;
    private Combobox roleFilterCombobox;
    private Combobox statusFilterCombobox;

    // ================================================================
    // SERVICES
    // ================================================================

    private UserService userService;
    private RoleService roleService;

    // ================================================================
    // INIT
    // ================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        userService = new UserServiceImpl();
        roleService = new RoleServiceImpl();

        loadRoleFilter();
        loadStatusFilter();

        userPaging.setPageSize(PAGE_SIZE);
        userPaging.setTotalSize(userService.getUserCount());

        loadUsers(0);

        userPaging.addEventListener("onPaging", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent pe = (PagingEvent) event;
                loadUsers(pe.getActivePage() * PAGE_SIZE);
            }
        });
    }

    // ================================================================
    // LOAD USERS
    // ================================================================

    private void loadUsers(int offset) {

        int serial = offset + 1;

        try {

            String searchText = userSearchTextbox.getValue();
            if (searchText == null) searchText = "";
            searchText = searchText.trim();

            Long roleId = null;
            if (roleFilterCombobox.getSelectedItem() != null) {
                Object v = roleFilterCombobox.getSelectedItem().getValue();
                if (v != null) roleId = (Long) v;
            }

            String status = null;
            if (statusFilterCombobox.getSelectedItem() != null) {
                Object v = statusFilterCombobox.getSelectedItem().getValue();
                if (v != null) status = v.toString();
            }

            List<User> users =
                    userService.getUsers(PAGE_SIZE, offset, searchText, roleId, status);

            userListbox.getItems().clear();

            for (User user : users) {

                Listitem item = new Listitem();

                /* S.No */
                Listcell snCell = new Listcell();
                Label snLabel = new Label(String.valueOf(serial));
                snLabel.setSclass("user-id-label");
                snCell.appendChild(snLabel);
                item.appendChild(snCell);

                /* Username */
                Listcell usernameCell = new Listcell();
                Label usernameLabel = new Label(user.getUsername());
                usernameLabel.setSclass("username-label");
                usernameCell.appendChild(usernameLabel);
                item.appendChild(usernameCell);

                /* Assigned Role */
                Listcell roleCell = new Listcell();
                String roleName = (user.getRole() != null
                        && user.getRole().getRoleName() != null)
                        ? user.getRole().getRoleName() : "-";
                Hbox roleBox = new Hbox();
                roleBox.setSclass("assigned-role-container");
                Label roleLabel = new Label(roleName);
                roleLabel.setSclass("assigned-role-label");
                roleBox.appendChild(roleLabel);
                roleCell.appendChild(roleBox);
                item.appendChild(roleCell);

                /* Status */
                Listcell statusCell = new Listcell();
                String userStatus = user.getStatus() != null ? user.getStatus() : "-";
                Hbox statusContainer = new Hbox();
                statusContainer.setSclass("status-container");
                Hbox statusBadge = new Hbox();
                statusBadge.setAlign("center");
                statusBadge.setSclass("status-badge");
                Label statusIcon = new Label("●");
                statusIcon.setSclass("status-icon");
                Label statusLbl = new Label(userStatus);
                statusLbl.setSclass("status-label");
                statusBadge.appendChild(statusIcon);
                statusBadge.appendChild(statusLbl);
                statusContainer.appendChild(statusBadge);
                statusCell.appendChild(statusContainer);
                item.appendChild(statusCell);

                /* Actions */
                Listcell actionCell = new Listcell();
                Hbox actionBox = new Hbox();
                actionBox.setSpacing("8px");
                actionBox.setAlign("center");
                actionBox.setSclass("actions-container");

                Button editBtn = new Button();
                editBtn.setIconSclass("z-icon-pencil");
                editBtn.setSclass("action-button edit-button");
                editBtn.setTooltiptext("Edit User");

                Button statusBtn = new Button();
                statusBtn.setIconSclass("z-icon-power-off");
                statusBtn.setSclass("action-button power-button");
                statusBtn.setTooltiptext("ACTIVE".equalsIgnoreCase(user.getStatus())
                        ? "Deactivate User" : "Activate User");

                /* Disable edit and status buttons for ADMIN role */
                boolean isAdmin = ("ADMIN".equalsIgnoreCase(user.getRoleName()))
                        || (user.getRole() != null
                                && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName()));

                if (isAdmin) {
                    editBtn.setDisabled(true);
                    editBtn.setSclass("action-button edit-button z-disabled");
                    editBtn.setTooltiptext("Cannot edit Admin user");
                    statusBtn.setDisabled(true);
                    statusBtn.setSclass("action-button power-button z-disabled");
                    statusBtn.setTooltiptext("Cannot change Admin user status");
                } else {
                    editBtn.addEventListener("onClick", new EventListener<Event>() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            openUserModal(user);
                        }
                    });
                    statusBtn.addEventListener("onClick", new EventListener<Event>() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            changeUserStatus(user);
                        }
                    });
                }

                actionBox.appendChild(editBtn);
                actionBox.appendChild(statusBtn);
                actionCell.appendChild(actionBox);
                item.appendChild(actionCell);

                item.setValue(user);
                userListbox.appendChild(item);
                serial++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show("Unable to load users.", "Error",
                    Messagebox.OK, Messagebox.ERROR);
        }
    }

    // ================================================================
    // BUTTON EVENTS
    // ================================================================

    public void onClick$createUserButton(Event event) throws Exception {
        openUserModal(null);
    }

    public void onClick$searchUserButton(Event event) throws Exception {
        userPaging.setActivePage(0);
        userPaging.setTotalSize(userService.getUserCount());
        loadUsers(0);
    }

    public void onClick$clearUserFilterButton(Event event) throws Exception {
        userSearchTextbox.setValue("");
        if (roleFilterCombobox.getItemCount() > 0)   roleFilterCombobox.setSelectedIndex(0);
        if (statusFilterCombobox.getItemCount() > 0) statusFilterCombobox.setSelectedIndex(0);
        userPaging.setActivePage(0);
        userPaging.setTotalSize(userService.getUserCount());
        loadUsers(0);
    }

    // ================================================================
    // FILTERS
    // ================================================================

    private void loadRoleFilter() {

        roleFilterCombobox.getItems().clear();

        Comboitem allItem = new Comboitem("All Roles");
        allItem.setValue(null);
        roleFilterCombobox.appendChild(allItem);

        try {
            for (Role role : roleService.getAllRoles()) {
                if ("ADMIN".equalsIgnoreCase(role.getRoleName())) continue;
                Comboitem item = new Comboitem(role.getRoleName());
                item.setValue(role.getRoleId());
                roleFilterCombobox.appendChild(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        roleFilterCombobox.setSelectedIndex(0);
    }

    private void loadStatusFilter() {

        statusFilterCombobox.getItems().clear();

        Comboitem all = new Comboitem("All Statuses");
        all.setValue(null);
        statusFilterCombobox.appendChild(all);

        Comboitem active = new Comboitem("ACTIVE");
        active.setValue("ACTIVE");
        statusFilterCombobox.appendChild(active);

        Comboitem inactive = new Comboitem("INACTIVE");
        inactive.setValue("INACTIVE");
        statusFilterCombobox.appendChild(inactive);

        statusFilterCombobox.setSelectedIndex(0);
    }

    // ================================================================
    // CREATE / EDIT MODAL
    // ================================================================

    private void openUserModal(User existingUser) {

        boolean editMode = existingUser != null;

        Window window = new Window();
        window.setTitle(editMode ? "Edit User" : "Create User");
        window.setWidth("500px");
        window.setBorder("normal");
        window.setClosable(true);
        window.setSizable(false);
        window.setStyle("padding:20px;");

        Vbox mainBox = new Vbox();
        mainBox.setSpacing("15px");
        mainBox.setWidth("100%");

        /* Username */
        Label usernameLabel = new Label("Username");
        Textbox usernameBox = new Textbox();
        usernameBox.setWidth("100%");
        usernameBox.setPlaceholder("Enter username");
        if (editMode) {
            usernameBox.setValue(existingUser.getUsername());
            usernameBox.setReadonly(true);
        }
        mainBox.appendChild(usernameLabel);
        mainBox.appendChild(usernameBox);

        /* Password */
        Label passwordLabel = new Label("Password");
        Textbox passwordBox = new Textbox();
        passwordBox.setType("password");
        passwordBox.setWidth("100%");
        passwordBox.setPlaceholder(editMode
                ? "Leave blank to keep current password"
                : "Enter password");
        mainBox.appendChild(passwordLabel);
        mainBox.appendChild(passwordBox);

        /* Role */
        Label roleLabel = new Label("Role");
        Combobox roleCombo = new Combobox();
        roleCombo.setWidth("100%");
        roleCombo.setReadonly(true);
        roleCombo.setPlaceholder("Select role");

        try {
            for (Role role : roleService.getAllRoles()) {
                if ("ADMIN".equalsIgnoreCase(role.getRoleName())) continue;
                Comboitem ci = new Comboitem(role.getRoleName());
                ci.setValue(role.getRoleId());
                roleCombo.appendChild(ci);
                if (editMode && existingUser.getRole() != null
                        && existingUser.getRole().getRoleId().equals(role.getRoleId())) {
                    roleCombo.setSelectedItem(ci);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mainBox.appendChild(roleLabel);
        mainBox.appendChild(roleCombo);

        /* Buttons */
        Hbox btnBox = new Hbox();
        btnBox.setSpacing("10px");
        btnBox.setAlign("end");
        btnBox.setWidth("100%");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.addEventListener("onClick", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                window.detach();
            }
        });

        Button saveBtn = new Button(editMode ? "Update User" : "Create User");
        saveBtn.setSclass("primary-button");
        saveBtn.addEventListener("onClick", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                saveUser(window, existingUser, usernameBox, passwordBox, roleCombo);
            }
        });

        btnBox.appendChild(cancelBtn);
        btnBox.appendChild(saveBtn);
        mainBox.appendChild(btnBox);

        window.appendChild(mainBox);
        window.setPage(userListbox.getPage());
        window.doModal();
    }

    // ================================================================
    // SAVE USER
    // ================================================================

    private void saveUser(Window window, User existingUser,
            Textbox usernameBox, Textbox passwordBox, Combobox roleCombo) {

        String username = usernameBox.getValue().trim();
        String password = passwordBox.getValue();

        if (username.isEmpty()) {
            Messagebox.show("Username is required.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        if (existingUser == null && (password == null || password.isEmpty())) {
            Messagebox.show("Password is required.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        if (roleCombo.getSelectedItem() == null) {
            Messagebox.show("Please select a role.", "Validation",
                    Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        Long roleId = (Long) roleCombo.getSelectedItem().getValue();

        try {

            if (existingUser == null) {

                User user = new User();
                user.setUsername(username);
                user.setPasswordHash(password);
                Role role = new Role();
                role.setRoleId(roleId);
                user.setRole(role);
                user.setStatus("ACTIVE");

                boolean created = userService.createUser(user);

                if (!created) {
                    Messagebox.show(
                            "Unable to create user. Username may already exist.",
                            "Error", Messagebox.OK, Messagebox.ERROR);
                    return;
                }

                Messagebox.show("User created successfully.", "Success",
                        Messagebox.OK, Messagebox.INFORMATION);

            } else {

                Role role = new Role();
                role.setRoleId(roleId);
                existingUser.setRole(role);

                if (password != null && !password.isEmpty()) {
                    existingUser.setPasswordHash(password);
                }

                userService.updateUser(existingUser);

                Messagebox.show("User updated successfully.", "Success",
                        Messagebox.OK, Messagebox.INFORMATION);
            }

            window.detach();
            int activePage = userPaging.getActivePage();
            userPaging.setTotalSize(userService.getUserCount());
            loadUsers(activePage * PAGE_SIZE);

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show("Unable to save user.", "Error",
                    Messagebox.OK, Messagebox.ERROR);
        }
    }

    // ================================================================
    // CHANGE STATUS
    // ================================================================

    private void changeUserStatus(User user) {

        boolean isActive = "ACTIVE".equalsIgnoreCase(user.getStatus());
        String newStatus = isActive ? "INACTIVE" : "ACTIVE";
        String action    = isActive ? "deactivate" : "activate";

        Messagebox.show(
                "Are you sure you want to " + action + " user '"
                        + user.getUsername() + "'?",
                action.substring(0, 1).toUpperCase() + action.substring(1) + " User",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        if ("onYes".equals(event.getName())) {
                            try {
                                userService.updateUserStatus(user.getUserId(), newStatus);
                                Messagebox.show("User " + action + "d successfully.",
                                        "Success", Messagebox.OK, Messagebox.INFORMATION);
                                loadUsers(userPaging.getActivePage() * PAGE_SIZE);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Messagebox.show("Unable to " + action + " user.",
                                        "Error", Messagebox.OK, Messagebox.ERROR);
                            }
                        }
                    }
                });
    }

    // ================================================================
    // DELETE USER
    // ================================================================

    private void confirmDeleteUser(User user) {

        Messagebox.show(
                "Are you sure you want to permanently delete user '"
                        + user.getUsername() + "'?",
                "Delete User",
                Messagebox.YES | Messagebox.NO,
                Messagebox.EXCLAMATION,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        if ("onYes".equals(event.getName())) {
                            try {
                                userService.deleteUser(user.getUserId());
                                Messagebox.show("User deleted successfully.",
                                        "Success", Messagebox.OK, Messagebox.INFORMATION);
                                userPaging.setTotalSize(userService.getUserCount());
                                loadUsers(userPaging.getActivePage() * PAGE_SIZE);
                            } catch (Exception e) {
                                e.printStackTrace();
                                String msg = "Unable to delete user.";
                                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")) {
                                    msg = "Cannot delete user — they have related records in the system (sessions, batches, or audit logs). Remove those records first.";
                                } else if (e instanceof IllegalStateException) {
                                    msg = e.getMessage();
                                }
                                Messagebox.show(msg, "Error", Messagebox.OK, Messagebox.ERROR);
                            }
                        }
                    }
                });
    }
}
