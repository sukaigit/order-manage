package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserSaveRequest {

    private String username;
    private String realName;
    private Long roleId;
    private Long departmentId;
    private Long organizationId;
    private String status;
    private String remark;
}
