package com.cts.inward.dto;

import java.util.List;

import com.cts.inward.model.NpciBatchData;
import com.cts.inward.model.NpciChequeData;

public class PxfParserResult {

    private final NpciBatchData batchData;
    private final List<NpciChequeData> chequeDataList;

    private PxfParserResult(
            NpciBatchData batchData,
            List<NpciChequeData> chequeDataList) {

        this.batchData = batchData;
        this.chequeDataList = chequeDataList;
    }

    public static PxfParserResult of(
            NpciBatchData batchData,
            List<NpciChequeData> chequeDataList) {

        return new PxfParserResult(
                batchData,
                chequeDataList);
    }

    public NpciBatchData getBatchData() {
        return batchData;
    }

    public List<NpciChequeData> getChequeDataList() {
        return chequeDataList;
    }
}