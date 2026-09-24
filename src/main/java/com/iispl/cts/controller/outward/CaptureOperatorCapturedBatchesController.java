package com.iispl.cts.controller.outward;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;

import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.CaptureOperatorBatchService;

public class CaptureOperatorCapturedBatchesController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox capturedBatchesList;

    @Wire
    private Paging batchPaging;

    private CaptureOperatorBatchService service;
    
    private SessionService sessionService;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Session session = Executions.getCurrent().getSession();

        if (session == null) {
            Executions.sendRedirect("/login.zul");
            return;
        }

        Object sessionUserId = session.getAttribute("userId");

        if (sessionUserId == null) {
            Executions.sendRedirect("/login.zul");
            return;
        }
        
        long userId;
        
        if (sessionUserId instanceof Number) {
            userId = ((Number) sessionUserId).longValue();
        } else {
            try {
                userId = Long.parseLong(sessionUserId.toString());
            } catch (NumberFormatException e) {
                Executions.sendRedirect("/login.zul");
                return;
            }
        }
        
        
        sessionService = new SessionServiceImpl();

		com.cts.admin.model.Session clearingSession =
				sessionService.getActiveSession();

        service = new CaptureOperatorBatchService();
        

		if (clearingSession == null
				|| clearingSession.getStatus() == null
				|| !"STARTED".equalsIgnoreCase(
						clearingSession.getStatus().trim())) {

			Messagebox.show(
					"Clearing session is not started.\n\n"
							+ "Capture Operator operations "
							+ "are currently unavailable.",
							"Session Not Started",
							Messagebox.OK,
							Messagebox.EXCLAMATION,
							event -> {

								if (Messagebox.ON_OK.equals(
										event.getName())) {

									Executions.sendRedirect("/zul/login.zul");
								}
							});

			return;
		}

        loadCapturedBatches();
    }

    private void loadCapturedBatches() {
        if (capturedBatchesList == null) {
            return;
        }

        if (service == null) {
            return;
        }

        try {
            List<OutwardBatch> batches =
                    service.getCapturedBatches();

            if (batches == null) {
                batches = new ArrayList<>();
            }

            ListModelList<OutwardBatch> model =
                    new ListModelList<>();

            model.addAll(batches);

            capturedBatchesList.setItemRenderer(
                    new ListitemRenderer<OutwardBatch>() {
                        @Override
                        public void render(
                                Listitem item,
                                OutwardBatch batch,
                                int index) {

                            if (batch == null) {
                                return;
                            }

                            Listcell batchNumberCell =
                                    new Listcell(
                                            safe(batch.getBatchNumber())
                                    );

                            item.appendChild(batchNumberCell);

                            Listcell chequeCountCell =
                                    new Listcell(
                                            String.valueOf(
                                                    batch.getNumberOfCheques()
                                            )
                                    );

                            item.appendChild(chequeCountCell);

                            Listcell statusCell =
                                    new Listcell(
                                            safe(batch.getBatchStatus())
                                    );

                            item.appendChild(statusCell);
                        }
                    }
            );

            capturedBatchesList.setModel(model);

            if (batchPaging != null) {
                batchPaging.setPageSize(10);
                batchPaging.setDetailed(false);
                capturedBatchesList.setPaginal(batchPaging);
            }

        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                    "Unable to load captured batches from database.\n\n"
                            + "Error: "
                            + e.getMessage(),
                    "Captured Batches",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}