package com.travel.api.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应
 */
@Data
public class LoginVO implements Serializable {

    private String token;
    private String loginSessionId;
    private Long userId;
    private String username;
    private List<String> roles;
}
