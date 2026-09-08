package com.cts.admin.service;

import java.util.List;

import com.cts.admin.model.Session;

public interface SessionService {

    boolean startSession(Long userId);

    boolean endSession(Long sessionId, Long userId);

    Session getActiveSession();

    List<Session> getAllSessions(int limit, int offset);

    int getSessionCount();
}
