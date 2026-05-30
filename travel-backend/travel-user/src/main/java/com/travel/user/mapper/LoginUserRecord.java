package com.travel.user.mapper;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoginUserRecord {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String phone;
    private BigDecimal balance;
    private Integer status;
    private LocalDateTime createTime;
    private String roles;
}
