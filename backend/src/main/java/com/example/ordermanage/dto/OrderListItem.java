package com.example.ordermanage.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderListItem {

    private Long id;
    private String orderNo;
    private String name;
    private BigDecimal amount;
    private Long supplierId;
    private String supplierName;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
