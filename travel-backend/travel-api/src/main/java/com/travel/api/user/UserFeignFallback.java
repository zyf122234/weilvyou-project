package com.travel.api.user;

import com.travel.common.result.R;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserFeignFallback implements UserFeignClient {

    private static final String MESSAGE = "用户服务暂时不可用，请稍后再试";

    @Override
    public R<UserVO> getUserById(Long id) {
        return R.fail(503, MESSAGE);
    }

    @Override
    public R<UserVO> getCurrentUserInfo(String authorization) {
        return R.fail(503, MESSAGE);
    }

    @Override
    public R<UserVO> getUserByUsername(String username) {
        return R.fail(503, MESSAGE);
    }

    @Override
    public R<List<UserVO>> listMerchants(String authorization, String keyword, List<Long> ids) {
        return R.fail(503, MESSAGE);
    }

    @Override
    public R<UserVO> deductBalance(Long userId, UserBalanceDeductDTO dto) {
        return R.fail(503, MESSAGE);
    }
}
