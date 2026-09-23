package com.iispl.cts.dao.outward.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.OutwardBatch;

public class CheckerDashboardDAO {

    private final javax.sql.DataSource dataSource =
            ConnectionPool.getDataSource();

    /*
     * ============================================================
     * GET CHECKER BATCHES
     * ============================================================
     *
     * Normal Checker batches are loaded here.
     *
     * Additionally, a batch containing RE_VERIFIED cheques is
     * returned only for the Checker who originally performed
     * the SEND_BACK action.
     *
     * Re-Verify eligibility is based ONLY on:
     *
     * 1. cheque_processing.checker_id
     * 2. cheque_processing.checker_action = SEND_BACK
     * 3. outward_cheque.cheque_status = RE_VERIFIED
     *
     * outward_batch.batch_status is NOT used for Re-Verify
     * eligibility.
     * ============================================================
     */

    public List<OutwardBatch> getCheckerBatches(
            String checkerUserId) {

        List<OutwardBatch> batches =
                new ArrayList<>();

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return batches;
        }

        String sql =
                "SELECT " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +
                "    cba.user_id AS checker_user_id, " +
                "    cba.assigned_at AS checker_assigned_at, " +
                "    cba.started_at AS checker_started_at, " +
                "    cba.completed_at AS checker_completed_at, " +
                "    cba.assignment_status AS checker_assignment_status " +

                "FROM public.outward_batch ob " +

                "LEFT JOIN public.outward_batch_assignment cba " +
                "    ON ob.batch_number = cba.batch_number " +
                "    AND UPPER(cba.assignment_role) = 'CHECKER' " +
                "    AND UPPER(cba.assignment_status) IN " +
                "        ('ASSIGNED', 'IN_PROGRESS') " +

                "WHERE " +
                "    UPPER(ob.batch_status) IN " +
                "        ('SUBMITTED_TO_CHECKER', " +
                "         'READY_FOR_CHECKER', " +
                "         'SUBMITTED', " +
                "         'CHECKER_PENDING', " +
                "         'PENDING_CHECKER', " +
                "         'CHECKER_PROCESSING') " +

                "    OR EXISTS ( " +
                "        SELECT 1 " +
                "        FROM public.cheque_processing cp " +

                "        INNER JOIN public.outward_cheque oc " +
                "            ON oc.batch_number = cp.batch_number " +
                "            AND oc.cheque_number = cp.cheque_number " +

                "        WHERE cp.batch_number = ob.batch_number " +
                "          AND cp.checker_id = ? " +
                "          AND UPPER(TRIM(cp.checker_action)) = " +
                "              'SEND_BACK' " +
                "          AND UPPER(TRIM(oc.cheque_status)) = " +
                "              'RE_VERIFIED' " +

                "    ) " +

                "ORDER BY ob.created_at DESC";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    Integer.parseInt(
                            checkerUserId
                    )
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

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

                    batch.setNumberOfCheques(
                            rs.getInt(
                                    "cheque_count"
                            )
                    );

                    batch.setBatchFolderPath(
                            rs.getString(
                                    "batch_folder_path"
                            )
                    );

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

                    if (rs.getTimestamp(
                            "created_at") != null) {

                        batch.setCreatedAt(
                                rs.getTimestamp(
                                        "created_at"
                                ).toLocalDateTime()
                        );
                    }

                    batch.setBatchStatus(
                            rs.getString(
                                    "batch_status"
                            )
                    );

                    int checkerUserId1 =
                            rs.getInt(
                                    "checker_user_id"
                            );

