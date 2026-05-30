package com.travel.ai.tool;

import com.travel.api.order.OrderFeignClient;
import com.travel.api.product.ProductFeignClient;
import com.travel.api.user.UserFeignClient;
import com.travel.common.result.R;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CustomerServiceTools {

    private final ProductFeignClient productFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final UserFeignClient userFeignClient;
    private final String authorization;

    @Autowired
    public CustomerServiceTools(ProductFeignClient productFeignClient,
                                OrderFeignClient orderFeignClient,
                                UserFeignClient userFeignClient) {
        this(productFeignClient, orderFeignClient, userFeignClient, null);
    }

    private CustomerServiceTools(ProductFeignClient productFeignClient,
                                 OrderFeignClient orderFeignClient,
                                 UserFeignClient userFeignClient,
                                 String authorization) {
        this.productFeignClient = productFeignClient;
        this.orderFeignClient = orderFeignClient;
        this.userFeignClient = userFeignClient;
        this.authorization = authorization;
    }

    public CustomerServiceTools withAuthorization(String authorization) {
        return new CustomerServiceTools(productFeignClient, orderFeignClient, userFeignClient, authorization);
    }

    @Tool(name = "searchHotels", description = "按酒店名、城市、品牌、星级或价格区间搜索已发布酒店商品，适合回答酒店推荐、价格筛选、城市筛选问题")
    public Map<String, Object> searchHotels(
            @ToolParam(description = "搜索关键词，可以是酒店名、城市、品牌或商圈", required = false) String keyword,
            @ToolParam(description = "城市名称，例如北京、上海、广州等", required = false) String city,
            @ToolParam(description = "星级名称，例如五星级、豪华型、高档型等", required = false) String starName,
            @ToolParam(description = "酒店品牌", required = false) String brand,
            @ToolParam(description = "价格区间，例如100元以下、100-300元、300-600元、600-1500元、1500元以上", required = false) String priceRange) {
        R<Map<String, Object>> response = productFeignClient.searchHotels(
                clean(keyword), clean(city), clean(starName), clean(brand), clean(priceRange), 1L, 5L, false);
        return toolResult(response, "酒店搜索失败");
    }

    @Tool(name = "getHotelDetail", description = "根据酒店商品ID查询酒店详情，适合回答某个酒店的价格、城市、品牌、星级、商家等详情")
    public Map<String, Object> getHotelDetail(@ToolParam(description = "酒店商品ID") Long productId) {
        if (productId == null) {
            return fail("酒店商品ID不能为空");
        }
        return toolResult(productFeignClient.getPublishedDetail(productId), "酒店详情查询失败");
    }

    @Tool(name = "listMyOrders", description = "查询当前登录用户的最近订单，适合回答我的订单、订单列表、最近订单等问题")
    public Map<String, Object> listMyOrders(
            @ToolParam(description = "页码，从1开始", required = false) Long current,
            @ToolParam(description = "每页数量，默认5，最大10", required = false) Long size) {
        long safeCurrent = current == null || current < 1 ? 1 : current;
        long safeSize = size == null || size < 1 ? 5 : Math.min(size, 10);
        return toolResult(orderFeignClient.listMyOrders(authorization, safeCurrent, safeSize), "订单列表查询失败");
    }

    @Tool(name = "getMyOrderDetail", description = "根据订单ID查询当前登录用户自己的订单详情，适合回答订单状态、支付状态、入住时间、订单金额等问题")
    public Map<String, Object> getMyOrderDetail(@ToolParam(description = "订单ID") Long orderId) {
        if (orderId == null) {
            return fail("订单ID不能为空");
        }
        return toolResult(orderFeignClient.getMyOrder(authorization, orderId), "订单详情查询失败");
    }

    @Tool(name = "getCurrentUserInfo", description = "查询当前登录用户基础信息和余额，适合回答余额、账号信息、手机号、昵称等问题")
    public Map<String, Object> getCurrentUserInfo() {
        return toolResult(userFeignClient.getCurrentUserInfo(authorization), "用户信息查询失败");
    }

    @Tool(name = "getMerchantByKeyword", description = "按商家名称关键词查询商家信息，适合回答酒店所属商家、商家联系方式等问题")
    public Map<String, Object> getMerchantByKeyword(
            @ToolParam(description = "商家名称或关键词") String keyword) {
        R<?> response = userFeignClient.listMerchants(authorization, clean(keyword), null);
        if (response == null || response.getCode() == null || response.getCode() != 200) {
            return toolResult(response, "商家查询失败");
        }
        Object merchants = response.getData();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "success");
        result.put("total", merchants instanceof java.util.List<?> list ? list.size() : 0);
        result.put("records", merchants);
        return result;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // 工具结果处理
    private Map<String, Object> toolResult(R<?> response, String fallbackMessage) {
        if (response == null) {
            return fail(fallbackMessage);
        }
        if (response.getCode() == null || response.getCode() != 200) {
            String message = StringUtils.hasText(response.getMessage()) ? response.getMessage() : fallbackMessage;
            return fail(message);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", StringUtils.hasText(response.getMessage()) ? response.getMessage() : "success");
        result.put("data", response.getData());
        return result;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("data", null);
        return result;
    }
}
