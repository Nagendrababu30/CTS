package com.cts.admin.dao;

import java.util.List;

import com.cts.admin.model.Session;

public interface SessionDAO {

    boolean startSession(Long userId);

    boolean endSession(Long sessionId, Long userId);

    Session getActiveSession();

    List<Session> getAllSessions();
}
