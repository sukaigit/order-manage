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
@TableName("tb_role_function")
public class RoleFunction {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("role_id")
    private Long roleId;
    @TableField("function_id")
    private Long functionId;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
