package com.cts.inward.service;

import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.dao.CheckerDashboardDAO;
import com.cts.inward.dao.CheckerDashboardDAOImpl;
import com.cts.inward.model.CheckerBatch;

public class CheckerDashboardServiceImpl implements CheckerDashboardService {

	private final CheckerDashboardDAO checkerDashboardDAO;

	public CheckerDashboardServiceImpl() {

		checkerDashboardDAO = new CheckerDashboardDAOImpl(ConnectionPool.getDataSource());
	}

	@Override
	public int getReceivedBatchCount() {

		return checkerDashboardDAO.getReceivedBatchCount();
	}

	@Override
	public int getAvailableBatchCount() {

		return checkerDashboardDAO.getAvailableBatchCount();
	}

	@Override
	public int getMyBatchCount(long userId) {

		return checkerDashboardDAO.getMyBatchCount(userId);
	}

	@Override
	public List<CheckerBatch> getBatches() {

		return checkerDashboardDAO.getBatches();
	}

	@Override
	public boolean lockBatch(long batchId, long userId) {

		return checkerDashboardDAO.lockBatch(batchId, userId);
	}
}