package com.travel.order;

import com.travel.api.product.ProductFeignClient;
import com.travel.api.user.UserFeignClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.travel")
@EnableDiscoveryClient
@EnableFeignClients(clients = {ProductFeignClient.class, UserFeignClient.class})
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
