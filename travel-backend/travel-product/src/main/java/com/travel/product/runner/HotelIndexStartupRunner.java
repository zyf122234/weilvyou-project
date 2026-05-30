package com.travel.product.runner;

import com.travel.product.service.HotelIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// 启动时重建索引库
// 重写run(String... args)方法，内部编写业务逻辑
// 项目启动、容器初始化完毕后，自动触发执行
@Component
@RequiredArgsConstructor
public class HotelIndexStartupRunner implements CommandLineRunner {

    private final HotelIndexService hotelIndexService;

    @Override
    public void run(String... args) {
        hotelIndexService.rebuildPublishedHotels();
    }
}
