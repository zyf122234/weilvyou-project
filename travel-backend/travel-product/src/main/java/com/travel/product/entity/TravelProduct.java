package com.travel.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("travel_product")
public class TravelProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String destination;

    private String coverUrl;

    private String tag;

    private BigDecimal price;

    private Integer status;  // 0: 待审核 1: 审核通过 2: 审核失败

    private Long merchantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
