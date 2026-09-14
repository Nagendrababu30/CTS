package com.cts.inward.service;

public interface CheckerReportService {

    byte[] generateRrfXml();

    byte[] generateApprovedXml();
}