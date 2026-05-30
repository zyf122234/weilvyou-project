package com.travel.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 用户服务静态资源映射。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 上传文件根目录
    @Value("${travel.upload.base-dir}")
    private String uploadBaseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件 将路径转换为绝对路径
        Path uploadPath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")  // 添加资源处理器，处理以/uploads开头的请求
                .addResourceLocations(uploadPath.toUri().toString());  //真实的本机磁盘路径
        //请求地址去掉 /uploads，剩下的部分直接拼到你配置的磁盘目录后面！
    }
}
