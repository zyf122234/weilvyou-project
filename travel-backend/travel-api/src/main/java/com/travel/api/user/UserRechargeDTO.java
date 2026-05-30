package com.travel.api.user;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class UserRechargeDTO implements Serializable {

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    @Digits(integer = 10, fraction = 2, message = "充值金额最多保留两位小数")  // 金额限制
    private BigDecimal amount;
}
