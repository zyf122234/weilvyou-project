package com.travel.api.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantApplicationVO {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String email;

    private Integer status;

    private String reason;

    private String token;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static MerchantApplicationVO of(Long id,
                                           Long userId,
                                           String username,
                                           String nickname,
                                           String email,
                                           Integer status,
                                           String reason,
                                           String token,
                                           LocalDateTime createTime,
                                           LocalDateTime updateTime) {
        MerchantApplicationVO vo = new MerchantApplicationVO();
        vo.setId(id);
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setNickname(nickname);
        vo.setEmail(email);
        vo.setStatus(status);
        vo.setReason(reason);
        vo.setToken(token);
        vo.setCreateTime(createTime);
        vo.setUpdateTime(updateTime);
        return vo;
    }
}
