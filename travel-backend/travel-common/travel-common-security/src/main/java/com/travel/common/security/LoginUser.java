package com.travel.common.security;

/**
 * 当前登录用户身份信息。
 * record 类，用于封装当前登录用户信息。会自动填充getter方法和全参构造函数 。
 */
public record LoginUser(Long userId, String username, String loginSessionId) {
}
