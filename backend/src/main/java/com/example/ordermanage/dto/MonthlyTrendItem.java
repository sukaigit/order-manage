package com.example.ordermanage.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MonthlyTrendItem {

    private String month;
    private long count;
    private BigDecimal amount;
}
