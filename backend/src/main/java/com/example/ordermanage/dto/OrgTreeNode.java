package com.example.ordermanage.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class OrgTreeNode {

    private Long id;
    private String code;
    private String name;
    private String shortName;
    private String level;
    private Long parentId;
    private String contact;
    private String phone;
    private String region;
    private String address;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<OrgTreeNode> children = new ArrayList<>();
}
