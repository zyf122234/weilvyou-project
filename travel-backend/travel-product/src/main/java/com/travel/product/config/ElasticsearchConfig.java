package com.travel.product.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Arrays;

@Configuration
public class ElasticsearchConfig {

    // 创建 RestHighLevelClient
    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient(
            @Value("${travel.elasticsearch.uris}") String uris) {
        HttpHost[] hosts = Arrays.stream(uris.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::toHttpHost)
                .toArray(HttpHost[]::new);
        return new RestHighLevelClient(RestClient.builder(hosts));
    }

    // 兼容 http:// 和 https://
    private HttpHost toHttpHost(String uriText) {
        String normalized = uriText.contains("://") ? uriText : "http://" + uriText;
        URI uri = URI.create(normalized);
        return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    }
}
