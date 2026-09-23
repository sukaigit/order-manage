package com.example.ordermanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("tb_organization")
public class Organization {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    @TableField("short_name")
    private String shortName;
    private String level;
    @TableField("parent_id")
    private Long parentId;
    private String contact;
    private String phone;
    private String region;
    private String address;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
