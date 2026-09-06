package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.dao.ChequeDao;
import com.cts.inward.model.InwardCheque;
import com.cts.inward.model.NpciChequeData;

public class ChequeDaoImpl implements ChequeDao {

    private final DataSource dataSource;

    private ChequeDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static ChequeDao of() {
        return new ChequeDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public void saveCheque(NpciChequeData cheque) {

        String sql =
                "INSERT INTO cheque " +
                "(chequenumber, batchid, accountnumber, drawername, " +
                "amount, micrcode, chequedate, presentingdate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    cheque.getChequeNumber());

            statement.setLong(
                    2,
                    cheque.getBatchId());

            statement.setString(
                    3,
                    cheque.getAccountNumber());

            statement.setString(
                    4,
                    cheque.getDrawerName());

            statement.setBigDecimal(
                    5,
                    cheque.getChequeAmount());

            statement.setString(
                    6,
                    cheque.getMicrCode());

            statement.setObject(
                    7,
                    cheque.getChequeDate());

            /*
             * PXF data does not currently contain
             * presenting date, so store NULL.
             */
            statement.setObject(
                    8,
                    null);

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to save cheque: "
                            + cheque.getChequeNumber(),
                    e);
        }
    }

    @Override
    public List<InwardCheque> getChequesForBatch(
            String batchId) {

        String sql =
                "SELECT chequenumber, batchid, accountnumber, " +
                "drawername, amount, micrcode, chequedate, " +
                "presentingdate " +
                "FROM cheque " +
                "WHERE batchid = ? " +
                "ORDER BY chequenumber";

        List<InwardCheque> cheques =
                new ArrayList<>();

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    InwardCheque cheque =
                            InwardCheque.of(
                                    resultSet.getString(
                                            "chequenumber"),

                                    resultSet.getString(
                                            "batchid"),

                                    resultSet.getString(
                                            "accountnumber"),

                                    resultSet.getString(
                                            "drawername"),

                                    resultSet.getBigDecimal(
                                            "amount"),

                                    resultSet.getString(
                                            "micrcode"),

                                    resultSet.getDate(
                                            "chequedate")
                                            .toLocalDate(),

                                    resultSet.getDate(
                                            "presentingdate") != null
                                            ? resultSet.getDate(
                                                    "presentingdate")
                                                    .toLocalDate()
                                            : null
                            );

                    cheques.add(cheque);
                }
            }

            return cheques;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to retrieve cheques for batch: "
                            + batchId,
                    e);
        }
    }
}