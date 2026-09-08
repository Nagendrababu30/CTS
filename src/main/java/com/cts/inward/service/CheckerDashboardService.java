package com.cts.inward.service;

import java.util.List;

import com.cts.inward.model.CheckerBatch;

public interface CheckerDashboardService {

	int getReceivedBatchCount();

	int getAvailableBatchCount();

	int getMyBatchCount(long userId);

	List<CheckerBatch> getBatches();

	boolean lockBatch(long batchId, long userId);
}