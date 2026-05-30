package com.travel.api.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long userId;

    private String username;

    private String userNickname;

    private Long productId;

    private String productName;

    private String productCoverUrl;

    private Long merchantId;

    private String merchantName;

    private LocalDate dateStart;

    private LocalDate dateEnd;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private String contactName;

    private String phone;

    private Integer status;

    private String statusText;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
