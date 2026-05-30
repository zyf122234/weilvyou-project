package com.travel.api.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUserStatusDTO {

    @NotNull(message = "账号状态不能为空")
    private Integer status;
}
