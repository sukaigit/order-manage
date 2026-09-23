package com.example.ordermanage.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OrderSaveRequest {

    private String orderNo;
    private String name;
    private BigDecimal amount;
    private Long supplierId;
    private String remark;
}
