package com.travel.api.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVO {

    private Long id;

    private String name;

    private String description;

    private String destination;

    private String city;

    private String coverUrl;

    private String tag;

    private String brand;

    private String starName;

    private BigDecimal price;

    private Integer status;

    private Long merchantId;

    private String merchantName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
