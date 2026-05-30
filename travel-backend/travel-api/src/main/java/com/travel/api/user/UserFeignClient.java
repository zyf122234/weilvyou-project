package com.travel.api.user;

import com.travel.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务Feign接口
 */
@FeignClient(
        contextId = "userFeignClient",
        name = "travel-user",
        url = "${travel.service.user-base-url}",
        path = "/user",
        fallback = UserFeignFallback.class)
public interface UserFeignClient {

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    R<UserVO> getUserById(@PathVariable("id") Long id);

    @GetMapping("/info")
    R<UserVO> getCurrentUserInfo(@RequestHeader(value = "Authorization", required = false) String authorization);

    /**
     * 根据用户名获取用户信息
     */
    @GetMapping("/username/{username}")
    R<UserVO> getUserByUsername(@PathVariable("username") String username);

    /**
     * 查询商家信息，用于商品服务展示和按商户名搜索
     */
    @GetMapping("/merchants")
    R<List<UserVO>> listMerchants(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam(name = "keyword", required = false) String keyword,
                                  @RequestParam(name = "ids", required = false) List<Long> ids);

    @PostMapping("/balance/deduct")
    R<UserVO> deductBalance(@RequestParam("userId") Long userId, @RequestBody UserBalanceDeductDTO dto);
}
