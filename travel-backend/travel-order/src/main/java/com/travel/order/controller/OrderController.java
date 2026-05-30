package com.travel.order.controller;

import com.travel.api.order.OrderCreateDTO;
import com.travel.api.order.OrderVO;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.R;
import com.travel.common.security.LoginUser;
import com.travel.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("!hasAuthority('ROLE_ADMIN')")
    public R<OrderVO> createOrder(@AuthenticationPrincipal LoginUser loginUser,
                                  @RequestBody @Valid OrderCreateDTO dto) {
        return R.ok(orderService.createOrder(resolveUserId(loginUser), dto));
    }

    @GetMapping("/{id}")
    public R<OrderVO> getMyOrder(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable("id") Long id) {
        return R.ok(orderService.getMyOrder(resolveUserId(loginUser), id));
    }

    // 支付
    @PatchMapping("/{id}/pay")
    public R<OrderVO> payOrder(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable("id") Long id) {
        try {
            return R.ok(orderService.payOrder(resolveUserId(loginUser), id));
        } catch (RuntimeException e) {
            BusinessException businessException = unwrapBusinessException(e);
            if (businessException != null) {
                return R.fail(businessException.getCode(), businessException.getMessage());
            }
            throw e;
        }
    }

    @PatchMapping("/{id}/cancel")
    public R<OrderVO> cancelOrder(@AuthenticationPrincipal LoginUser loginUser,
                                  @PathVariable("id") Long id) {
        return R.ok(orderService.cancelOrder(resolveUserId(loginUser), id));
    }

    @GetMapping("/mine")
    public R<PageResult<OrderVO>> listMyOrders(@AuthenticationPrincipal LoginUser loginUser,
                                               @RequestParam(name = "current", defaultValue = "1") Long current,
                                               @RequestParam(name = "size", defaultValue = "10") Long size) {
        return R.ok(orderService.listMyOrders(resolveUserId(loginUser), current, size));
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    public R<PageResult<OrderVO>> listMerchantOrders(@AuthenticationPrincipal LoginUser loginUser,
                                                     @RequestParam(name = "current", defaultValue = "1") Long current,
                                                     @RequestParam(name = "size", defaultValue = "10") Long size,
                                                     @RequestParam(name = "status", required = false) Integer status,
                                                     @RequestParam(name = "keyword", required = false) String keyword) {
        return R.ok(orderService.listMerchantOrders(resolveUserId(loginUser), current, size, status, keyword));
    }

    @GetMapping({"/admin/all", "/admin/list"})
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public R<PageResult<OrderVO>> listAllOrders(@RequestParam(name = "current", defaultValue = "1") Long current,
                                                @RequestParam(name = "size", defaultValue = "10") Long size,
                                                @RequestParam(name = "status", required = false) Integer status,
                                                @RequestParam(name = "keyword", required = false) String keyword) {
        return R.ok(orderService.listAllOrders(current, size, status, keyword));
    }

    private Long resolveUserId(LoginUser loginUser) {
        return loginUser == null ? null : loginUser.userId();
    }

    // 处理业务异常
    private BusinessException unwrapBusinessException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException;
            }
            current = current.getCause();
        }
        return null;
    }
}
