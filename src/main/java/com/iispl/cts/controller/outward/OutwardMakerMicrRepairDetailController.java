package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.OutwardMakerMicrRepairDetailService;
import com.iispl.cts.service.outward.OutwardValidationService;

public class OutwardMakerMicrRepairDetailController
extends SelectorComposer<Component> {

private static final long serialVersionUID = 1L;

@Wire
private Label batchIdLabel;


@Wire
private Image frontImage;

@Wire
private Image backImage;

@Wire
private Textbox chequeNumberTextbox;

@Wire
private Label currentStatusLabel;

@Wire
private Textbox cityCodeTextbox;

@Wire
private Textbox bankCodeTextbox;

@Wire
private Textbox branchCodeTextbox;

@Wire
private Textbox originalMicrTextbox;


@Wire
private Label cityCodeLabel;

@Wire
private Label bankCodeLabel;

@Wire
private Label branchCodeLabel;

private List<OutwardCheque> cheques;

private int currentIndex = 0;

private String batchNumber;

private OutwardMakerMicrRepairDetailService service;

private OutwardValidationService validationService;

@Override
public void doAfterCompose(Component comp)
        throws Exception {

    super.doAfterCompose(comp);

    service = new OutwardMakerMicrRepairDetailService();
    validationService = new OutwardValidationService();

    batchNumber =
            Executions.getCurrent()
                    .getParameter("batchNumber");

    if (batchNumber == null ||
            batchNumber.trim().isEmpty()) {

        Messagebox.show(
                "Batch number is missing.",
                "Error",
                Messagebox.OK,
                Messagebox.ERROR);

        return;
    }

    batchNumber = batchNumber.trim();

    batchIdLabel.setValue(batchNumber);

    loadCheques();
}


private void loadCheques() {

    cheques = service.getMicrErrorCheques(batchNumber);

    if (cheques == null || cheques.isEmpty()) {
        Messagebox.show("No MICR error cheques found for this batch.","Information",Messagebox.OK,Messagebox.INFORMATION);
        return;
    }

    currentIndex = 0;
    loadCurrentCheque();
}

private void loadCurrentCheque() {

    if (cheques == null || cheques.isEmpty()) {
        return;
    }

    if (currentIndex < 0) {
        currentIndex = 0;
    }

    if (currentIndex >= cheques.size()) {
        currentIndex = cheques.size() - 1;
    }

    OutwardCheque cheque = cheques.get(currentIndex);
   
   
    cityCodeLabel.setSclass("single-field-label");
    bankCodeLabel.setSclass("single-field-label");
    branchCodeLabel.setSclass("single-field-label");

    String micrErrorType =
    validationService.getMicrErrorType(cheque);

    // Highlight only the incorrect component
    if ("CITY".equals(micrErrorType)) {
        cityCodeLabel.setSclass("micr-component-error");

    } else if ("BANK".equals(micrErrorType)) {
        bankCodeLabel.setSclass("micr-component-error");

    } else if ("BRANCH".equals(micrErrorType)) {
        branchCodeLabel.setSclass("micr-component-error");
    }


    chequeNumberTextbox.setValue(safe(cheque.getChequeNumber()));
    currentStatusLabel.setValue(safe(cheque.getChequeStatus()));
    cityCodeTextbox.setValue(safe(cheque.getCityCode()));
    bankCodeTextbox.setValue(safe(cheque.getBankCode()));
    branchCodeTextbox.setValue(safe(cheque.getBranchCode()));
   
    originalMicrTextbox.setValue(buildMicr(cheque.getCityCode(),cheque.getBankCode(),cheque.getBranchCode()));
    loadImages(cheque);

}


private String safe(String value) {
    return value == null ? "" : value;
}


private String buildMicr(String cityCode,String bankCode,String branchCode) {
    return safe(cityCode) + safe(bankCode) + safe(branchCode);
}


private void loadImages(OutwardCheque cheque) {

    String frontPath = cheque.getFrontImagePath();
    String backPath = cheque.getBackImagePath();
   
    if (frontPath != null && !frontPath.trim().isEmpty()) {
        frontImage.setSrc(frontPath);
    }
    else {
        frontImage.setSrc("https://placehold.co/900x400?text=Cheque+Front");
    }

    if (backPath != null &&!backPath.trim().isEmpty()) {
        backImage.setSrc(backPath);
    }
    else {
        backImage.setSrc("https://placehold.co/900x400?text=Cheque+Back");
    }
}


@Listen("onClick = #saveNextButton")
public void saveAndNext() {
    if (cheques == null || cheques.isEmpty()) {
        return;
    }

    OutwardCheque cheque = cheques.get(currentIndex);

    String cityCode = cityCodeTextbox.getValue().trim();
    String bankCode = bankCodeTextbox.getValue().trim();
    String branchCode = branchCodeTextbox.getValue().trim();


    if (cityCode.isEmpty()) {
        Messagebox.show("Please enter the corrected city code.","Validation",Messagebox.OK,Messagebox.EXCLAMATION);
        return;
    }


    if (bankCode.isEmpty()) {
        Messagebox.show("Please enter the corrected bank code.","Validation",Messagebox.OK,Messagebox.EXCLAMATION);
        return;
    }


    if (branchCode.isEmpty()) {
        Messagebox.show("Please enter the corrected branch code.","Validation",Messagebox.OK,Messagebox.EXCLAMATION);
        return;
    }


    boolean updated = service.updateCorrectedMicr(batchNumber,
                    cheque.getChequeNumber(),
                    cityCode,
                    bankCode,
                    branchCode);


    if (!updated) {

        Messagebox.show(
                "MICR repair could not be saved.",
                "Error",
                Messagebox.OK,
                Messagebox.ERROR);

        return;
    }


    /*
     * Update the current object after
     * successful database update.
     */

    cheque.setCityCode(cityCode);

    cheque.setBankCode(bankCode);

    cheque.setBranchCode(branchCode);

    cheque.setChequeStatus(
            "MICR_REPAIRED");


    /*
     * Check whether any MICR_ERROR
     * cheques remain in the batch.
     */

    boolean remaining =
            service.hasRemainingMicrErrors(
                    batchNumber);


    if (!remaining) {

        boolean batchUpdated =
                service.updateBatchStatus(
                        batchNumber);


        if (!batchUpdated) {

            Messagebox.show(
                    "MICR repair completed, but batch status could not be updated.",
                    "Warning",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }


        Messagebox.show(
                "All MICR errors have been repaired.\n"
                + "Batch status changed to MICR_REPAIR_COMPLETED.",
                "MICR Repair Completed",
                Messagebox.OK,
                Messagebox.INFORMATION);


        Executions.getCurrent()
                .sendRedirect(
                        "outward-maker-micr-repair.zul");

        return;
    }


    /*
     * Remove repaired cheque from the
     * current MICR_ERROR list.
     */

    cheques.remove(currentIndex);


    /*
     * If another MICR_ERROR cheque exists,
     * show it automatically.
     */

    if (!cheques.isEmpty()) {

        if (currentIndex >= cheques.size()) {

            currentIndex =
                    cheques.size() - 1;
        }

        loadCurrentCheque();

    } else {

        Executions.getCurrent()
                .sendRedirect(
                        "outward-maker-micr-repair.zul");
    }
}


@Listen("onClick = #prevButton")
public void previousCheque() {

    if (cheques == null ||
            cheques.isEmpty()) {

        return;
    }


    if (currentIndex > 0) {

        currentIndex--;

        loadCurrentCheque();

    } else {

        Messagebox.show(
                "This is the first cheque.",
                "Information",
                Messagebox.OK,
                Messagebox.INFORMATION);
    }
}


@Listen("onClick = #backButton")
public void back() {

    Executions.getCurrent()
            .sendRedirect(
                    "outward-maker-micr-repair.zul");
}

}
