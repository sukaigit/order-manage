package com.example.ordermanage.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class StatusDistItem {

    private String status;
    private long count;
    private BigDecimal percent;
}
