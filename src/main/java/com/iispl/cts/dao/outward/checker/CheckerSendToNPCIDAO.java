package com.iispl.cts.dao.outward.checker;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.OutwardBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


public class CheckerSendToNPCIDAO {

	// Fetches batches that have completed Checker verification and are ready for
	// NPCI.
	public List<OutwardBatch> getBatchesReadyForNPCI() {

    public List<OutwardBatch> getBatchesReadyForNPCI() {

				"    COUNT(CASE " + "        WHEN UPPER(COALESCE(oc.cheque_status, '')) "
				+ "             = 'CHECKER_ACCEPTED' " + "        THEN 1 " + "    END) AS accepted_cheques, " +

				"    COUNT(CASE " + "        WHEN UPPER(COALESCE(oc.cheque_status, '')) "
				+ "             = 'CHECKER_REJECTED' " + "        THEN 1 " + "    END) AS rejected_cheques " +

				"FROM public.outward_batch ob " +

				"LEFT JOIN public.outward_cheque oc " + "    ON ob.batch_number = oc.batch_number " +

				"WHERE UPPER(ob.batch_status) = 'CHECKER_VERIFIED' " +

				"GROUP BY " + "    ob.batch_number, " + "    ob.branch_code, " + "    ob.cheque_count, "
				+ "    ob.batch_folder_path, " + "    ob.created_by, " + "    ob.created_at, " + "    ob.batch_status "
				+

				"ORDER BY ob.created_at DESC";

		try (Connection con = dataSource.getConnection();

				PreparedStatement ps = con.prepareStatement(sql);

				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				OutwardBatch batch = new OutwardBatch();

				batch.setBatchNumber(rs.getString("batch_number"));

				batch.setBranchCode(rs.getString("branch_code"));

				batch.setNumberOfCheques(rs.getInt("total_cheques"));

				batch.setBatchFolderPath(rs.getString("batch_folder_path"));

                batch.setBatchNumber(
                        rs.getString("batch_number")
                );

                batch.setBranchCode(
                        rs.getString("branch_code")
                );

                batch.setNumberOfCheques(
                        rs.getInt("total_cheques")
                );

                batch.setBatchFolderPath(
                        rs.getString("batch_folder_path")
                );

		if (batchNumber == null || batchNumber.trim().isEmpty()) {

                int createdBy =
                        rs.getInt("created_by");

		try (Connection con = dataSource.getConnection();

				PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setString(1, batchNumber.trim());

                if (rs.getTimestamp("created_at") != null) {

					return rs.getBoolean(1);
				}
			}

                batch.setBatchStatus(
                        rs.getString("batch_status")
                );

                batches.add(batch);
            }

			return false;
		}

		String sql = "UPDATE public.outward_batch " + "SET batch_status = 'NPCI_SENT' " + "WHERE batch_number = ? "
				+ "  AND UPPER(batch_status) <> 'NPCI_SENT'";

            throw new RuntimeException(
                    "Unable to load batches ready for NPCI.", e );
        }

        return batches;
    }

			int updated = ps.executeUpdate();

    // CHECK WHETHER BATCH IS READY

    public boolean isBatchReadyForNPCI(
            String batchNumber) {

			e.printStackTrace();

			throw new RuntimeException("Unable to mark batch as NPCI_SENT.", e);
		}
	}

	// Returns the current database status of the specified batch.
	public String getBatchStatus(String batchNumber) {

        String sql =
                "SELECT EXISTS (" +
                "    SELECT 1 " +
                "    FROM public.outward_batch " +
                "    WHERE batch_number = ? " +
                "      AND UPPER(batch_status) = 'CHECKER_VERIFIED' " + ")";

			return null;
		}

        try ( 
        		Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)) {

            ps.setString( 1, batchNumber.trim());

            try ( ResultSet rs =
                            ps.executeQuery() ) {

				if (rs.next()) {

					return rs.getString("batch_status");
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

            throw new RuntimeException(
                    "Unable to check NPCI batch status.",e );
        }
        return false;
    }

    // MARK BATCH AS NPCI SENT

    public boolean markBatchAsNPCISent(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {
        	
        	return false;
        }


        String sql =
                "UPDATE public.outward_batch " +
                "SET batch_status = 'NPCI_SENT' " +
                "WHERE batch_number = ? " +
                "  AND UPPER(batch_status) <> 'NPCI_SENT'";


        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)) {

            ps.setString( 1, batchNumber.trim() );

            int updated =
                    ps.executeUpdate();

            return updated == 1;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to mark batch as NPCI_SENT.", e );
        }
    }


    // GET BATCH STATUS

    
    public String getBatchStatus(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return null;
        }


        String sql =
                "SELECT batch_status " +
                "FROM public.outward_batch " +
                "WHERE batch_number = ?";


        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql) ) {

            ps.setString( 1, batchNumber.trim() );


            try (
                    ResultSet rs =
                            ps.executeQuery()) {

                if (rs.next()) {
                	
                	return rs.getString("batch_status" );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to get batch status.", e );
        }
        
        return null;
    }
}