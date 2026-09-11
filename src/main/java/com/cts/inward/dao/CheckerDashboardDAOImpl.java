package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.cts.inward.model.CheckerBatch;

public class CheckerDashboardDAOImpl implements CheckerDashboardDAO {

    private final DataSource dataSource;

    public CheckerDashboardDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /*
     * Total batches with latest status = SENT_TO_CHECKER
     */
    @Override
    public int getReceivedBatchCount() {

        String sql =
                "SELECT COUNT(*) "
                + "FROM ( "
                + "    SELECT DISTINCT ON (batch_id) batch_id, batch_status "
                + "    FROM inward_batch_history "
                + "    ORDER BY batch_id, changed_on DESC, batch_history_id DESC "
                + ") hs "
                + "WHERE hs.batch_status = 'SENT_TO_CHECKER'";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get received batch count", e);
        }
    }

    /*
     * Batches available for any checker to pick up.
     * lock_status = AVAILABLE or UNLOCKED
     */
    @Override
    public int getAvailableBatchCount() {

        String sql =
                "SELECT COUNT(DISTINCT l.batch_id) "
                + "FROM inward_batch_lock l "
                + "JOIN ( "
                + "    SELECT DISTINCT ON (batch_id) batch_id, batch_status "
                + "    FROM inward_batch_history "
                + "    ORDER BY batch_id, changed_on DESC, batch_history_id DESC "
                + ") hs ON hs.batch_id = l.batch_id "
                + "WHERE l.lock_status IN ('AVAILABLE', 'UNLOCKED') "
                + "AND hs.batch_status = 'SENT_TO_CHECKER'";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get available batch count", e);
        }
    }

    /*
     * Batches currently locked by this checker.
     */
    @Override
    public int getMyBatchCount(long userId) {

        String sql =
                "SELECT COUNT(*) "
                + "FROM inward_batch_lock "
                + "WHERE user_id = ? "
                + "AND lock_status = 'LOCKED'";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get my batch count", e);
        }
    }

    /*
     * All batches whose latest status = SENT_TO_CHECKER
     * AND whose latest lock status = AVAILABLE, UNLOCKED, or LOCKED.
     */
    @Override
    public List<CheckerBatch> getBatches() {

        String sql =
                "SELECT "
                + "    b.batch_id, "
                + "    b.total_cheques, "
                + "    l.user_id AS checker_id, "
                + "    l.lock_status, "
                + "    h.maker_id "
                + "FROM inward_batch b "

                // Latest lock row per batch by locked_time
                + "LEFT JOIN ( "
                + "    SELECT DISTINCT ON (batch_id) "
                + "        batch_id, user_id, lock_status, locked_time "
                + "    FROM inward_batch_lock "
                + "    ORDER BY batch_id, locked_time DESC "
                + ") l ON l.batch_id = b.batch_id "

                // Maker who sent to checker
                + "LEFT JOIN ( "
                + "    SELECT bh.batch_id, MAX(bh.changed_by) AS maker_id "
                + "    FROM inward_batch_history bh "
                + "    JOIN public.user u ON u.user_id = bh.changed_by AND u.role_id = 1 "
                + "    WHERE bh.batch_status = 'SENT_TO_CHECKER' "
                + "    GROUP BY bh.batch_id "
                + ") h ON h.batch_id = b.batch_id "

                // Latest batch status
                + "JOIN ( "
                + "    SELECT DISTINCT ON (batch_id) batch_id, batch_status "
                + "    FROM inward_batch_history "
                + "    ORDER BY batch_id, changed_on DESC, batch_history_id DESC "
                + ") hs ON hs.batch_id = b.batch_id "

                // Only SENT_TO_CHECKER batches
                + "WHERE hs.batch_status = 'SENT_TO_CHECKER' "

                + "ORDER BY b.batch_id";

        List<CheckerBatch> batches = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                CheckerBatch batch = new CheckerBatch();

                batch.setBatchId(resultSet.getLong("batch_id"));
                batch.setTotalCheques(resultSet.getInt("total_cheques"));

                Object maker = resultSet.getObject("maker_id");
                batch.setMaker(maker != null ? String.valueOf(maker) : "Not Assigned");

                Object checkerId = resultSet.getObject("checker_id");
                batch.setUserId(checkerId != null ? ((Number) checkerId).longValue() : null);

                String lockStatus = resultSet.getString("lock_status");
                batch.setLockStatus(lockStatus != null ? lockStatus : "UNLOCKED");

                batches.add(batch);
            }

            return batches;

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to retrieve checker batches", e);
        }
    }

    /*
     * Lock a batch for this checker by inserting a new LOCKED row.
     * The previous UNLOCKED row stays for audit trail.
     */
    @Override
    public boolean lockBatch(long batchId, long userId) {

        String sql =
                "INSERT INTO inward_batch_lock "
                + "(batch_id, user_id, locked_time, lock_status) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, 'LOCKED')";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, batchId);
            statement.setLong(2, userId);

            int rowsInserted = statement.executeUpdate();
            return rowsInserted == 1;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to lock batch: " + batchId, e);
        }
    }
}
