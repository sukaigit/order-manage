package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderAuditActionResponse {

    private Long orderId;
    private String status;
    private AuditInfo audit;

    @Data
    public static class AuditInfo {

        private String result;
        private String auditor;
        private String opinion;
        private LocalDateTime auditTime;
    }
}
