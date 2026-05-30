package com.travel.api.product;

import com.travel.common.result.R;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "productFeignClient", name = "travel-product", path = "/product", fallback = ProductFeignFallback.class)
public interface ProductFeignClient {

    @GetMapping("/hotel/search")
    R<Map<String, Object>> searchHotels(@RequestParam(name = "keyword", required = false) String keyword,
                                        @RequestParam(name = "city", required = false) String city,
                                        @RequestParam(name = "starName", required = false) String starName,
                                        @RequestParam(name = "brand", required = false) String brand,
                                        @RequestParam(name = "priceRange", required = false) String priceRange,
                                        @RequestParam(name = "current", defaultValue = "1") Long current,
                                        @RequestParam(name = "size", defaultValue = "5") Long size,
                                        @RequestParam(name = "all", defaultValue = "false") Boolean all);

    @GetMapping("/published/{id}")
    R<ProductVO> getPublishedDetail(@PathVariable("id") Long id);
}
