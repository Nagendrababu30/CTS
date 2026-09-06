package com.cts.inward.dao;

import com.cts.inward.model.MicrMaster;

public interface MicrMasterDao {

    boolean exists(String micrCode);

    MicrMaster findByMicrCode(String micrCode);
}