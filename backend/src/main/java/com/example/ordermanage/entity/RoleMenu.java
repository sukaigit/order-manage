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
@TableName("tb_role_menu")
public class RoleMenu {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("role_id")
    private Long roleId;
    @TableField("menu_id")
    private Long menuId;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
