package com.cts.admin.controller;

import java.text.SimpleDateFormat;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.event.PagingEvent;

import com.cts.admin.model.AuditLog;
import com.cts.admin.service.AuditLogService;
import com.cts.admin.service.AuditLogServiceImpl;
import com.cts.admin.service.RoleService;
import com.cts.admin.service.RoleServiceImpl;

public class AuditLogController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final int PAGE_SIZE = 10;

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");
    

    // ================================================================
    // ZUL COMPONENTS
    // ================================================================

    private Listbox  auditLogListbox;
    private Paging   auditLogPaging;
    private Textbox  auditSearchTextbox;
    private Button   auditSearchButton;
    private Combobox auditRoleCombobox;

    // ================================================================
    // SERVICE
    // ================================================================

    private AuditLogService auditLogService;
    private RoleService     roleService;

    // ================================================================
    // INIT
    // ================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        auditLogService = new AuditLogServiceImpl();
        roleService     = new RoleServiceImpl();

        /* Load role filter from DB */
        loadRoleFilter();

        auditLogPaging.setPageSize(PAGE_SIZE);
        auditLogPaging.setTotalSize(auditLogService.getTotalAuditLogCount());

        loadAuditLogs(0);

        /* Paging event */
        auditLogPaging.addEventListener("onPaging", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent pe = (PagingEvent) event;
                loadAuditLogs(pe.getActivePage() * PAGE_SIZE);
            }
        });

        /* Search button */
        auditSearchButton.addEventListener("onClick", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                auditLogPaging.setActivePage(0);
                auditLogPaging.setTotalSize(
                        auditLogService.getTotalAuditLogCount());
                loadAuditLogs(0);
            }
        });

        /* Enter key in search box */
        auditSearchTextbox.addEventListener("onOK", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                auditLogPaging.setActivePage(0);
                auditLogPaging.setTotalSize(
                        auditLogService.getTotalAuditLogCount());
                loadAuditLogs(0);
            }
        });

        /* Role filter change */
        auditRoleCombobox.addEventListener("onSelect", new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                auditLogPaging.setActivePage(0);
                auditLogPaging.setTotalSize(
                        auditLogService.getTotalAuditLogCount());
                loadAuditLogs(0);
            }
        });
    }

    // ================================================================
    // LOAD ROLE FILTER FROM DB
    // ================================================================

    private void loadRoleFilter() {
        auditRoleCombobox.getItems().clear();
        Comboitem all = new Comboitem("All Roles");
        all.setValue(null);
        auditRoleCombobox.appendChild(all);
        try {
            roleService.getAllRoles().forEach(role -> {
                Comboitem item = new Comboitem(role.getRoleName());
                item.setValue(role.getRoleName());
                auditRoleCombobox.appendChild(item);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        auditRoleCombobox.setSelectedIndex(0);
    }

    // ================================================================
    // LOAD
    // ================================================================

    private void loadAuditLogs(int offset) {

        try {

            int page = (offset / PAGE_SIZE) + 1;

            /* Get selected role filter */
            String roleFilter = null;
            if (auditRoleCombobox.getSelectedItem() != null) {
                String selected = auditRoleCombobox.getSelectedItem().getLabel();
                if (selected != null && !selected.equals("All Roles")) {
                    roleFilter = selected;
                }
            }

            /* Get search text */
            String searchText = auditSearchTextbox.getValue();
            if (searchText != null) searchText = searchText.trim();

            List<AuditLog> logs =
                    auditLogService.getAuditLogs(page, PAGE_SIZE);

            auditLogListbox.getItems().clear();

            for (AuditLog log : logs) {

                /* Apply client-side role filter */
                if (roleFilter != null && !roleFilter.isEmpty()) {
                    if (log.getRoleName() == null
                            || !log.getRoleName().equalsIgnoreCase(roleFilter)) {
                        continue;
                    }
                }

                /* Apply client-side search filter (by user ID or username) */
                if (searchText != null && !searchText.isEmpty()) {
                    boolean match = false;
                    if (log.getUserId() != null
                            && String.valueOf(log.getUserId()).contains(searchText)) {
                        match = true;
                    }
                    if (log.getUserName() != null
                            && log.getUserName().toLowerCase()
                                    .contains(searchText.toLowerCase())) {
                        match = true;
                    }
                    if (!match) continue;
                }

                Listitem item = new Listitem();

                /* User ID */
                item.appendChild(createCell(
                        log.getUserId() != null
                                ? String.valueOf(log.getUserId()) : "-"));

                /* Role */
                item.appendChild(createCell(
                        log.getRoleName() != null
                                ? log.getRoleName() : "-"));

                /* Login */
                item.appendChild(createCell(
                        log.getLoginTime() != null
                                ? formatDateTime(log.getLoginTime()) : "-"));

                /* Logout */
                item.appendChild(createCell(
                        log.getLogoutTime() != null
                                ? formatDateTime(log.getLogoutTime()) : "-"));

                auditLogListbox.appendChild(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show("Unable to load audit logs.", "Audit Logs",
                    Messagebox.OK, Messagebox.ERROR);
        }
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private Listcell createCell(String value) {
        Listcell cell = new Listcell();
        Label label = new Label(value != null ? value : "-");
        cell.appendChild(label);
        return cell;
    }

    private String formatDateTime(java.util.Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a");
        sdf.setTimeZone(IST);
        return sdf.format(date);
    }
}
