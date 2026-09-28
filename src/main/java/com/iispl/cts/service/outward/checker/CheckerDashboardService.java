package com.iispl.cts.service.outward.checker;

import java.util.Collections;
import java.util.List;

import com.iispl.cts.dao.outward.checker.CheckerAssignmentDAO;
import com.iispl.cts.dao.outward.checker.CheckerDashboardDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class CheckerDashboardService {

    private final CheckerDashboardDAO dao;
    private final CheckerAssignmentDAO assignmentDao;

    public CheckerDashboardService() {
        this.dao = new CheckerDashboardDAO();
        this.assignmentDao = new CheckerAssignmentDAO();
    }

    public List<OutwardBatch> getBatches(String checkerUserId) {
        if (checkerUserId == null || checkerUserId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return dao.getCheckerBatches(checkerUserId.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean hasReVerifiedCheques(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null || batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {
            return false;
        }

        try {
            return dao.hasReVerifiedCheques(
                    batchNumber.trim(),
                    checkerUserId.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean releaseBatchLock(
            String batchNumber,
            long checkerUserId) {

        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            return false;
        }

        if (checkerUserId <= 0) {
            return false;
        }

        return assignmentDao.releaseBatchLock(
                batchNumber.trim(),
                checkerUserId);
    }

    public int getReVerifiedChequeCount(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null || batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {
            return 0;
        }

        try {
            return dao.getReVerifiedChequeCount(
                    batchNumber.trim(),
                    checkerUserId.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public OutwardBatch findBatch(String batchNumber) {
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            return null;
        }

        try {
            return dao.findBatch(batchNumber.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean assignBatch(
            String batchNumber,
            long checkerUserId) {

        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            return false;
        }

        try {
            return assignmentDao.takeBatch(
                    batchNumber.trim(),
                    checkerUserId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isBatchAvailable(String batchNumber) {
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            return false;
        }

        try {
            OutwardBatch batch = findBatch(batchNumber);

            if (batch == null) {
                return false;
            }

            return "AVAILABLE".equalsIgnoreCase(
                    batch.getLockStatus());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isAssignedToChecker(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null || batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {
            return false;
        }

        try {
            long checkerId = Long.parseLong(checkerUserId.trim());

            return assignmentDao.isBatchAssignedToChecker(
                    batchNumber.trim(),
                    checkerId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<OutwardBatch> getReVerifyBatches(
            String checkerUserId) {

        if (checkerUserId == null || checkerUserId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return dao.getReVerifyBatches(checkerUserId.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean hasPendingMakerCheques(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null || batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {
            return false;
        }

        try {
            return dao.hasPendingMakerCheques(
                    batchNumber.trim(),
                    checkerUserId.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getReVerifiedChequeNumbers(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null || batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return dao.getReVerifiedChequeNumbers(
                    batchNumber.trim(),
                    checkerUserId.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}