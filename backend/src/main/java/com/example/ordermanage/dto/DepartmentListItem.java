package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DepartmentListItem {

    private Long id;
    private String code;
    private String name;
    private String remark;
    private LocalDateTime createTime;
}
