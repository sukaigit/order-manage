package com.example.ordermanage.dto;

import lombok.Data;

@Data
public class OrganizationSaveRequest {

    private String code;
    private String name;
    private String shortName;
    private Long parentId;
    private String contact;
    private String phone;
    private String region;
    private String address;
    private String remark;
}
