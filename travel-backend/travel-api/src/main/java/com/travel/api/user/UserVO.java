package com.travel.api.user;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息响应
 */
@Data
public class UserVO implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String phone;
    private BigDecimal balance;
    private Integer status;
    private List<String> roles;
    private LocalDateTime createTime;
}
