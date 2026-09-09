package com.cts.admin.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import com.cts.admin.model.Role;
import com.cts.admin.service.RoleService;
import com.cts.admin.service.RoleServiceImpl;

public class RoleController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private Listbox roleListbox;
    private RoleService roleService;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        roleService = new RoleServiceImpl();
        loadRoles();
    }

    private void loadRoles() {

        try {

            List<Role> roles = roleService.getAllRoles();

            roleListbox.getItems().clear();

            if (roles == null || roles.isEmpty()) return;

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
                Label descLabel = new Label(
                        role.getDescription() == null ? "-" : role.getDescription());
                descLabel.setSclass("role-description-label");
                descCell.appendChild(descLabel);
                item.appendChild(descCell);

                /* ── Status badge ── */
                Listcell statusCell = new Listcell();
                Hbox badge = new Hbox();
                boolean isActive = "ACTIVE".equalsIgnoreCase(role.getStatus());
                badge.setSclass(isActive
                        ? "role-status-badge role-status-active"
                        : "role-status-badge role-status-inactive");
                Label dot = new Label("●");
                dot.setSclass("role-status-dot");
                Label statusLabel = new Label(role.getStatus() == null ? "-" : role.getStatus());
                statusLabel.setSclass("role-status-label");
                badge.appendChild(dot);
                badge.appendChild(statusLabel);
                statusCell.appendChild(badge);
                item.appendChild(statusCell);

                item.setValue(role);
                roleListbox.appendChild(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show("Unable to load system roles.", "Roles",
                    Messagebox.OK, Messagebox.ERROR);
        }
    }

}
