package com.example.ordermanage.dto;

import java.util.List;
import lombok.Data;

@Data
public class ReportSummaryResponse {

    private long total;
    private List<StatusDistItem> statusDist;
    private List<SupplierAmountItem> supplierAmount;
    private List<MonthlyTrendItem> monthlyTrend;
}
