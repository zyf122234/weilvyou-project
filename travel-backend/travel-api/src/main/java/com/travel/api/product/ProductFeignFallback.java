package com.travel.api.product;

import com.travel.common.result.R;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductFeignFallback implements ProductFeignClient {

    @Override
    public R<Map<String, Object>> searchHotels(String keyword,
                                               String city,
                                               String starName,
                                               String brand,
                                               String priceRange,
                                               Long current,
                                               Long size,
                                               Boolean all) {
        return R.fail(503, "商品搜索服务暂时不可用，请稍后再试");
    }

    @Override
    public R<ProductVO> getPublishedDetail(Long id) {
        return R.fail(503, "商品服务暂时不可用，请稍后再试");
    }
}
