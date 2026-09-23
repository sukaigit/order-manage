package com.example.ordermanage.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderAuditListItem {

    private Long id;
    private String orderNo;
    private String name;
    private BigDecimal amount;
    private Long supplierId;
    private String supplierName;
    private String status;
    private LocalDateTime createTime;
    private String auditor;
    private LocalDateTime auditTime;
    private String opinion;
    private long pendingCount;
}
