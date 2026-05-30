package com.travel.api.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String description;

    private String destination;

    private String city;

    private String coverUrl;

    private String tag;

    private String brand;

    private String starName;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.00", message = "商品价格不能小于0")
    private BigDecimal price;
}
