package com.travel.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/// 商户入驻申请
@Data
@TableName("merchant_application")
public class MerchantApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer status;

    private String reason;  // 审核理由

    private Long reviewerId;  // 审核人id

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
