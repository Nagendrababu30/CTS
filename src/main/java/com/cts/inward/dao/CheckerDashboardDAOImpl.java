 package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.cts.inward.model.CheckerBatch;

public class CheckerDashboardDAOImpl
        implements CheckerDashboardDAO {

    private final DataSource dataSource;


    public CheckerDashboardDAOImpl(
            DataSource dataSource) {

        this.dataSource = dataSource;
    }


    /*
     * Total batches received by Checker.
     */
    @Override
    public int getReceivedBatchCount() {

        String sql =
                "SELECT COUNT(*) "
                + "FROM ( "
                + "    SELECT DISTINCT ON (batch_id) "
                + "        batch_id, batch_status "
                + "    FROM inward_batch_history "
                + "    ORDER BY "
                + "        batch_id, "
                + "        changed_on DESC, "
                + "        batch_history_id DESC "
                + ") hs "
                + "WHERE hs.batch_status IN "
                + "('SENT_TO_CHECKER', "
                + " 'RETURN_TO_MAKER', "
                + " 'ON_HOLD')";


        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to get received batch count",
                    e);
        }
    }


    /*
     * Available batches for Checker.
     */
    @Override
    public int getAvailableBatchCount() {

        String sql =
                "SELECT COUNT(DISTINCT l.batch_id) "
                + "FROM inward_batch_lock l "
                + "JOIN ( "
                + "    SELECT DISTINCT ON (batch_id) "
                + "        batch_id, batch_status "
                + "    FROM inward_batch_history "
                + "    ORDER BY "
                + "        batch_id, "
                + "        changed_on DESC, "
                + "        batch_history_id DESC "
                + ") hs "
                + "ON hs.batch_id = l.batch_id "
                + "WHERE l.lock_status IN "
                + "('AVAILABLE', 'UNLOCKED') "
                + "AND hs.batch_status = 'SENT_TO_CHECKER'";


        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to get available batch count",
                    e);
        }
    }


    /*
     * Batches locked by current Checker.
     */
    @Override
    public int getMyBatchCount(long userId) {

        String sql =
                "SELECT COUNT(*) "
                + "FROM inward_batch_lock "
                + "WHERE user_id = ? "
                + "AND lock_status = 'LOCKED'";


        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);

            try (
                    ResultSet rs =
                            statement.executeQuery()
            ) {

                if (rs.next()) {
                    return rs.getInt(1);
                }

                return 0;
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to get my batch count",
                    e);
        }
    }


    /*
     * Get all batches required by Checker dashboard.
     */
    @Override
    public List<CheckerBatch> getBatches() {

        String sql =
                "SELECT "
                + "    b.batch_id, "
                + "    b.total_cheques, "

                // Latest checker ID
                + "    l_chk.user_id AS checker_id, "

                // Latest checker username
                + "    l_chk.username AS checker_name, "

                // Latest lock status
                + "    l.lock_status, "

                // Maker ID
                + "    h.maker_id, "

                // Maker username
                + "    h.maker_name, "

                // Latest batch status
                + "    hs.batch_status, "

                // Re-verify flag
                + "    ( "
                + "        hs.batch_status IN "
                + "        ('RETURN_TO_MAKER', 'ON_HOLD') "
                + "        OR EXISTS ( "
                + "            SELECT 1 "
                + "            FROM inward_cheque c "
                + "            JOIN inward_cheque_status_history sh "
                + "              ON sh.cheque_number = c.cheque_number "
                + "            WHERE c.batch_id = b.batch_id "
                + "            AND ( "
                + "                sh.status = 'RETURN_TO_MAKER' "
                + "                OR sh.checker_action = 'Sent Back' "
                + "                OR ( "
                + "                    sh.return_reason_code IS NOT NULL "
                + "                    AND ( "
                + "                        sh.return_reason_code LIKE 'CR-%' "
                + "                        OR sh.return_reason_code = 'OTHER' "
                + "                    ) "
                + "                ) "
                + "            ) "
                + "        ) "
                + "    ) AS is_reverify "

                + "FROM inward_batch b "

                /*
                 * Latest lock record.
                 */
                + "LEFT JOIN ( "
                + "    SELECT DISTINCT ON (batch_id) "
                + "        batch_id, "
                + "        user_id, "
                + "        lock_status, "
                + "        locked_time, "
                + "        lock_id "
                + "    FROM inward_batch_lock "
                + "    ORDER BY "
                + "        batch_id, "
                + "        CASE WHEN lock_status = 'LOCKED' THEN 1 ELSE 2 END, "
                + "        locked_time DESC, "
                + "        lock_id DESC "
                + ") l "
                + "ON l.batch_id = b.batch_id "

                /*
                 * Latest Checker.
                 */
                + "LEFT JOIN ( "
                + "    SELECT DISTINCT ON (bl.batch_id) "
                + "        bl.batch_id, "
                + "        bl.user_id, "
                + "        u.username "
                + "    FROM inward_batch_lock bl "
                + "    JOIN public.\"user\" u "
                + "      ON u.user_id = bl.user_id "
                + "     AND u.role_id = 2 "
                + "    ORDER BY "
                + "        bl.batch_id, "
                + "        bl.locked_time DESC, "
                + "        bl.lock_id DESC "
                + ") l_chk "
                + "ON l_chk.batch_id = b.batch_id "

                /*
                 * Maker who sent the batch to Checker.
                 */
                + "LEFT JOIN ( "
                + "    SELECT "
                + "        bh.batch_id, "
                + "        MAX(bh.changed_by) AS maker_id, "
                + "        MAX(u.username) AS maker_name "
                + "    FROM inward_batch_history bh "
                + "    JOIN public.\"user\" u "
                + "      ON u.user_id = bh.changed_by "
                + "     AND u.role_id = 1 "
                + "    WHERE bh.batch_status IN "
                + "        ('SENT_TO_CHECKER', 'RETURN_TO_MAKER') "
                + "    GROUP BY bh.batch_id "
                + ") h "
                + "ON h.batch_id = b.batch_id "

                /*
                 * Latest batch status.
                 */
                + "JOIN ( "
                + "    SELECT DISTINCT ON (batch_id) "
                + "        batch_id, "
                + "        batch_status "
                + "    FROM inward_batch_history "
                + "    ORDER BY "
                + "        batch_id, "
                + "        changed_on DESC, "
                + "        batch_history_id DESC "
                + ") hs "
                + "ON hs.batch_id = b.batch_id "

                /*
                 * Checker should see these batches.
                 */
                + "WHERE hs.batch_status IN "
                + "('SENT_TO_CHECKER', "
                + " 'RETURN_TO_MAKER', "
                + " 'ON_HOLD') "

                + "ORDER BY b.batch_id";


        List<CheckerBatch> batches =
                new ArrayList<>();


        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        statement.executeQuery()
        ) {

            while (rs.next()) {

                CheckerBatch batch =
                        new CheckerBatch();


                batch.setBatchId(
                        rs.getLong("batch_id"));


                batch.setTotalCheques(
                        rs.getInt("total_cheques"));


                /*
                 * Maker name.
                 */
                String makerName =
                        rs.getString("maker_name");

                if (makerName == null
                        || makerName.trim().isEmpty()) {

                    makerName =
                            "Not Assigned";
                }

                batch.setMaker(makerName);


                /*
                 * Checker ID.
                 *
                 * This is NOT displayed.
                 * It is used internally for ownership.
                 */
                Object checkerId =
                        rs.getObject("checker_id");

                if (checkerId != null) {

                    batch.setUserId(
                            ((Number) checkerId)
                                    .longValue());

                } else {

                    batch.setUserId(null);
                }


                /*
                 * Checker name.
                 */
                String checkerName =
                        rs.getString("checker_name");

                if (checkerName == null
                        || checkerName.trim().isEmpty()) {

                    checkerName =
                            "Not Assigned";
                }

                batch.setCheckerName(
                        checkerName);


                /*
                 * Lock status.
                 */
                String lockStatus =
                        rs.getString("lock_status");

                if (lockStatus == null
                        || lockStatus.trim().isEmpty()) {

                    lockStatus =
                            "UNLOCKED";
                }

                batch.setLockStatus(
                        lockStatus);


                /*
                 * Batch status.
                 */
                batch.setBatchStatus(
                        rs.getString("batch_status"));


                /*
                 * Re-verify flag.
                 */
                batch.setReVerify(
                        rs.getBoolean("is_reverify"));


                batches.add(batch);
            }


            return batches;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to retrieve checker batches",
                    e);
        }
    }


    /*
     * Lock batch for current Checker.
     */
    @Override
    public boolean lockBatch(
            long batchId,
            long userId) {

        String sql =
                "INSERT INTO inward_batch_lock "
                + "(batch_id, user_id, locked_time, lock_status) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, 'LOCKED')";


        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    batchId);

            statement.setLong(
                    2,
                    userId);

            int rowsInserted =
                    statement.executeUpdate();

            return rowsInserted == 1;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to lock batch: "
                    + batchId,
                    e);
        }
    }
}