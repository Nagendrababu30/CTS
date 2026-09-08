package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cts.admin.model.Session;
import com.cts.inward.config.ConnectionPool;

public class SessionDAOImpl implements SessionDAO {

    @Override
    public boolean startSession(Long userId) {

        String sessionName = "Clearing Session - "
                + new java.text.SimpleDateFormat("dd MMM yyyy HH:mm")
                        .format(new java.util.Date());

        java.sql.Timestamp nowIST = new java.sql.Timestamp(
                java.util.Calendar.getInstance(
                        java.util.TimeZone.getTimeZone("Asia/Kolkata")
                ).getTimeInMillis());

        String sql =
                "INSERT INTO sessions "
                + "(session_name, status, started_at, started_by) "
                + "VALUES (?, 'STARTED', ?, ?)";

        try (
                Connection connection =
                        ConnectionPool.getDataSource().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, sessionName);
            statement.setTimestamp(2, nowIST);
            statement.setLong(3, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to start internal processing session.",
                    e
            );
        }
    }


    @Override
    public boolean endSession(Long sessionId, Long userId) {

        java.sql.Timestamp nowIST = new java.sql.Timestamp(
                java.util.Calendar.getInstance(
                        java.util.TimeZone.getTimeZone("Asia/Kolkata")
                ).getTimeInMillis());

        String sql =
                "UPDATE sessions "
                + "SET status = 'ENDED', "
                + "    ended_at = ?, "
                + "    ended_by = ? "
                + "WHERE session_id = ? "
                + "AND status = 'STARTED'";

        try (
                Connection connection =
                        ConnectionPool.getDataSource().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setTimestamp(1, nowIST);
            statement.setLong(2, userId);
            statement.setLong(3, sessionId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to end internal processing session.",
                    e
            );
        }
    }


    @Override
    public Session getActiveSession() {

        String sql =
                "SELECT session_id, "
                + "       session_name, "
                + "       status, "
                + "       started_at, "
                + "       ended_at, "
                + "       started_by, "
                + "       ended_by "
                + "FROM sessions "
                + "WHERE status = 'STARTED' "
                + "ORDER BY session_id DESC "
                + "LIMIT 1";

        try (
                Connection connection =
                        ConnectionPool.getDataSource().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            if (resultSet.next()) {

                return mapSession(resultSet);
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to fetch active internal processing session.",
                    e
            );
        }

        return null;
    }


    @Override
    public List<Session> getAllSessions() {

        List<Session> sessions = new ArrayList<>();

        String sql =
                "SELECT session_id, "
                + "       session_name, "
                + "       status, "
                + "       started_at, "
                + "       ended_at, "
                + "       started_by, "
                + "       ended_by "
                + "FROM sessions "
                + "ORDER BY session_id DESC";

        try (
                Connection connection =
                        ConnectionPool.getDataSource().getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                sessions.add(mapSession(resultSet));
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to fetch session history.",
                    e
            );
        }

        return sessions;
    }


    private Session mapSession(ResultSet resultSet)
            throws SQLException {

        Session session = new Session();

        session.setSessionId(resultSet.getLong("session_id"));
        session.setSessionName(resultSet.getString("session_name"));
        session.setStatus(resultSet.getString("status"));
        session.setStartedAt(resultSet.getTimestamp("started_at"));
        session.setEndedAt(resultSet.getTimestamp("ended_at"));

        long startedByValue = resultSet.getLong("started_by");
        if (!resultSet.wasNull()) {
            session.setStartedBy(startedByValue);
        }

        long endedByValue = resultSet.getLong("ended_by");
        if (!resultSet.wasNull()) {
            session.setEndedBy(endedByValue);
        }

        return session;
    }
}
