package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RoleListItem {

    private Long id;
    private String code;
    private String name;
    private String remark;
    private Long menuCount;
    private Long functionCount;
    private Long userCount;
    private Boolean builtin;
    private LocalDateTime createTime;
}
