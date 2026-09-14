package com.iispl.cts.service.outward;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.iispl.cts.dao.outward.OutwardMakerDataEntryDetailDAO;
import com.iispl.cts.model.outward.OutwardCheque;

public class OutwardMakerDataEntryDetailService {

    private final OutwardMakerDataEntryDetailDAO dao;


    public OutwardMakerDataEntryDetailService() {

        dao =
                new OutwardMakerDataEntryDetailDAO();
    }


    // =========================================================
    // NORMAL DATA ENTRY
    //
    // Existing functionality.
    //
    // Returns all cheques belonging to the batch.
    // =========================================================

    public List<OutwardCheque> getCheques(
            String batchId) {

        return dao.getCheques(batchId);
    }


    // =========================================================
    // RETURNED DATA ENTRY CHEQUES
    //
    // Used ONLY when:
    //
    //     returnMode = RETURNED
    //
    // This returns only the cheques that were sent back
    // by Checker for Data Entry correction.
    // =========================================================

    public List<OutwardCheque> getReturnedCheques(
            String batchId) {

        return dao.getReturnedCheques(batchId);
    }


    // =========================================================
    // SAVE CHEQUE
    //
    // Existing functionality unchanged.
    // =========================================================

    public void saveCheque(
            OutwardCheque cheque) {

        dao.saveCheque(cheque);
    }


    // =========================================================
    // REJECT CHEQUE
    //
    // Existing functionality unchanged.
    // =========================================================

    public void rejectCheque(
            OutwardCheque cheque,
            String reason) {

        dao.rejectCheque(
                cheque,
                reason
        );
    }


    // =========================================================
    // COMPLETE BATCH DATA ENTRY
    //
    // Existing functionality unchanged.
    // =========================================================

    public boolean completeBatchDataEntry(
            String batchId,
            int currentUserId) {

        return dao.completeBatchDataEntry(
                batchId,
                currentUserId
        );
    }


    // =========================================================
    // SAVE AND VERIFY CHEQUE
    //
    // Existing functionality unchanged.
    // =========================================================

    public boolean saveAndVerifyCheque(
            OutwardCheque cheque,
            int makerId) {

        try {

            // =====================================================
            // 1. UPDATE OUTWARD CHEQUE
            // =====================================================

            dao.saveCheque(cheque);


            // =====================================================
            // 2. ENSURE STATUS IS VERIFIED
            // =====================================================

            dao.updateChequeStatus(
                    cheque.getBatchNumber(),
                    cheque.getChequeNumber(),
                    "VERIFIED"
            );


            // =====================================================
            // 3. SAVE MAKER PROCESSING RECORD
            // =====================================================

            return dao.saveMakerVerify(
                    cheque.getBatchNumber(),
                    cheque.getChequeNumber(),
                    makerId
            );


        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }


    // =========================================================
    // RECORD MAKER REJECT
    //
    // Existing functionality unchanged.
    // =========================================================

    public boolean recordMakerReject(
            String batchNumber,
            String chequeNumber,
            int makerId,
            String reasonId) {

        try {

            dao.updateChequeStatus(
                    batchNumber,
                    chequeNumber,
                    "REJECT_REQUESTED"
            );


            return dao.saveMakerReject(
                    batchNumber,
                    chequeNumber,
                    makerId,
                    reasonId
            );


        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }


    // =========================================================
    // GET RETURN REASONS
    //
    // Existing functionality unchanged.
    // =========================================================

    public Map<String, String> getReturnReasons() {

        return dao.getReturnReasons();
    }
}