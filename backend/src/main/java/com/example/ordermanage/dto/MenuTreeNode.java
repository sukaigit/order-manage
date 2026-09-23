package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class MenuTreeNode {

    private Long id;
    private String code;
    private String name;
    private Long parentId;
    private String route;
    private String menuType;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<MenuFunctionItem> functions = new ArrayList<>();
    private List<MenuTreeNode> children = new ArrayList<>();
}
