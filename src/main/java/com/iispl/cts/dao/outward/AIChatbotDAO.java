package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.iispl.cts.data.CTSStaticData;

public class AIChatbotDAO {

    // =========================================================
    // GET TOTAL OUTWARD BATCH COUNT
    // =========================================================

    public int getTotalBatchCount() {

        String sql =
                "SELECT COUNT(*) " +
                "FROM public.outward_batch";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return 0;
    }


    // =========================================================
    // GET BATCH COUNT BY STATUS
    // =========================================================

    public int getBatchCountByStatus(String status) {

        String sql =
                "SELECT COUNT(*) " +
                "FROM public.outward_batch " +
                "WHERE UPPER(batch_status) = UPPER(?)";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, status);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return 0;
    }
}