package com.travel.user.controller;

import com.travel.api.admin.AdminUserStatusDTO;
import com.travel.api.admin.MerchantApplicationReviewDTO;
import com.travel.api.user.MerchantApplicationVO;
import com.travel.api.user.UserVO;
import com.travel.common.result.PageResult;
import com.travel.common.result.R;
import com.travel.common.security.LoginUser;
import com.travel.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口 - 供管理端直接调用
 */
@Tag(name = "用户管理接口", description = "管理端用户管理接口")
@RestController
@RequestMapping("/user/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "查询用户列表")
    @GetMapping("/users")
    public R<PageResult<UserVO>> listUsers(@AuthenticationPrincipal LoginUser loginUser,
                                            @RequestParam(value = "current", defaultValue = "1") Long current,
                                            @RequestParam(value = "size", defaultValue = "10") Long size,
                                            @RequestParam(value = "keyword", required = false) String keyword,
                                            @RequestParam(value = "status", required = false) Integer status,
                                            @RequestParam(value = "role", required = false) String role) {
        return R.ok(userService.listUsers(loginUser.userId(), current, size, keyword, status, role));
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/users/{id}/status")
    public R<UserVO> updateUserStatus(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable("id") Long id,
                                       @RequestBody @Valid AdminUserStatusDTO dto) {
        return R.ok(userService.updateUserStatus(loginUser.userId(), id, dto.getStatus()));
    }

    @Operation(summary = "查询商户申请列表")
    @GetMapping("/merchant-applications")
    public R<PageResult<MerchantApplicationVO>> listMerchantApplications(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "20") Long size,
            @RequestParam(value = "status", required = false) Integer status) {
        return R.ok(userService.listMerchantApplications(loginUser.userId(), current, size, status));
    }

    @Operation(summary = "审核商户申请")
    @PutMapping("/merchant-applications/{id}")
    public R<MerchantApplicationVO> reviewMerchantApplication(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable("id") Long id,
            @RequestBody @Valid MerchantApplicationReviewDTO dto) {
        return R.ok(userService.reviewMerchantApplication(loginUser.userId(), id, dto.getStatus(), dto.getReason()));
    }

    @Operation(summary = "根据申请ID查询商户申请详情")
    @GetMapping("/merchant-applications/{id}")
    public R<MerchantApplicationVO> getMerchantApplicationById(@AuthenticationPrincipal LoginUser loginUser,
                                                               @PathVariable("id") Long id) {
        return R.ok(userService.getMerchantApplicationById(loginUser.userId(), id));
    }
}
