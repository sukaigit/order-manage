package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FunctionListItem {

    private Long id;
    private String code;
    private String name;
    private Long menuId;
    private String menuName;
    private String perm;
    private String remark;
    private LocalDateTime createTime;
}
