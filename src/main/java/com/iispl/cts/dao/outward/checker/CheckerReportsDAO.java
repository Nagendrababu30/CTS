package com.iispl.cts.dao.outward.checker;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.data.CTSStaticData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CheckerReportsDAO {

    /**
     * Get only ASSIGNED batches.
     *
     * Temporary status for Reports testing.
     */
    public List<OutwardBatch> getCheckerCompletedBatches() {

        List<OutwardBatch> batches = new ArrayList<>();

        String sql =
                "SELECT " +
                "batch_number, " +
                "cheque_count, " +
                "batch_status " +
                "FROM public.outward_batch " +
                "WHERE batch_status = ? " +
                "ORDER BY batch_number DESC";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(
                    1,
                    "ASSIGNED"
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

                    // Batch Number
                    batch.setBatchNumber(
                            rs.getString(
                                    "batch_number"
                            )
                    );

                    // Total Cheques
                    batch.setNumberOfCheques(
                            rs.getInt(
                                    "cheque_count"
                            )
                    );

                    // Status
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
        }

        return batches;
    }
}