package com.example.ordermanage.dto;

import java.util.List;
import lombok.Data;

@Data
public class RolePermissionsResponse {

    private Long roleId;
    private List<Long> menuIds;
    private List<Long> functionIds;
}
