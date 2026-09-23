package com.example.ordermanage.dto;

import java.util.List;
import lombok.Data;

@Data
public class RolePermissionsRequest {

    private List<Long> menuIds;
    private List<Long> functionIds;
}
