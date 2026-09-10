package com.iispl.cts.dao.outward;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class OutwardMakerDashboardDAO {

	private final javax.sql.DataSource dataSource =ConnectionPool.getDataSource();

    // ============================================================
    // GET ALL BATCHES
    // ============================================================

    public List<OutwardBatch> getBatches() throws SQLException {

        List<OutwardBatch> batches = new ArrayList<>();

        String sql =
                "SELECT " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +

                // Maker assignment
                "    mba.user_id AS maker_user_id, " +
                "    mba.assigned_at AS maker_assigned_at, " +
                "    mba.started_at AS maker_started_at, " +
                "    mba.completed_at AS maker_completed_at, " +
                "    mba.assignment_status AS maker_assignment_status " +

                "FROM public.outward_batch ob " +

                "LEFT JOIN public.outward_batch_assignment mba " +
                "    ON ob.batch_number = mba.batch_number " +
                "    AND UPPER(mba.assignment_role) = 'MAKER' " +
                "    AND UPPER(mba.assignment_status) IN " +
                "        ('ASSIGNED', 'IN_PROGRESS') " +

                "ORDER BY ob.batch_number";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                OutwardBatch batch = new OutwardBatch();

                // ====================================================
                // BASIC BATCH INFORMATION
                // ====================================================

                batch.setBatchNumber(
                        rs.getString("batch_number")
                );

                batch.setBranchCode(
                        rs.getString("branch_code")
                );

                batch.setNumberOfCheques(
                        rs.getInt("cheque_count")
                );

                batch.setBatchFolderPath(
                        rs.getString("batch_folder_path")
                );

                // ====================================================
                // CREATED BY
                // ====================================================

                int createdBy = rs.getInt("created_by");

                if (!rs.wasNull()) {
                    batch.setCreatedBy(
                            String.valueOf(createdBy)
                    );
                }

                // ====================================================
                // CREATED AT
                // ====================================================

                Timestamp createdAt =
                        rs.getTimestamp("created_at");

                if (createdAt != null) {
                    batch.setCreatedAt(
                            createdAt.toLocalDateTime()
                    );
                }

                // ====================================================
                // BATCH STATUS
                // ====================================================

                batch.setBatchStatus(
                        rs.getString("batch_status")
                );

                // ====================================================
                // MAKER ASSIGNMENT
                // ====================================================

                int makerUserId =
                        rs.getInt("maker_user_id");

                if (!rs.wasNull()) {

                    String makerUser =
                            String.valueOf(makerUserId);

                    batch.setMakerUserNumber(
                            makerUser
                    );

                    /*
                     * The model contains lockedBy even though the
                     * database stores the assignment in
                     * outward_batch_assignment.
                     */
                    batch.setLockedBy(
                            makerUser
                    );
                }

                // ====================================================
                // MAKER ASSIGNED AT
                // ====================================================

                Timestamp makerAssignedAt =
                        rs.getTimestamp("maker_assigned_at");

                if (makerAssignedAt != null) {

                    batch.setMakerStartedAt(
                            makerAssignedAt.toLocalDateTime()
                    );

                    batch.setLockedAt(
                            makerAssignedAt.toLocalDateTime()
                    );
                }

                // ====================================================
                // MAKER STARTED AT
                // ====================================================

                Timestamp makerStartedAt =
                        rs.getTimestamp("maker_started_at");

                if (makerStartedAt != null) {

                    batch.setMakerStartedAt(
                            makerStartedAt.toLocalDateTime()
                    );
                }

                // ====================================================
                // MAKER COMPLETED AT
                // ====================================================

                Timestamp makerCompletedAt =
                        rs.getTimestamp("maker_completed_at");

                if (makerCompletedAt != null) {

                    batch.setMakerCompletedAt(
                            makerCompletedAt.toLocalDateTime()
                    );
                }

                // ====================================================
                // MAKER ASSIGNMENT STATUS
                // ====================================================

                String assignmentStatus =
                        rs.getString("maker_assignment_status");

                batch.setMakerAssignmentStatus(
                        assignmentStatus
                );

                // ====================================================
                // LOCK STATUS
                // ====================================================

                if (assignmentStatus != null) {

                    if ("IN_PROGRESS".equalsIgnoreCase(
                            assignmentStatus)) {

                        batch.setLockStatus(
                                "IN_PROGRESS"
                        );

                    } else if ("ASSIGNED".equalsIgnoreCase(
                            assignmentStatus)) {

                        batch.setLockStatus(
                                "LOCKED"
                        );
                    }

                } else {

                    /*
                     * No active Maker assignment means the batch
                     * is available from the assignment perspective.
                     */
                    batch.setLockStatus(
                            "AVAILABLE"
                    );
                }

                batches.add(batch);
            }
        }

        return batches;
    }

    // ============================================================
    // ASSIGN BATCH TO MAKER
    // ============================================================

    public boolean assignBatch(
            String batchNumber,
            String userId) throws SQLException {

        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            throw new SQLException("Batch number is required.");
        }

        if (userId == null || userId.trim().isEmpty()) {
            throw new SQLException("User ID is required.");
        }

        int makerId;

        try {
            makerId = Integer.parseInt(userId.trim());
        } catch (NumberFormatException e) {
            throw new SQLException(
                    "Invalid maker user ID: " + userId
            );
        }

        try (Connection con = dataSource.getConnection()) {

            con.setAutoCommit(false);

            try {

                // =================================================
                // 1. VERIFY LOGGED-IN USER IS AN ACTIVE MAKER
                // =================================================

                String userSql =
                        "SELECT u.user_id, u.status, u.role_id " +
                        "FROM public.\"user\" u " +
                        "WHERE u.user_id = ? " +
                        "AND UPPER(TRIM(u.status)) = 'ACTIVE' " +
                        "AND u.role_id = 3";

                try (PreparedStatement ps =
                             con.prepareStatement(userSql)) {

                    ps.setInt(1, makerId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new SQLException(
                                    "User " + makerId +
                                    " is not an active Maker. " +
                                    "Expected status ACTIVE and Maker role_id 3."
                            );
                        }
                    }
                }

                // =================================================
                // 2. LOCK BATCH ROW
                // =================================================

                String batchSql =
                        "SELECT batch_status " +
                        "FROM public.outward_batch " +
                        "WHERE batch_number = ? " +
                        "FOR UPDATE";

                String currentStatus;

                try (PreparedStatement ps =
                             con.prepareStatement(batchSql)) {

                    ps.setString(1, batchNumber.trim());

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new SQLException(
                                    "Batch not found: " + batchNumber
                            );
                        }

                        currentStatus =
                                rs.getString("batch_status");
                    }
                }

                // =================================================
                // 3. CHECK EXISTING MAKER ASSIGNMENT
                // =================================================

                String assignmentSql =
                        "SELECT user_id, assignment_status " +
                        "FROM public.outward_batch_assignment " +
                        "WHERE batch_number = ? " +
                        "AND UPPER(assignment_role) = 'MAKER' " +
                        "AND UPPER(assignment_status) IN " +
                        "    ('ASSIGNED', 'IN_PROGRESS') " +
                        "FOR UPDATE";

                Integer existingMakerId = null;
                String existingAssignmentStatus = null;

                try (PreparedStatement ps =
                             con.prepareStatement(assignmentSql)) {

                    ps.setString(1, batchNumber.trim());

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {

                            existingMakerId =
                                    rs.getInt("user_id");

                            existingAssignmentStatus =
                                    rs.getString("assignment_status");
                        }
                    }
                }

                // =================================================
                // 4. IF ALREADY ASSIGNED, ONLY SAME MAKER CAN CONTINUE
                // =================================================

                if (existingMakerId != null) {

                    // Another Maker already owns the batch.
                    if (existingMakerId.intValue() != makerId) {

                        throw new SQLException(
                                "Batch " + batchNumber +
                                " is already assigned to Maker " +
                                existingMakerId + "."
                        );
                    }

                    // Same Maker already has the batch in progress.
                    // Do NOT create another assignment.
                    if ("IN_PROGRESS".equalsIgnoreCase(
                            existingAssignmentStatus)) {

                        con.commit();
                        return true;
                    }

                    // Same Maker owns the batch but assignment is ASSIGNED.
                    // Resume it.
                    if ("ASSIGNED".equalsIgnoreCase(
                            existingAssignmentStatus)) {

                        String resumeSql =
                                "UPDATE public.outward_batch_assignment " +
                                "SET started_at = COALESCE(" +
                                "started_at, CURRENT_TIMESTAMP), " +
                                "assignment_status = 'IN_PROGRESS' " +
                                "WHERE batch_number = ? " +
                                "AND user_id = ? " +
                                "AND UPPER(assignment_role) = 'MAKER' " +
                                "AND UPPER(assignment_status) = 'ASSIGNED'";

                        try (PreparedStatement ps =
                                     con.prepareStatement(resumeSql)) {

                            ps.setString(1, batchNumber.trim());
                            ps.setInt(2, makerId);

                            ps.executeUpdate();
                        }

                        String statusSql =
                                "UPDATE public.outward_batch " +
                                "SET batch_status = 'ASSIGNED' " +
                                "WHERE batch_number = ?";

                        try (PreparedStatement ps =
                                     con.prepareStatement(statusSql)) {

                            ps.setString(1, batchNumber.trim());

                            ps.executeUpdate();
                        }

                        con.commit();
                        return true;
                    }
                }

                // =================================================
                // 5. NEW ASSIGNMENT ONLY WHEN BATCH IS AVAILABLE
                // =================================================

                if (!"CAPTURED".equalsIgnoreCase(currentStatus)) {

                    throw new SQLException(
                            "Batch " + batchNumber +
                            " is not available for a new Maker assignment. " +
                            "Current status: " + currentStatus
                    );
                }

                // =================================================
                // 6. CREATE MAKER ASSIGNMENT
                // =================================================

                String insertSql =
                        "INSERT INTO public.outward_batch_assignment " +
                        "(batch_number, user_id, assignment_role, " +
                        "assigned_at, started_at, assignment_status) " +
                        "VALUES (?, ?, 'MAKER', " +
                        "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, " +
                        "'IN_PROGRESS')";

                try (PreparedStatement ps =
                             con.prepareStatement(insertSql)) {

                    ps.setString(1, batchNumber.trim());
                    ps.setInt(2, makerId);

                    ps.executeUpdate();
                }

                // =================================================
                // 7. UPDATE BATCH STATUS
                // =================================================

                String updateSql =
                        "UPDATE public.outward_batch " +
                        "SET batch_status = 'ASSIGNED' " +
                        "WHERE batch_number = ?";

                try (PreparedStatement ps =
                             con.prepareStatement(updateSql)) {

                    ps.setString(1, batchNumber.trim());

                    ps.executeUpdate();
                }

                con.commit();

                return true;

            } catch (SQLException e) {

                con.rollback();

                throw e;

            } finally {

                con.setAutoCommit(true);
            }
        }
    }    // ============================================================
    // GET CHEQUES
    // ============================================================
 // ============================================================
 // GET CHEQUES
 // ============================================================

 public List<OutwardCheque> getCheques(String batchNumber) throws SQLException {

     List<OutwardCheque> cheques = new ArrayList<>();

     if (batchNumber == null || batchNumber.trim().isEmpty()) {
         throw new SQLException("Batch number is required.");
     }

     String sql =
             "SELECT " +
             "    oc.batch_number, " +
             "    oc.cheque_number, " +
             "    oc.city_code, " +
             "    oc.bank_code, " +
             "    oc.branch_code, " +
             "    oc.drawer_account_number, " +
             "    oc.drawer_name, " +
             "    oc.payee_account_number, " +
             "    oc.payee_name, " +
             "    oc.amount, " +
             "    oc.amount_in_words, " +
             "    oc.cheque_date, " +
             "    oc.front_image_path, " +
             "    oc.back_image_path, " +
             "    oc.cheque_status, " +
             "    ob.created_by AS batch_created_by, " +
             "    ob.created_at AS batch_created_at " +
             "FROM public.outward_cheque oc " +
             "INNER JOIN public.outward_batch ob " +
             "    ON ob.batch_number = oc.batch_number " +
             "WHERE oc.batch_number = ? " +
             "ORDER BY oc.cheque_number";

     try (Connection con = dataSource.getConnection();
          PreparedStatement ps = con.prepareStatement(sql)) {

         ps.setString(1, batchNumber.trim());

         try (ResultSet rs = ps.executeQuery()) {

             while (rs.next()) {

                 OutwardCheque cheque = new OutwardCheque();

                 // =================================================
                 // BATCH NUMBER
                 // =================================================

                 cheque.setBatchNumber(
                         rs.getString("batch_number")
                 );

                 // =================================================
                 // CHEQUE NUMBER
                 // =================================================

                 cheque.setChequeNumber(
                         rs.getString("cheque_number")
                 );

                 // =================================================
                 // CITY CODE
                 // =================================================

                 cheque.setCityCode(
                         rs.getString("city_code")
                 );

                 // =================================================
                 // BANK CODE
                 // =================================================

                 cheque.setBankCode(
                         rs.getString("bank_code")
                 );

                 // =================================================
                 // BRANCH CODE
                 // =================================================

                 cheque.setBranchCode(
                         rs.getString("branch_code")
                 );

                 // =================================================
                 // DRAWER ACCOUNT NUMBER
                 // =================================================

                 cheque.setDrawerAccountNumber(
                         rs.getString("drawer_account_number")
                 );

                 // =================================================
                 // DRAWER NAME
                 // =================================================

                 cheque.setDrawerName(
                         rs.getString("drawer_name")
                 );

                 // =================================================
                 // DEPOSITOR ACCOUNT NUMBER
                 // =================================================

                 cheque.setDepositorAccountNumber(
                         rs.getString("payee_account_number")
                 );

                 // =================================================
                 // DEPOSITOR NAME
                 // =================================================

                 cheque.setDepositorName(
                         rs.getString("payee_name")
                 );

                 // =================================================
                 // PAYEE NAME
                 // =================================================

                 cheque.setPayeeName(
                         rs.getString("payee_name")
                 );

                 // =================================================
                 // AMOUNT
                 // =================================================

                 cheque.setAmount(
                         rs.getBigDecimal("amount")
                 );

                 // =================================================
                 // AMOUNT IN WORDS
                 // =================================================

                 cheque.setAmountInWords(
                         rs.getString("amount_in_words")
                 );

                 // =================================================
                 // CHEQUE DATE
                 // =================================================

                 Date chequeDate = rs.getDate("cheque_date");

                 if (chequeDate != null) {
                     cheque.setChequeDate(
                             chequeDate.toLocalDate()
                     );
                 } else {
                     cheque.setChequeDate(null);
                 }

                 // =================================================
                 // FRONT IMAGE PATH
                 // =================================================

                 cheque.setFrontImagePath(
                         rs.getString("front_image_path")
                 );

                 // =================================================
                 // BACK IMAGE PATH
                 // =================================================

                 cheque.setBackImagePath(
                         rs.getString("back_image_path")
                 );

                 // =================================================
                 // CHEQUE STATUS
                 // =================================================

                 cheque.setChequeStatus(
                         rs.getString("cheque_status")
                 );

                 // =================================================
                 // CREATED BY
                 // =================================================

                 int batchCreatedBy =
                         rs.getInt("batch_created_by");

                 if (!rs.wasNull()) {
                     cheque.setCreatedBy(
                             String.valueOf(batchCreatedBy)
                     );
                 } else {
                     cheque.setCreatedBy(null);
                 }

                 // =================================================
                 // CREATED AT
                 // =================================================

                 Timestamp batchCreatedAt =
                         rs.getTimestamp("batch_created_at");

                 if (batchCreatedAt != null) {
                     cheque.setCreatedAt(
                             batchCreatedAt.toLocalDateTime()
                     );
                 } else {
                     cheque.setCreatedAt(null);
                 }

                 // =================================================
                 // UPDATED BY
                 // =================================================

                 cheque.setUpdatedBy(null);

                 // =================================================
                 // UPDATED AT
                 // =================================================

                 cheque.setUpdatedAt(null);

                 // =================================================
                 // ADD CHEQUE
                 // =================================================

                 cheques.add(cheque);
             }
         }
     }

     return cheques;
 }
    // ============================================================
    // CHECK BATCH EXISTS
    // ============================================================

    public boolean isBatchValid(
            String batchNumber) throws SQLException {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        String sql =
                "SELECT 1 " +
                "FROM public.outward_batch " +
                "WHERE batch_number = ?";

        try (Connection con =
                     CTSStaticData.getConnection();

             PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                return rs.next();
            }
        }
    }

    // ============================================================
    // COMPLETE BATCH
    // ============================================================

    public void updateBatchIfCompleted(
            String batchNumber) throws SQLException {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new SQLException(
                    "Batch number is required."
            );
        }

        try (Connection con =
        		dataSource.getConnection()) {

            con.setAutoCommit(false);

            try {

                // ================================================
                // 1. COMPLETE MAKER ASSIGNMENT
                // ================================================

                String assignmentSql =
                        "UPDATE public.outward_batch_assignment " +
                        "SET assignment_status = 'COMPLETED', " +
                        "completed_at = CURRENT_TIMESTAMP " +
                        "WHERE batch_number = ? " +
                        "AND UPPER(assignment_role) = 'MAKER' " +
                        "AND UPPER(assignment_status) = 'IN_PROGRESS'";

                try (PreparedStatement ps =
                             con.prepareStatement(
                                     assignmentSql)) {

                    ps.setString(
                            1,
                            batchNumber.trim()
                    );

                    ps.executeUpdate();
                }

                // ================================================
                // 2. COMPLETE BATCH
                // ================================================

                String batchSql =
                        "UPDATE public.outward_batch " +
                        "SET batch_status = 'COMPLETED' " +
                        "WHERE batch_number = ?";

                try (PreparedStatement ps =
                             con.prepareStatement(batchSql)) {

                    ps.setString(
                            1,
                            batchNumber.trim()
                    );

                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException e) {

                con.rollback();

                throw e;

            } finally {

                con.setAutoCommit(true);
            }
        }
    }
   
 // ============================================================
 // UPDATE CHEQUE STATUS
 // ============================================================

 public boolean updateChequeStatus(
         String batchNumber,
         String chequeNumber,
         String status) throws SQLException {

     String sql =
             "UPDATE public.outward_cheque "
             + "SET cheque_status = ? "
             + "WHERE batch_number = ? "
             + "AND cheque_number = ?";

     try (Connection con =
    		 dataSource.getConnection();
          PreparedStatement ps =
                  con.prepareStatement(sql)) {

         ps.setString(1, status);
         ps.setString(2, batchNumber);
         ps.setString(3, chequeNumber);

         return ps.executeUpdate() > 0;
     }
 }


 // ============================================================
 // UPDATE BATCH STATUS
 // ============================================================

 public boolean updateBatchStatus(
         String batchNumber,
         String status) throws SQLException {

     String sql =
             "UPDATE public.outward_batch "
             + "SET batch_status = ? "
             + "WHERE batch_number = ?";

     try (Connection con =
    		 dataSource.getConnection();
          PreparedStatement ps =
                  con.prepareStatement(sql)) {

         ps.setString(1, status);
         ps.setString(2, batchNumber);

         return ps.executeUpdate() > 0;
     }
 }
}

