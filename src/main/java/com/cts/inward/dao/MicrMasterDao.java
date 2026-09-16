package com.cts.inward.dao;

import com.cts.inward.model.MicrMaster;

public interface MicrMasterDao {

    boolean exists(String micrCode);

    java.util.Set<String> findExistingMicrCodes(java.util.Set<String> micrCodes);

    MicrMaster findByMicrCode(String micrCode);
}