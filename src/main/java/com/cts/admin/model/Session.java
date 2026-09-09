package com.cts.admin.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String sessionName;
    private String status;
    private Timestamp startedAt;
    private Timestamp endedAt;
    private Long startedBy;
    private Long endedBy;

    public Session() {
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }

    public Timestamp getEndedAt() { return endedAt; }
    public void setEndedAt(Timestamp endedAt) { this.endedAt = endedAt; }

    public Long getStartedBy() { return startedBy; }
    public void setStartedBy(Long startedBy) { this.startedBy = startedBy; }

    public Long getEndedBy() { return endedBy; }
    public void setEndedBy(Long endedBy) { this.endedBy = endedBy; }
}
