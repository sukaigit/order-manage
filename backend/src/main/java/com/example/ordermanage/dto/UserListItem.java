package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserListItem {

    private Long id;
    private String username;
    private String realName;
    private Long roleId;
    private String roleName;
    private Long departmentId;
    private String departmentName;
    private Long organizationId;
    private String organizationName;
    private String status;
    private Boolean firstLogin;
    private Integer failCount;
    private Boolean locked;
    private String remark;
    private LocalDateTime createTime;
}
