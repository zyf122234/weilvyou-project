package com.travel.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatarUrl;

    private String email;

    private String phone;

    private BigDecimal balance;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 逻辑删除字段默认值 -- 0: 未删除，1: 已删除,但还在数据库中保留
    @TableLogic
    private Integer deleted;
    /***
     * 执行 userMapper.deleteById(1)
     * 自动变成：
     *
     *  UPDATE sys_user
     *  SET deleted = 1
     *  WHERE id = 1 AND deleted = 0
     *
     * 执行查询方法（selectList /selectOne/getById 等）
     * 自动拼接：
     *  WHERE deleted = 0
     */



}
