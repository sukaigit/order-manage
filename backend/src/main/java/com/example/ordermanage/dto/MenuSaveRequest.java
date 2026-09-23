package com.example.ordermanage.dto;

import lombok.Data;

@Data
public class MenuSaveRequest {

    private String code;
    private String name;
    private Long parentId;
    private String route;
    private String menuType;
    private Integer sort;
    private String remark;
}
