package com.example.ordermanage.dto;

import lombok.Data;

@Data
public class SupplierSaveRequest {

    private String code;
    private String name;
    private String contact;
    private String phone;
    private String address;
    private String status;
}
