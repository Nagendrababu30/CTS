package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerDataEntryDAO {

	private final javax.sql.DataSource dataSource = ConnectionPool.getDataSource();

	// GET ALL BATCHES

	public List<OutwardBatch> getAllBatches() {

		List<OutwardBatch> batches = new ArrayList<>();

		String sql = "SELECT batch_number, " + "       cheque_count, " + "       batch_status "
				+ "FROM public.outward_batch " + "ORDER BY batch_number ASC";

		try (Connection con = dataSource.getConnection();

				PreparedStatement ps = con.prepareStatement(sql);

				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				OutwardBatch batch = new OutwardBatch();

				batch.setBatchNumber(rs.getString("batch_number"));

				batch.setNumberOfCheques(rs.getInt("cheque_count"));

				batch.setBatchStatus(rs.getString("batch_status"));

				batches.add(batch);
			}

		} catch (SQLException e) {

			e.printStackTrace();
		}

		return batches;
	}

	// GET BATCHES FOR MAKER

	public List<OutwardBatch> getBatchesForMaker(long userId) {

		List<OutwardBatch> batches = new ArrayList<>();

		String sql = "SELECT " 
		        + "    b.batch_number, " 
		        + "    b.branch_code, " 
		        + "    b.cheque_count, "
		        + "    b.batch_folder_path, " 
		        + "    b.batch_status, " 
		        + "    MAX(ba.assignment_status) AS assignment_status, "
		        + "    COALESCE(rc.returned_cheque_count, 0) AS returned_cheque_count "
		        + "FROM public.outward_batch b "
		        + "INNER JOIN public.outward_batch_assignment ba "
		        + "    ON b.batch_number = ba.batch_number "
		        + "   AND ba.user_id = ? "
		        + "   AND UPPER(TRIM(ba.assignment_role)) = 'MAKER' "
		        + "LEFT JOIN ( " 
		        + "    SELECT " 
		        + "        oc.batch_number, "
		        + "        COUNT(*) AS returned_cheque_count " 
		        + "    FROM public.outward_cheque oc "
		        + "    WHERE UPPER(TRIM(oc.cheque_status)) = 'SENT_BACK_TO_MAKER' "
		        + "      AND EXISTS ( " 
		        + "          SELECT 1 " 
		        + "          FROM public.cheque_processing cp "
		        + "          WHERE cp.batch_number = oc.batch_number "
		        + "            AND cp.cheque_number = oc.cheque_number "
		        + "            AND UPPER(TRIM(cp.checker_action)) = 'SEND_BACK' "
		        + "            AND ( " 
		        + "                 cp.checker_reason_code IS NULL "
		        + "                 OR UPPER(TRIM(cp.checker_reason_code)) NOT IN ( "
		        + "                           'MICR', " 
		        + "                           'MICR_CORRECTION', "
		        + "                           'MICR_MISMATCH' " 
		        + "                 ) " 
		        + "            ) " 
		        + "      ) "
		        + "    GROUP BY oc.batch_number "
		        + ") rc ON rc.batch_number = b.batch_number "
		        + "WHERE ( " 
		        + "        b.batch_status IN ( 'MICR_VERIFIED', 'MICR_REPAIR_COMPLETED' ) "
		        + "        OR COALESCE(rc.returned_cheque_count, 0) > 0 "
		        + "  ) "
		        + "GROUP BY "
		        + "    b.batch_number, "
		        + "    b.branch_code, "
		        + "    b.cheque_count, "
		        + "    b.batch_folder_path, "
		        + "    b.batch_status, "
		        + "    rc.returned_cheque_count "
		        + "ORDER BY b.batch_number ASC";
		try (Connection con = dataSource.getConnection();

				PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setLong(1, userId);

			try (ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {

					OutwardBatch batch = new OutwardBatch();

					batch.setBatchNumber(rs.getString("batch_number"));

					batch.setBranchCode(rs.getString("branch_code"));

					batch.setBatchFolderPath(rs.getString("batch_folder_path"));

					String originalBatchStatus = rs.getString("batch_status");

					batch.setMakerAssignmentStatus(rs.getString("assignment_status"));

					int returnedCount = rs.getInt("returned_cheque_count");

					if (returnedCount > 0) {

						batch.setNumberOfCheques(returnedCount);

						batch.setBatchStatus("SENT_TO_MAKER");

					} else {

						batch.setNumberOfCheques(rs.getInt("cheque_count"));

						batch.setBatchStatus(originalBatchStatus);
					}

					batches.add(batch);
				}
			}

		} catch (SQLException e) {

			System.err.println("Error loading Data Entry batches " + "for Maker: " + userId);

			e.printStackTrace();
		}

		return batches;
	}
}