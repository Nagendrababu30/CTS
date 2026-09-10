

package com.iispl.cts.service.outward.checker;

import java.util.Collections;
import java.util.List;

import com.iispl.cts.dao.outward.checker.CheckerBatchDAO;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class CheckerBatchService {

    private final CheckerBatchDAO batchDAO;


    // ============================================
    // CONSTRUCTOR
    // ============================================

    public CheckerBatchService() {

        batchDAO =
                new CheckerBatchDAO();

    }


    // ============================================
    // GET CHECKER QUEUE BATCHES
    // ============================================

    public List<OutwardBatch> getCheckerQueueBatches(
            String checkerUserId) {

        if (checkerUserId == null
                || checkerUserId.trim().isEmpty()) {

            return Collections.emptyList();

        }


        return batchDAO.getCheckerBatches(

                checkerUserId.trim()

        );

    }


    // ============================================
    // GET CHEQUES BY BATCH NUMBER
    // ============================================

    public List<OutwardCheque> getChequesByBatchNumber(
            String batchNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return Collections.emptyList();

        }


        return batchDAO.getChequesByBatchNumber(

                batchNumber.trim()

        );

    }


    // ============================================
    // GET CHEQUES BY BATCH ID
    // ============================================

    public List<OutwardCheque> getChequesByBatchId(
            String batchId) {

        if (batchId == null
                || batchId.trim().isEmpty()) {

            return Collections.emptyList();

        }


        return batchDAO.getChequesByBatchId(

                batchId.trim()

        );

    }
 // ============================================
 // VERIFY ACCOUNT
 // ============================================

 public boolean verifyAccount(String accountNumber) {

     if (accountNumber == null
             || accountNumber.trim().isEmpty()) {

         return false;
     }

     return batchDAO.accountExists(
             accountNumber.trim()
     );
 }
}