                    if (!rs.wasNull()) {

                        batch.setCheckerUserNumber(
                                String.valueOf(
                                        checkerUserId1
                                )
                        );

                        batch.setLockedBy(
                                String.valueOf(
                                        checkerUserId1
                                )
                        );

                        if (rs.getTimestamp(
                                "checker_assigned_at") != null) {

                            batch.setLockedAt(
                                    rs.getTimestamp(
                                            "checker_assigned_at"
                                    ).toLocalDateTime()
                            );
                        }

                        if (rs.getTimestamp(
                                "checker_started_at") != null) {

                            batch.setCheckerStartedAt(
                                    rs.getTimestamp(
                                            "checker_started_at"
                                    ).toLocalDateTime()
                            );
                        }

                        if (rs.getTimestamp(
                                "checker_completed_at") != null) {

                            batch.setCheckerCompletedAt(
                                    rs.getTimestamp(
                                            "checker_completed_at"
                                    ).toLocalDateTime()
                            );
                        }

                        String assignmentStatus =
                                rs.getString(
                                        "checker_assignment_status"
                                );

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

                        } else {

                            batch.setLockStatus(
                                    assignmentStatus
                            );
                        }

                    } else {

                        batch.setCheckerUserNumber(
                                null
                        );

                        batch.setLockedBy(
                                null
                        );

                        batch.setLockedAt(
                                null
                        );

                        batch.setCheckerStartedAt(
                                null
                        );

                        batch.setCheckerCompletedAt(
                                null
                        );

                        batch.setLockStatus(
                                "AVAILABLE"
                        );
                    }

                    batches.add(batch);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to load checker batches from database.",
                    e
            );
        }

        return batches;
    }

    /*
     * ============================================================
     * GET RE-VERIFY BATCHES
     * ============================================================
     */

    public List<OutwardBatch> getReVerifyBatches(
            String checkerUserId) {

        List<OutwardBatch> batches =
                new ArrayList<>();

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return batches;
        }

        String sql =
                "SELECT " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +
                "    cp.checker_id AS original_checker_id, " +
                "    COUNT(DISTINCT cp.cheque_number) " +
                "        AS reverify_cheque_count " +

                "FROM public.outward_batch ob " +

                "INNER JOIN public.cheque_processing cp " +
                "    ON ob.batch_number = cp.batch_number " +

                "INNER JOIN public.outward_cheque oc " +
                "    ON oc.batch_number = cp.batch_number " +
                "    AND oc.cheque_number = cp.cheque_number " +

                "WHERE cp.checker_id = ? " +

                "  AND UPPER(TRIM(cp.checker_action)) = " +
                "      'SEND_BACK' " +

                "  AND UPPER(TRIM(oc.cheque_status)) = " +
                "      'RE_VERIFIED' " +

                "GROUP BY " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +
                "    cp.checker_id " +

                "ORDER BY ob.created_at DESC";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    Integer.parseInt(
                            checkerUserId
                    )
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

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

                    batch.setBatchFolderPath(
                            rs.getString(
                                    "batch_folder_path"
                            )
                    );

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

                    if (rs.getTimestamp(
                            "created_at") != null) {

                        batch.setCreatedAt(
                                rs.getTimestamp(
                                        "created_at"
                                ).toLocalDateTime()
                        );
                    }

                    batch.setNumberOfCheques(
                            rs.getInt(
                                    "reverify_cheque_count"
                            )
                    );

                    String originalChecker =
                            rs.getString(
                                    "original_checker_id"
                            );

                    batch.setCheckerUserNumber(
                            originalChecker
                    );

                    batch.setLockedBy(
                            originalChecker
                    );

                    batch.setLockStatus(
                            "RE_VERIFY"
                    );

                    batch.setBatchStatus(
                            rs.getString(
                                    "batch_status"
                            )
                    );

                    batches.add(batch);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to load Re-Verify batches from database.",
                    e
            );
        }

        return batches;
    }

    /*
     * ============================================================
     * HAS RE-VERIFIED CHEQUES
     * ============================================================
     */

    public boolean hasReVerifiedCheques(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return false;
        }

        String sql =
                "SELECT EXISTS ( " +

                "    SELECT 1 " +

                "    FROM public.cheque_processing cp " +

                "    INNER JOIN public.outward_cheque oc " +
                "        ON oc.batch_number = cp.batch_number " +
                "        AND oc.cheque_number = cp.cheque_number " +

                "    WHERE cp.batch_number = ? " +

                "      AND cp.checker_id = ? " +

                "      AND UPPER(TRIM(cp.checker_action)) = " +
                "          'SEND_BACK' " +

                "      AND UPPER(TRIM(oc.cheque_status)) = " +
                "          'RE_VERIFIED' " +

                ")";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber
            );

            ps.setInt(
                    2,
                    Integer.parseInt(
                            checkerUserId
                    )
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    return rs.getBoolean(
                            1
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to check Re-Verify cheques.",
                    e
            );
        }

        return false;
    }

    /*
     * ============================================================
     * GET RE-VERIFIED CHEQUE COUNT
     * ============================================================
     */

    public int getReVerifiedChequeCount(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return 0;
        }

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return 0;
        }

        String sql =
                "SELECT COUNT(DISTINCT oc.cheque_number) " +

                "FROM public.outward_cheque oc " +

                "INNER JOIN public.cheque_processing cp " +
                "    ON oc.batch_number = cp.batch_number " +
                "    AND oc.cheque_number = cp.cheque_number " +

                "WHERE oc.batch_number = ? " +

                "  AND UPPER(TRIM(oc.cheque_status)) = " +
                "      'RE_VERIFIED' " +

                "  AND cp.checker_id = ? " +

                "  AND UPPER(TRIM(cp.checker_action)) = " +
                "      'SEND_BACK'";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber
            );

            ps.setInt(
                    2,
                    Integer.parseInt(
                            checkerUserId
                    )
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to get Re-Verify cheque count.",
                    e
            );
        }

        return 0;
    }

    /*
     * ============================================================
     * GET RE-VERIFIED CHEQUE NUMBERS
     * ============================================================
     *
     * Returns the exact cheque numbers that:
     *
     * 1. Belong to this batch
     * 2. Were SEND_BACK by this Checker
     * 3. Are now RE_VERIFIED
     *
     * Used by Dashboard to open the exact corrected cheque.
     * ============================================================
     */

    public List<String> getReVerifiedChequeNumbers(
            String batchNumber,
            String checkerUserId) {

        List<String> chequeNumbers =
                new ArrayList<>();

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return chequeNumbers;
        }

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return chequeNumbers;
        }

        String sql =
                "SELECT oc.cheque_number " +

                "FROM public.outward_cheque oc " +

                "INNER JOIN public.cheque_processing cp " +
                "    ON oc.batch_number = cp.batch_number " +
                "    AND oc.cheque_number = cp.cheque_number " +

                "WHERE oc.batch_number = ? " +

                "  AND cp.checker_id = ? " +

                "  AND UPPER(TRIM(cp.checker_action)) = " +
                "      'SEND_BACK' " +

                "  AND UPPER(TRIM(oc.cheque_status)) = " +
                "      'RE_VERIFIED' " +

                "ORDER BY oc.cheque_number";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            ps.setInt(
                    2,
                    Integer.parseInt(
                            checkerUserId.trim()
                    )
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    chequeNumbers.add(
                            rs.getString(
                                    "cheque_number"
                            )
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to get Re-Verify cheque numbers.",
                    e
            );
        }

        return chequeNumbers;
    }

    /*
     * ============================================================
     * HAS PENDING MAKER CHEQUES
     * ============================================================
     */

    public boolean hasPendingMakerCheques(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return false;
        }

        String sql =
                "SELECT EXISTS ( " +

                "    SELECT 1 " +

                "    FROM public.cheque_processing cp " +

                "    INNER JOIN public.outward_cheque oc " +
                "        ON oc.batch_number = cp.batch_number " +
                "        AND oc.cheque_number = cp.cheque_number " +

                "    WHERE cp.batch_number = ? " +

                "      AND cp.checker_id = ? " +

                "      AND UPPER(TRIM(cp.checker_action)) = " +
                "          'SEND_BACK' " +

                "      AND ( " +
                "          oc.cheque_status IS NULL " +
                "          OR UPPER(TRIM(oc.cheque_status)) <> " +
                "              'RE_VERIFIED' " +
                "      ) " +

                ")";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber
            );

            ps.setInt(
                    2,
                    Integer.parseInt(
                            checkerUserId
                    )
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    return rs.getBoolean(
                            1
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to check pending Maker cheques.",
                    e
            );
        }

        return false;
    }

    /*
     * ============================================================
     * FIND ONE CHECKER BATCH
     * ============================================================
     */

    public OutwardBatch findBatch(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return null;
        }

        String sql =
                "SELECT " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +
                "    cba.user_id AS checker_user_id, " +
                "    cba.assigned_at AS checker_assigned_at, " +
                "    cba.started_at AS checker_started_at, " +
                "    cba.completed_at AS checker_completed_at, " +
                "    cba.assignment_status AS checker_assignment_status " +

                "FROM public.outward_batch ob " +

                "LEFT JOIN public.outward_batch_assignment cba " +
                "    ON ob.batch_number = cba.batch_number " +
                "    AND UPPER(cba.assignment_role) = 'CHECKER' " +
                "    AND UPPER(cba.assignment_status) IN " +
                "        ('ASSIGNED', 'IN_PROGRESS') " +

                "WHERE ob.batch_number = ?";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (!rs.next()) {

                    return null;
                }

                OutwardBatch batch =
                        new OutwardBatch();

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

                batch.setNumberOfCheques(
                        rs.getInt(
                                "cheque_count"
                        )
                );

                batch.setBatchFolderPath(
                        rs.getString(
                                "batch_folder_path"
                        )
                );

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

                if (rs.getTimestamp(
                        "created_at") != null) {

                    batch.setCreatedAt(
                            rs.getTimestamp(
                                    "created_at"
                            ).toLocalDateTime()
                    );
                }

                batch.setBatchStatus(
                        rs.getString(
                                "batch_status"
                        )
                );

                int checkerUserId =
                        rs.getInt(
                                "checker_user_id"
                        );

                if (!rs.wasNull()) {

                    batch.setCheckerUserNumber(
                            String.valueOf(
                                    checkerUserId
                            )
                    );

                    batch.setLockedBy(
                            String.valueOf(
                                    checkerUserId
                            )
                    );

                    if (rs.getTimestamp(
                            "checker_assigned_at") != null) {

                        batch.setLockedAt(
                                rs.getTimestamp(
                                        "checker_assigned_at"
                                ).toLocalDateTime()
                        );
                    }

                    if (rs.getTimestamp(
                            "checker_started_at") != null) {

                        batch.setCheckerStartedAt(
                                rs.getTimestamp(
                                        "checker_started_at"
                                ).toLocalDateTime()
                        );
                    }

                    if (rs.getTimestamp(
                            "checker_completed_at") != null) {

                        batch.setCheckerCompletedAt(
                                rs.getTimestamp(
                                        "checker_completed_at"
                                ).toLocalDateTime()
                        );
                    }

                    String status =
                            rs.getString(
                                    "checker_assignment_status"
                            );

                    if ("IN_PROGRESS".equalsIgnoreCase(
                            status)) {

                        batch.setLockStatus(
                                "IN_PROGRESS"
                        );

                    } else {

                        batch.setLockStatus(
                                "LOCKED"
                        );
                    }

                } else {

                    batch.setCheckerUserNumber(
                            null
                    );

                    batch.setLockedBy(
                            null
                    );

                    batch.setLockStatus(
                            "AVAILABLE"
                    );
                }

                return batch;
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to find checker batch.",
                    e
            );
        }
    }
    
    

    /*
     * ============================================================
     * ASSIGN / LOCK CHECKER BATCH
     * ============================================================
     *
     * ONLY for normal batches.
     *
     * Re-Verify flow does NOT call this method.
     * ============================================================
     */

    public boolean assignBatch(
            String batchNumber,
            long checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        String sql =
                "INSERT INTO public.outward_batch_assignment " +
                "    (batch_number, user_id, assignment_role, " +
                "     assigned_at, started_at, assignment_status) " +

                "SELECT " +
                "    ?, " +
                "    ?, " +
                "    'CHECKER', " +
                "    CURRENT_TIMESTAMP, " +
                "    CURRENT_TIMESTAMP, " +
                "    'IN_PROGRESS' " +

                "WHERE NOT EXISTS ( " +

                "    SELECT 1 " +

                "    FROM public.outward_batch_assignment " +

                "    WHERE batch_number = ? " +

                "      AND UPPER(assignment_role) = 'CHECKER' " +

                "      AND UPPER(assignment_status) " +
                "          IN ('ASSIGNED', 'IN_PROGRESS') " +

                ")";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber
            );

            ps.setInt(
                    2,
                    (int) checkerUserId
            );

            ps.setString(
                    3,
                    batchNumber
            );

            int inserted =
                    ps.executeUpdate();

            return inserted == 1;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to assign checker batch.",
                    e
            );
        }
    }
}