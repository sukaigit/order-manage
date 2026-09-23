package com.example.ordermanage.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SupplierAmountItem {

    private Long supplierId;
    private String supplierName;
    private BigDecimal amount;
}
