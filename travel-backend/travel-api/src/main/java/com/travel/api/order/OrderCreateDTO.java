package com.travel.api.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrderCreateDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private LocalDate dateStart;

    private LocalDate dateEnd;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;

    @DecimalMin(value = "0.00", message = "订单金额不能为负数")
    private BigDecimal totalPrice;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    private String phone;
}
