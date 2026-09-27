package com.cts.inward.dao;

import java.util.Set;

// DAO interface for checking MICR codes against the master directory
public interface MicrMasterDao {

    // Checks if a 9-digit MICR code exists in the master table
    boolean exists(String micrCode);

    // Bulk checks existing MICR codes from a set of codes
    Set<String> findExistingMicrCodes(Set<String> micrCodes);
}