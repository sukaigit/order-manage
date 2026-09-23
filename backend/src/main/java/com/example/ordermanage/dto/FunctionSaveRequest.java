package com.example.ordermanage.dto;

import lombok.Data;

@Data
public class FunctionSaveRequest {

    private String name;
    private Long menuId;
    private String perm;
    private String remark;
}
