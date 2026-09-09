package com.cts.admin.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Batch implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long      batchId;
    private String    batchType;
    private String    branch;
    private int       chequeCount;
    private String    status;
    private String    maker;
    private String    checker;
    private String    capturedBy;
    private Timestamp createdAt;
    private Timestamp makerReceiveTime;
    private Timestamp checkerReceiveTime;
    private Timestamp batchCompletionTime;

    public Batch() {}

    public Long      getBatchId()                                  { return batchId; }
    public void      setBatchId(Long batchId)                      { this.batchId = batchId; }
    public String    getBatchType()                                { return batchType; }
    public void      setBatchType(String batchType)                { this.batchType = batchType; }
    public String    getBranch()                                   { return branch; }
    public void      setBranch(String branch)                      { this.branch = branch; }
    public int       getChequeCount()                              { return chequeCount; }
    public void      setChequeCount(int chequeCount)               { this.chequeCount = chequeCount; }
    public String    getStatus()                                   { return status; }
    public void      setStatus(String status)                      { this.status = status; }
    public String    getMaker()                                    { return maker; }
    public void      setMaker(String maker)                        { this.maker = maker; }
    public String    getChecker()                                  { return checker; }
    public void      setChecker(String checker)                    { this.checker = checker; }
    public String    getCapturedBy()                               { return capturedBy; }
    public void      setCapturedBy(String capturedBy)              { this.capturedBy = capturedBy; }
    public Timestamp getCreatedAt()                                { return createdAt; }
    public void      setCreatedAt(Timestamp createdAt)             { this.createdAt = createdAt; }
    public Timestamp getMakerReceiveTime()                         { return makerReceiveTime; }
    public void      setMakerReceiveTime(Timestamp t)              { this.makerReceiveTime = t; }
    public Timestamp getCheckerReceiveTime()                       { return checkerReceiveTime; }
    public void      setCheckerReceiveTime(Timestamp t)            { this.checkerReceiveTime = t; }
    public Timestamp getBatchCompletionTime()                      { return batchCompletionTime; }
    public void      setBatchCompletionTime(Timestamp t)           { this.batchCompletionTime = t; }
}
