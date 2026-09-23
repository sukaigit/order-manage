package com.example.ordermanage.dto;

import lombok.Data;

@Data
public class WorkbenchStatsResponse {

    private long totalOrders;
    private long pendingOrders;
    private long doneOrders;
    private long rejectedOrders;
    private long activeSuppliers;
    private Welcome welcome;

    @Data
    public static class Welcome {
        private String title;
        private long pendingCount;
        private long rejectedCount;
        private String tips;
    }
}
