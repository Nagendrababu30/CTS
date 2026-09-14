package com.iispl.cts.dao.outward;

import com.cts.inward.config.ConnectionPool;

import com.iispl.cts.data.CTSStaticData;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.ChequeProcessing;

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

    private final javax.sql.DataSource dataSource =
            ConnectionPool.getDataSource();

    // ============================================================
    // GET ALL BATCHES
    // ============================================================
 
    public List<OutwardBatch> getBatches() throws SQLException {

        // =========================================================
        // GET CURRENT LOGGED-IN MAKER USER ID
        // =========================================================

        Session session =
                Executions.getCurrent().getSession();

        Object sessionUserId =
                session != null
                        ? session.getAttribute("userId")
                        : null;

        if (sessionUserId == null) {
            throw new SQLException(
                    "Maker user ID is required."
            );
        }

        int currentMakerUserId;

        try {
            currentMakerUserId =
                    Integer.parseInt(
                            sessionUserId.toString().trim()
                    );
        } catch (NumberFormatException e) {
            throw new SQLException(
                    "Invalid maker user ID: "
                            + sessionUserId
            );
        }

        List<OutwardBatch> batches =
                new ArrayList<>();

        String sql =
                "SELECT " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +

                // =================================================
                // RETURNED CHEQUE COUNT
                // =================================================

                "    COALESCE(rc.returned_cheque_count, 0) " +
                "        AS returned_cheque_count, " +

                // =================================================
                // ORIGINAL MAKER
                //
                // IMPORTANT:
                // ASC is used because the FIRST Maker assignment
                // is the original Maker.
                // =================================================

                "    (SELECT oba.user_id " +
                "       FROM public.outward_batch_assignment oba " +
                "      WHERE oba.batch_number = ob.batch_number " +
                "        AND UPPER(TRIM(oba.assignment_role)) = 'MAKER' " +
                "      ORDER BY oba.assigned_at ASC NULLS LAST " +
                "      LIMIT 1) AS returned_maker_user_id, " +

                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +

                // =================================================
                // ACTIVE MAKER ASSIGNMENT
                //
                // This is intentionally NOT restricted by the
                // logged-in Maker.
                //
                // Therefore every Maker can SEE a locked batch.
                // The assigned Maker information is still returned
                // so the UI can display who owns the lock.
                // =================================================

                "    mba.user_id AS maker_user_id, " +
                "    mba.assigned_at AS maker_assigned_at, " +
                "    mba.started_at AS maker_started_at, " +
                "    mba.completed_at AS maker_completed_at, " +
                "    mba.assignment_status AS maker_assignment_status " +

                "FROM public.outward_batch ob " +

                // =================================================
                // RETURNED CHEQUE COUNT
                // =================================================

                "LEFT JOIN ( " +
                "    SELECT " +
                "        batch_number, " +
                "        COUNT(*) AS returned_cheque_count " +
                "    FROM public.outward_cheque " +
                "    WHERE UPPER(TRIM(cheque_status)) " +
                "          = 'SENT_BACK_TO_MAKER' " +
                "    GROUP BY batch_number " +
                ") rc " +
                "    ON rc.batch_number = ob.batch_number " +

                // =================================================
                // EXISTING / ACTIVE MAKER ASSIGNMENT
                //
                // DO NOT add user_id here.
                //
                // This allows ALL Makers to see a batch which is
                // currently locked by another Maker.
                // =================================================

                "LEFT JOIN public.outward_batch_assignment mba " +
                "    ON ob.batch_number = mba.batch_number " +
                "    AND UPPER(TRIM(mba.assignment_role)) = 'MAKER' " +
                "    AND UPPER(TRIM(mba.assignment_status)) IN " +
                "        ('ASSIGNED', 'IN_PROGRESS', 'RELEASED') " +

                // =================================================
                // BATCH FILTER
                // =================================================

                "WHERE " +

                // =================================================
                // 1. NORMAL MAKER WORKFLOW
                //
                // returned_cheque_count = 0 is important.
                //
                // Otherwise a returned batch could also enter this
                // condition and become visible to every Maker.
                // =================================================

                "      ( " +
                "          COALESCE(rc.returned_cheque_count, 0) = 0 " +
                "          AND " +
                "          UPPER(TRIM(ob.batch_status)) NOT IN " +
                "              ('SUBMITTED_TO_CHECKER', " +
                "               'CHECKER_COMPLETED', " +
                "               'COMPLETED', " +
                "               'REJECTED', " +
                "               'HOLD', " +
                "               'ON_HOLD') " +
                "          AND " +
                "          ( " +

                // -------------------------------------------------
                // NEW CAPTURED BATCH
                //
                // Every Maker can see it.
                // -------------------------------------------------

                "              UPPER(TRIM(ob.batch_status)) = " +
                "                  'CAPTURED' " +

                "              OR " +

                // -------------------------------------------------
                // EXISTING MAKER WORKFLOW
                //
                // IMPORTANT:
                // NO user_id restriction here.
                //
                // Every Maker can see the batch.
                // The assignment/lock still belongs to the Maker
                // already holding it.
                // -------------------------------------------------

                "              EXISTS ( " +
                "                  SELECT 1 " +
                "                  FROM public.outward_batch_assignment current_maker " +
                "                  WHERE current_maker.batch_number = " +
                "                        ob.batch_number " +
                "                    AND UPPER(TRIM(current_maker.assignment_role)) " +
                "                        = 'MAKER' " +
                "              ) " +

                "          ) " +
                "      ) " +

                "   OR " +

                // =================================================
                // 2. RETURNED BATCH
                //
                // Only ORIGINAL Maker can see it.
                //
                // SENT_BACK_TO_MAKER cheque count > 0
                // AND current logged-in Maker = first/original Maker.
                // =================================================

                "      ( " +
                "          COALESCE(rc.returned_cheque_count, 0) > 0 " +
                "          AND " +
                "          EXISTS ( " +
                "              SELECT 1 " +
                "              FROM public.outward_batch_assignment original_maker " +
                "              WHERE original_maker.batch_number = " +
                "                    ob.batch_number " +
                "                AND UPPER(TRIM(original_maker.assignment_role)) " +
                "                    = 'MAKER' " +
                "                AND original_maker.user_id = ? " +
                "                AND original_maker.user_id = " +
                "                    ( " +
                "                        SELECT first_maker.user_id " +
                "                        FROM public.outward_batch_assignment first_maker " +
                "                        WHERE first_maker.batch_number = " +
                "                              ob.batch_number " +
                "                          AND UPPER(TRIM(first_maker.assignment_role)) " +
                "                              = 'MAKER' " +
                "                        ORDER BY first_maker.assigned_at ASC NULLS LAST " +
                "                        LIMIT 1 " +
                "                    ) " +
                "          ) " +
                "      ) " +

                // =================================================
                // IMPORTANT:
                //
                // DO NOT ADD ANOTHER WHERE CLAUSE HERE.
                //
                // The main WHERE above already contains the complete
                // normal + returned batch filtering.
                // =================================================

                "ORDER BY ob.batch_number";


        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)

        ) {

            // =====================================================
            // CURRENT MAKER ID
            //
            // Used ONLY for returned-batch ownership.
            // Normal batches are visible to every Maker.
            // =====================================================

            ps.setInt(
                    1,
                    currentMakerUserId
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()

            ) {

                while (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

                    // ====================================================
                    // BASIC BATCH INFORMATION
                    // ====================================================

                    batch.setBatchNumber(
                            rs.getString(
                                    "batch_number"
                            )
                    );

                    batch.setBranchCode(
                            rs.getString(
                                    "branch_code"
                            )
                    );

                    // ====================================================
                    // NORMAL BATCH COUNT
                    // ====================================================

                    batch.setNumberOfCheques(
                            rs.getInt(
                                    "cheque_count"
                            )
                    );

                    // ====================================================
                    // RETURNED CHEQUE COUNT
                    // ====================================================

                    int returnedChequeCount =
                            rs.getInt(
                                    "returned_cheque_count"
                            );

                    if (returnedChequeCount > 0) {

                        // For returned mode, show ONLY
                        // returned cheque count.

                        batch.setNumberOfCheques(
                                returnedChequeCount
                        );
                    }

                    // ====================================================
                    // BATCH FOLDER PATH
                    // ====================================================

                    batch.setBatchFolderPath(
                            rs.getString(
                                    "batch_folder_path"
                            )
                    );

                    // ====================================================
                    // CREATED BY
                    // ====================================================

                    int createdBy =
                            rs.getInt(
                                    "created_by"
                            );

                    if (!rs.wasNull()) {

                        batch.setCreatedBy(
                                String.valueOf(
                                        createdBy
                                )
                        );
                    }

                    // ====================================================
                    // CREATED AT
                    // ====================================================

                    Timestamp createdAt =
                            rs.getTimestamp(
                                    "created_at"
                            );

                    if (createdAt != null) {

                        batch.setCreatedAt(
                                createdAt.toLocalDateTime()
                        );
                    }

                    // ====================================================
                    // BATCH STATUS
                    // ====================================================

                    batch.setBatchStatus(
                            rs.getString(
                                    "batch_status"
                            )
                    );

                    // ====================================================
                    // RETURNED BATCH DISPLAY STATUS
                    // ====================================================

                    if (returnedChequeCount > 0) {

                        batch.setBatchStatus(
                                "SENT_TO_MAKER"
                        );
                    }

                    // ====================================================
                    // MAKER ASSIGNMENT
                    //
                    // This is the Maker currently holding the lock.
                    //
                    // Therefore Maker 2 can see:
                    //
                    // Batch B001
                    // Locked By Maker 1
                    //
                    // ====================================================

                    int makerUserId =
                            rs.getInt(
                                    "maker_user_id"
                            );

                    if (!rs.wasNull()) {

                        String makerUser =
                                String.valueOf(
                                        makerUserId
                                );

                        batch.setMakerUserNumber(
                                makerUser
                        );

                        batch.setLockedBy(
                                makerUser
                        );
                    }

                    // ====================================================
                    // MAKER ASSIGNED AT
                    // ====================================================

                    Timestamp makerAssignedAt =
                            rs.getTimestamp(
                                    "maker_assigned_at"
                            );

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
                            rs.getTimestamp(
                                    "maker_started_at"
                            );

                    if (makerStartedAt != null) {

                        batch.setMakerStartedAt(
                                makerStartedAt.toLocalDateTime()
                        );
                    }

                    // ====================================================
                    // MAKER COMPLETED AT
                    // ====================================================

                    Timestamp makerCompletedAt =
                            rs.getTimestamp(
                                    "maker_completed_at"
                            );

                    if (makerCompletedAt != null) {

                        batch.setMakerCompletedAt(
                                makerCompletedAt.toLocalDateTime()
                        );
                    }

                    // ====================================================
                    // MAKER ASSIGNMENT STATUS
                    // ====================================================

                    String assignmentStatus =
                            rs.getString(
                                    "maker_assignment_status"
                            );

                    batch.setMakerAssignmentStatus(
                            assignmentStatus
                    );

                    // ====================================================
                    // LOCK STATUS
                    // ====================================================

                    if (assignmentStatus != null) {

                        if (
                                "IN_PROGRESS"
                                        .equalsIgnoreCase(
                                                assignmentStatus
                                        )
                        ) {

                            batch.setLockStatus(
                                    "IN_PROGRESS"
                            );

                        } else if (
                                "ASSIGNED"
                                        .equalsIgnoreCase(
                                                assignmentStatus
                                        )
                        ) {

                            batch.setLockStatus(
                                    "LOCKED"
                            );

                        } else if (
                                "RELEASED"
                                        .equalsIgnoreCase(
                                                assignmentStatus
                                        )
                        ) {

                            batch.setLockStatus(
                                    "AVAILABLE"
                            );

                            batch.setLockedBy(
                                    null
                            );

                            batch.setLockedAt(
                                    null
                            );

                        }

                    } else {

                        batch.setLockStatus(
                                "AVAILABLE"
                        );
                    }

                    // ====================================================
                    // RETURNED BATCH
                    // ====================================================

                    if (returnedChequeCount > 0) {

                        int returnedMakerId =
                                rs.getInt(
                                        "returned_maker_user_id"
                                );

                        if (!rs.wasNull()) {

                            batch.setMakerUserNumber(
                                    String.valueOf(
                                            returnedMakerId
                                    )
                            );
                        }

                        // =================================================
                        // Returned batch must be available to the
                        // ORIGINAL Maker for Re-Verify.
                        // =================================================

                        batch.setLockedBy(
                                null
                        );

                        batch.setLockedAt(
                                null
                        );

                        batch.setLockStatus(
                                "AVAILABLE"
                        );

                        batch.setMakerAssignmentStatus(
                                "RETURNED"
                        );
                    }

                    batches.add(
                            batch
                    );
                }
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

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new SQLException(
                    "Batch number is required."
            );
        }

        if (userId == null ||
                userId.trim().isEmpty()) {

            throw new SQLException(
                    "User ID is required."
            );
        }

        int makerId;

        try {

            makerId =
                    Integer.parseInt(
                            userId.trim()
                    );

        } catch (NumberFormatException e) {

            throw new SQLException(
                    "Invalid maker user ID: " + userId
            );
        }

        try (Connection con =
                     dataSource.getConnection()) {

            con.setAutoCommit(
                    false
            );

            try {

                // =================================================
                // 1. VERIFY LOGGED-IN USER IS AN ACTIVE MAKER
                // =================================================

                String userSql =
                        "SELECT u.user_id, " +
                        "u.status, " +
                        "u.role_id " +
                        "FROM public.\"user\" u " +
                        "WHERE u.user_id = ? " +
                        "AND UPPER(TRIM(u.status)) = 'ACTIVE' " +
                        "AND u.role_id = 3";

                try (PreparedStatement ps =
                             con.prepareStatement(
                                     userSql
                             )) {

                    ps.setInt(
                            1,
                            makerId
                    );

                    try (ResultSet rs =
                                 ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new SQLException(
                                    "User " +
                                    makerId +
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
                             con.prepareStatement(
                                     batchSql
                             )) {

                    ps.setString(
                            1,
                            batchNumber.trim()
                    );

                    try (ResultSet rs =
                                 ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new SQLException(
                                    "Batch not found: "
                                    + batchNumber
                            );
                        }

                        currentStatus =
                                rs.getString(
                                        "batch_status"
                                );
                    }
                }

                // =================================================
                // 3. CHECK EXISTING MAKER ASSIGNMENT
                // =================================================

                String assignmentSql =
                        "SELECT user_id, " +
                        "assignment_status " +
                        "FROM public.outward_batch_assignment " +
                        "WHERE batch_number = ? " +
                        "AND UPPER(assignment_role) = 'MAKER' " +
                        "AND UPPER(assignment_status) IN " +
                        "    ('ASSIGNED', 'IN_PROGRESS') " +
                        "FOR UPDATE";

                Integer existingMakerId =
                        null;

                String existingAssignmentStatus =
                        null;

                try (PreparedStatement ps =
                             con.prepareStatement(
                                     assignmentSql
                             )) {

                    ps.setString(
                            1,
                            batchNumber.trim()
                    );

                    try (ResultSet rs =
                                 ps.executeQuery()) {

                        if (rs.next()) {

                            existingMakerId =
                                    rs.getInt(
                                            "user_id"
                                    );

                            existingAssignmentStatus =
                                    rs.getString(
                                            "assignment_status"
                                    );
                        }
                    }
                }

                // =================================================
                // 4. IF ALREADY ASSIGNED, ONLY SAME MAKER CAN CONTINUE
                // =================================================

                if (existingMakerId != null) {

                    if (existingMakerId.intValue()
                            != makerId) {

                        throw new SQLException(
                                "Batch " +
                                batchNumber +
                                " is already assigned to Maker " +
                                existingMakerId +
                                "."
                        );
                    }

                    if ("IN_PROGRESS".equalsIgnoreCase(
                            existingAssignmentStatus)) {

                        con.commit();

                        return true;
                    }

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
                                     con.prepareStatement(
                                             resumeSql
                                     )) {

                            ps.setString(
                                    1,
                                    batchNumber.trim()
                            );

                            ps.setInt(
                                    2,
                                    makerId
                            );

                            ps.executeUpdate();
                        }

                        String statusSql =
                                "UPDATE public.outward_batch " +
                                "SET batch_status = 'ASSIGNED' " +
                                "WHERE batch_number = ?";

                        try (PreparedStatement ps =
                                     con.prepareStatement(
                                             statusSql
                                     )) {

                            ps.setString(
                                    1,
                                    batchNumber.trim()
                            );

                            ps.executeUpdate();
                        }

                        con.commit();

                        return true;
                    }
                }

                // =================================================
                // 5. NEW ASSIGNMENT ONLY WHEN BATCH IS CAPTURED
                // =================================================

                if (!"CAPTURED".equalsIgnoreCase(
                        currentStatus)) {

                    throw new SQLException(
                            "Batch " +
                            batchNumber +
                            " is not available for a new Maker assignment. " +
                            "Current status: " +
                            currentStatus
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
                             con.prepareStatement(
                                     insertSql
                             )) {

                    ps.setString(
                            1,
                            batchNumber.trim()
                    );

                    ps.setInt(
                            2,
                            makerId
                    );

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
                             con.prepareStatement(
                                     updateSql
                             )) {

                    ps.setString(
                            1,
                            batchNumber.trim()
                    );

                    ps.executeUpdate();
                }

                con.commit();

                return true;

            } catch (SQLException e) {

                con.rollback();

                throw e;

            } finally {

                con.setAutoCommit(
                        true
                );
            }
        }
    }

    // ============================================================
    // GET CHEQUES
    // ============================================================

    public List<OutwardCheque> getCheques(
            String batchNumber) throws SQLException {

        List<OutwardCheque> cheques =
                new ArrayList<>();

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new SQLException(
                    "Batch number is required."
            );
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

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                sql
                        )
        ) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    OutwardCheque cheque =
                            new OutwardCheque();

                    // BATCH NUMBER

                    cheque.setBatchNumber(
                            rs.getString(
                                    "batch_number"
                            )
                    );

                    // CHEQUE NUMBER

                    cheque.setChequeNumber(
                            rs.getString(
                                    "cheque_number"
                            )
                    );

                    // CITY CODE

                    cheque.setCityCode(
                            rs.getString(
                                    "city_code"
                            )
                    );

                    // BANK CODE

                    cheque.setBankCode(
                            rs.getString(
                                    "bank_code"
                            )
                    );

                    // BRANCH CODE

                    cheque.setBranchCode(
                            rs.getString(
                                    "branch_code"
                            )
                    );

                    // DRAWER ACCOUNT NUMBER

                    cheque.setDrawerAccountNumber(
                            rs.getString(
                                    "drawer_account_number"
                            )
                    );

                    // DRAWER NAME

                    cheque.setDrawerName(
                            rs.getString(
                                    "drawer_name"
                            )
                    );

                    // DEPOSITOR ACCOUNT NUMBER

                    cheque.setDepositorAccountNumber(
                            rs.getString(
                                    "payee_account_number"
                            )
                    );

                    // DEPOSITOR NAME

                    cheque.setDepositorName(
                            rs.getString(
                                    "payee_name"
                            )
                    );

                    // PAYEE NAME

                    cheque.setPayeeName(
                            rs.getString(
                                    "payee_name"
                            )
                    );

                    // AMOUNT

                    cheque.setAmount(
                            rs.getBigDecimal(
                                    "amount"
                            )
                    );

                    // AMOUNT IN WORDS

                    cheque.setAmountInWords(
                            rs.getString(
                                    "amount_in_words"
                            )
                    );

                    // CHEQUE DATE

                    Date chequeDate =
                            rs.getDate(
                                    "cheque_date"
                            );

                    if (chequeDate != null) {

                        cheque.setChequeDate(
                                chequeDate.toLocalDate()
                        );

                    } else {

                        cheque.setChequeDate(
                                null
                        );
                    }

                    // FRONT IMAGE PATH

                    cheque.setFrontImagePath(
                            rs.getString(
                                    "front_image_path"
                            )
                    );

                    // BACK IMAGE PATH

                    cheque.setBackImagePath(
                            rs.getString(
                                    "back_image_path"
                            )
                    );

                    // CHEQUE STATUS

                    cheque.setChequeStatus(
                            rs.getString(
                                    "cheque_status"
                            )
                    );

                    // CREATED BY

                    int batchCreatedBy =
                            rs.getInt(
                                    "batch_created_by"
                            );

                    if (!rs.wasNull()) {

                        cheque.setCreatedBy(
                                String.valueOf(
                                        batchCreatedBy
                                )
                        );

                    } else {

                        cheque.setCreatedBy(
                                null
                        );
                    }

                    // CREATED AT

                    Timestamp batchCreatedAt =
                            rs.getTimestamp(
                                    "batch_created_at"
                            );

                    if (batchCreatedAt != null) {

                        cheque.setCreatedAt(
                                batchCreatedAt.toLocalDateTime()
                        );

                    } else {

                        cheque.setCreatedAt(
                                null
                        );
                    }

                    // UPDATED BY

                    cheque.setUpdatedBy(
                            null
                    );

                    // UPDATED AT

                    cheque.setUpdatedAt(
                            null
                    );

                    cheques.add(
                            cheque
                    );
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

        try (
                Connection con =
                        CTSStaticData.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                sql
                        )
        ) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

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

            con.setAutoCommit(
                    false
            );

            try {

                // =================================================
                // 1. COMPLETE MAKER ASSIGNMENT
                // =================================================

                String assignmentSql =
                        "UPDATE public.outward_batch_assignment " +
                        "SET assignment_status = 'COMPLETED', " +
                        "completed_at = CURRENT_TIMESTAMP " +
                        "WHERE batch_number = ? " +
                        "AND UPPER(assignment_role) = 'MAKER' " +
                        "AND UPPER(assignment_status) = 'IN_PROGRESS'";

                try (PreparedStatement ps =
                             con.prepareStatement(
                                     assignmentSql
                             )) {

                    ps.setString(
                            1,
                            batchNumber.trim()
                    );

                    ps.executeUpdate();
                }

                // =================================================
                // 2. COMPLETE BATCH
                // =================================================

                String batchSql =
                        "UPDATE public.outward_batch " +
                        "SET batch_status = 'COMPLETED' " +
                        "WHERE batch_number = ?";

                try (PreparedStatement ps =
                             con.prepareStatement(
                                     batchSql
                             )) {

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

                con.setAutoCommit(
                        true
                );
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
                     dataSource.getConnection()) {

            con.setAutoCommit(
                    false
            );

            try (PreparedStatement ps =
                         con.prepareStatement(
                                 sql
                         )) {

                ps.setString(
                        1,
                        status
                );

                ps.setString(
                        2,
                        batchNumber
                );

                ps.setString(
                        3,
                        chequeNumber
                );

                boolean updated =
                        ps.executeUpdate() > 0;

                if (updated) {

                    con.commit();

                } else {

                    con.rollback();
                }

                return updated;

            } catch (SQLException e) {

                con.rollback();

                throw e;
            }
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
                     dataSource.getConnection()) {

            con.setAutoCommit(
                    false
            );

            try (PreparedStatement ps =
                         con.prepareStatement(
                                 sql
                         )) {

                ps.setString(
                        1,
                        status
                );

                ps.setString(
                        2,
                        batchNumber
                );

                boolean updated =
                        ps.executeUpdate() > 0;

                if (updated) {

                    con.commit();

                } else {

                    con.rollback();
                }

                return updated;

            } catch (SQLException e) {

                con.rollback();

                throw e;
            }
        }
    }

    // ============================================================
    // RELEASE MAKER LOCK
    // ============================================================

    public boolean releaseBatchLock(
            String batchNumber,
            String userId) throws SQLException {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new SQLException(
                    "Batch number is required."
            );
        }

        if (userId == null ||
                userId.trim().isEmpty()) {

            throw new SQLException(
                    "User ID is required."
            );
        }

        int makerId;

        try {

            makerId =
                    Integer.parseInt(
                            userId.trim()
                    );

        } catch (NumberFormatException e) {

            throw new SQLException(
                    "Invalid maker user ID: " + userId
            );
        }

        String sql =
                "UPDATE public.outward_batch_assignment " +
                "SET assignment_status = 'RELEASED' " +
                "WHERE batch_number = ? " +
                "AND user_id = ? " +
                "AND UPPER(assignment_role) = 'MAKER' " +
                "AND UPPER(assignment_status) IN " +
                "    ('ASSIGNED', 'IN_PROGRESS')";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                sql
                        )
        ) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            ps.setInt(
                    2,
                    makerId
            );

            return ps.executeUpdate() > 0;
        }
    }

    // ============================================================
    // GET CHEQUE PROCESSING
    // ============================================================

    public ChequeProcessing getChequeProcessing(
            String batchNumber,
            String chequeNumber) throws SQLException {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new SQLException(
                    "Batch number is required."
            );
        }

        if (chequeNumber == null ||
                chequeNumber.trim().isEmpty()) {

            throw new SQLException(
                    "Cheque number is required."
            );
        }

        String sql =
                "SELECT " +
                "    cp.batch_number, " +
                "    cp.cheque_number, " +
                "    cp.maker_id, " +
                "    cp.maker_action, " +
                "    cp.maker_reason_code, " +
                "    cp.checker_id, " +
                "    cp.checker_action, " +
                "    cp.checker_reason_code " +
                "FROM public.cheque_processing cp " +
                "WHERE cp.batch_number = ? " +
                "AND cp.cheque_number = ?";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                sql
                        )
        ) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            ps.setString(
                    2,
                    chequeNumber.trim()
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                if (!rs.next()) {

                    return null;
                }

                ChequeProcessing processing =
                        new ChequeProcessing();

                // =================================================
                // BATCH NUMBER
                // =================================================

                processing.setBatchNumber(
                        rs.getString(
                                "batch_number"
                        )
                );

                // =================================================
                // CHEQUE NUMBER
                // =================================================

                processing.setChequeNumber(
                        rs.getString(
                                "cheque_number"
                        )
                );

                // =================================================
                // MAKER ID
                // =================================================

                int makerId =
                        rs.getInt(
                                "maker_id"
                        );

                if (!rs.wasNull()) {

                    processing.setMakerId(
                            makerId
                    );
                }

                // =================================================
                // MAKER ACTION
                // =================================================

                processing.setMakerAction(
                        rs.getString(
                                "maker_action"
                        )
                );

                // =================================================
                // MAKER REASON CODE
                // =================================================

                processing.setMakerReasonCode(
                        rs.getString(
                                "maker_reason_code"
                        )
                );

                // =================================================
                // CHECKER ID
                // =================================================

                int checkerId =
                        rs.getInt(
                                "checker_id"
                        );

                if (!rs.wasNull()) {

                    processing.setCheckerId(
                            checkerId
                    );
                }

                // =================================================
                // CHECKER ACTION
                // =================================================

                processing.setCheckerAction(
                        rs.getString(
                                "checker_action"
                        )
                );

                // =================================================
                // CHECKER REASON CODE
                // =================================================

                processing.setCheckerReasonCode(
                        rs.getString(
                                "checker_reason_code"
                        )
                );

                return processing;
            }
        }
    }
}