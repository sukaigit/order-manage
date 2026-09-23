package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LogListItem {

    private Long id;
    private String operator;
    private String action;
    private String target;
    private String ip;
    private LocalDateTime createTime;
}
