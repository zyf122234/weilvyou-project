package com.travel.api.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantApplicationReviewDTO {

    @NotNull(message = "审核状态不能为空")
    private Integer status;

    private String reason;
}
