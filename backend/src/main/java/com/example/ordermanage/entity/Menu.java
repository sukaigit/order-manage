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
@TableName("tb_menu")
public class Menu {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    @TableField("parent_id")
    private Long parentId;
    private String route;
    @TableField("menu_type")
    private String menuType;
    private Integer sort;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
