package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderAuditHistoryItem {

    private Long id;
    private Long orderId;
    private String result;
    private String auditor;
    private String opinion;
    private LocalDateTime createTime;
}
