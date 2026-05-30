package com.travel.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateDTO implements Serializable {

    @Size(min = 3, max = 20, message = "用户名长度必须在 3 到 20 位之间")
    private String username;

    @Size(max = 30, message = "商户名称不能超过 30 个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;
}
