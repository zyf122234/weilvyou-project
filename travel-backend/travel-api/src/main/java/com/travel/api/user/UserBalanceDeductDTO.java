package com.travel.api.user;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class UserBalanceDeductDTO implements Serializable {

    @NotNull(message = "扣减金额不能为空")
    @DecimalMin(value = "0.01", message = "扣减金额必须大于 0")
    @Digits(integer = 10, fraction = 2, message = "扣减金额最多保留两位小数")
    private BigDecimal amount;
}
