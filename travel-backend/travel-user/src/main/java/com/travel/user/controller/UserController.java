package com.travel.user.controller;

import com.travel.api.user.LoginDTO;
import com.travel.api.user.LoginVO;
import com.travel.api.user.MerchantApplicationVO;
import com.travel.api.user.RegisterDTO;
import com.travel.api.user.UserBalanceDeductDTO;
import com.travel.api.user.UserRechargeDTO;
import com.travel.api.user.UserUpdateDTO;
import com.travel.api.user.UserVO;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.travel.common.result.R;
import com.travel.common.security.LoginUser;
import com.travel.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Tag(name = "用户管理", description = "用户注册、登录、信息查询")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${travel.service.order-host}")
    private String orderServiceHost;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@RequestBody @Valid RegisterDTO dto) {
        userService.register(dto);
        return R.ok();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @SentinelResource(value = "userLogin", blockHandler = "loginBlockHandler")  // 限流以及降级处理
    public R<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        log.debug("用户登录成功，用户编号：{}", vo.getUserId());
        return R.ok(vo);
    }

    public R<LoginVO> loginBlockHandler(LoginDTO dto, BlockException ex) {
        return R.fail(429, "登录请求过于频繁，请稍后再试");
    }

    @Operation(summary = "用户退出登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        userService.logout(resolveToken(request));
        return R.ok();
    }

    @Operation(summary = "申请成为商户")
    @PostMapping("/apply-merchant")
    public R<MerchantApplicationVO> applyForMerchant(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null || loginUser.userId() == null) {
            return R.fail(401, "未登录");
        }
        return R.ok(userService.applyForMerchant(loginUser.userId()));
    }

    @Operation(summary = "查询商户申请状态")
    @GetMapping("/merchant-application")
    public R<MerchantApplicationVO> getMerchantApplication(@AuthenticationPrincipal LoginUser loginUser,
                                                           HttpServletRequest request) {
        if (loginUser == null || loginUser.userId() == null) {
            return R.fail(401, "未登录");
        }
        return R.ok(userService.getLatestMerchantApplication(loginUser.userId(), resolveToken(request)));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Operation(summary = "根据 ID 获取用户信息")
    @GetMapping("/{id}")
    public R<UserVO> getUserById(@PathVariable("id") Long id) {
        UserVO vo = userService.getUserById(id);
        return R.ok(vo);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public R<UserVO> getCurrentUserInfo(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null || loginUser.userId() == null) {
            return R.fail(401, "未登录");
        }
        UserVO vo = userService.getUserById(loginUser.userId());
        return R.ok(vo);
    }

    @Operation(summary = "更新当前登录用户信息")
    @PutMapping("/info")
    public R<UserVO> updateCurrentUserInfo(@AuthenticationPrincipal LoginUser loginUser,
                                           @RequestBody @Valid UserUpdateDTO dto) {
        if (loginUser == null || loginUser.userId() == null) {
            return R.fail(401, "未登录");
        }
        UserVO vo = userService.updateUserInfo(loginUser.userId(), dto);
        return R.ok(vo);
    }

    @Operation(summary = "充值当前登录用户余额")
    @PostMapping("/recharge")
    public R<UserVO> rechargeBalance(@AuthenticationPrincipal LoginUser loginUser,
                                     @RequestBody @Valid UserRechargeDTO dto) {
        if (loginUser == null || loginUser.userId() == null) {
            return R.fail(401, "未登录");
        }
        return R.ok(userService.rechargeBalance(loginUser.userId(), dto));
    }

    @Operation(summary = "扣减用户余额")
    @PostMapping("/balance/deduct")
    public R<UserVO> deductBalance(@RequestParam("userId") Long userId,
                                   @RequestBody @Valid UserBalanceDeductDTO dto,
                                   HttpServletRequest request) {
        if (!isOrderServiceCall(request)) {
            throw new AccessDeniedException("Access Denied");
        }
        if (userId == null) {
            return R.fail(400, "用户ID不能为空");
        }
        return R.ok(userService.deductBalance(userId, dto));
    }

    @Operation(summary = "上传当前登录用户头像")
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@AuthenticationPrincipal LoginUser loginUser,
                                  @RequestParam("file") MultipartFile file) {
        if (loginUser == null || loginUser.userId() == null) {
            return R.fail(401, "未登录");
        }
        String avatarUrl = userService.uploadAvatar(loginUser.userId(), file);
        return R.ok(avatarUrl);
    }

    @Operation(summary = "根据用户名获取用户信息")
    @GetMapping("/username/{username}")
    public R<UserVO> getUserByUsername(@PathVariable("username") String username) {
        UserVO vo = userService.getUserByUsername(username);
        return R.ok(vo);
    }

    @Operation(summary = "查询全部商家信息")
    @GetMapping("/merchants")
    public R<List<UserVO>> listMerchants(@RequestParam(name = "keyword", required = false) String keyword,
                                         @RequestParam(name = "ids", required = false) List<Long> ids) {
        return R.ok(userService.listMerchants(keyword, ids));
    }

    private boolean isOrderServiceCall(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        try {
            return Arrays.stream(InetAddress.getAllByName(orderServiceHost))
                    .map(InetAddress::getHostAddress)
                    .anyMatch(remoteAddress::equals);
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
