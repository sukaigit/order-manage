package com.example.ordermanage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuery {

    private String keyword;
    private Long roleId;
    private String status;
    private Long departmentId;
    private Long organizationId;
}
