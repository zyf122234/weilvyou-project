package com.travel.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.product.entity.TravelProduct;
import com.travel.product.mapper.TravelProductMapper;
import com.travel.product.service.HotelIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelIndexServiceImpl implements HotelIndexService {

    private static final String HOTEL_INDEX = "hotel";
    private static final int HOTEL_MAPPING_VERSION = 5;

    private final RestHighLevelClient restHighLevelClient;
    private final TravelProductMapper productMapper;

    // 重建索引库：全量
    @Override
    public void rebuildPublishedHotels() {
        try {
            recreateIndexForFullSyncIfNeeded();
            List<TravelProduct> products = productMapper.selectList(new LambdaQueryWrapper<TravelProduct>()
                    .eq(TravelProduct::getStatus, 1)
                    .orderByDesc(TravelProduct::getUpdateTime));
            if (products.isEmpty()) {
                return;
            }

            // 批量插入
            BulkRequest bulkRequest = new BulkRequest();
            // 写入后立即刷新索引库
            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            for (TravelProduct product : products) {
                bulkRequest.add(toIndexRequest(product));
            }
            restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);   // 批量插入
            log.info("已同步{}条已发布商品到搜索索引", products.size());
        } catch (Exception e) {
            log.warn("同步商品到搜索索引失败", e);
        }
    }

    // 保存已发布的酒店数据
    @Override
    public void savePublishedHotel(TravelProduct product) {
        if (product == null || product.getId() == null) {
            return;
        }
        // 判断商品是否已发布
        if (product.getStatus() == null || product.getStatus() != 1) {
            deleteHotel(product.getId());
            return;
        }
        try {
            createIndexIfAbsent();
            IndexRequest request = toIndexRequest(product); // 创建写入索引请求
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);  //当数据被写入到es中时，立即刷新索引
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.warn("保存商品{}到酒店搜索索引失败", product.getId(), e);
        }
    }

    // 删除数据
    @Override
    public void deleteHotel(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            DeleteRequest request = new DeleteRequest(HOTEL_INDEX, String.valueOf(productId));
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            restHighLevelClient.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.warn("从酒店搜索索引删除商品{}失败", productId, e);
        }
    }

    // 重建索引库：全量
    private void recreateIndexForFullSyncIfNeeded() throws IOException {
        if (!indexExists()) {
            createHotelIndex();
            return;
        }
        if (isCurrentMappingVersion()) {
            return;
        }
        if (!areRequiredAnalyzersAvailable()) {
            log.warn("中文分词器或拼音分词器不可用，保留现有酒店搜索索引并跳过映射重建");
            return;
        }

        restHighLevelClient.indices().delete(new DeleteIndexRequest(HOTEL_INDEX), RequestOptions.DEFAULT);
        createHotelIndex();
        log.info("已按映射版本{}重建酒店搜索索引", HOTEL_MAPPING_VERSION);
    }

    // 创建索引库
    private void createIndexIfAbsent() throws IOException {
        if (!indexExists()) {
            createHotelIndex();
        }
    }

    private boolean indexExists() throws IOException {
        return restHighLevelClient.indices().exists(new GetIndexRequest(HOTEL_INDEX), RequestOptions.DEFAULT);
    }

    // 判断索引库版本
    private boolean isCurrentMappingVersion() {
        try {
            Map<String, Object> mapping = restHighLevelClient.indices()
                    .getMapping(new GetMappingsRequest().indices(HOTEL_INDEX), RequestOptions.DEFAULT)
                    .mappings()
                    .get(HOTEL_INDEX)
                    .sourceAsMap();
            Object meta = mapping.get("_meta");
            if (meta instanceof Map<?, ?> metaMap) {
                return String.valueOf(HOTEL_MAPPING_VERSION).equals(String.valueOf(metaMap.get("mappingVersion")));
            }
        } catch (Exception e) {
            log.warn("读取酒店搜索索引映射版本失败", e);
        }
        return false;
    }

    private boolean areRequiredAnalyzersAvailable() {
        return canAnalyzeWith("ik_smart") && isPluginLoaded("py");
    }

    // 判断分词器是否可用
    private boolean canAnalyzeWith(String analyzer) {
        try {
            Request request = new Request("POST", "/_analyze");
            request.setJsonEntity("""
                    {
                      "analyzer": "%s",
                      "text": "上海酒店搜索"
                    }
                    """.formatted(analyzer));
            restHighLevelClient.getLowLevelClient().performRequest(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 判断插件是否已安装
    private boolean isPluginLoaded(String pluginName) {
        try {
            Request request = new Request("GET", "/_cat/plugins");
            String plugins = EntityUtils.toString(restHighLevelClient.getLowLevelClient()
                    .performRequest(request)
                    .getEntity());
            return plugins.contains(pluginName);
        } catch (Exception e) {
            return false;
        }
    }

    /// 创建索引库结构与映射关系
    private void createHotelIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(HOTEL_INDEX);
        request.source("""
                {
                  "settings": {
                    "analysis": {
                      "analyzer": {
                        "hotel_pinyin": {
                          "tokenizer": "hotel_pinyin_tokenizer"
                        },
                        "hotel_pinyin_search": {
                          "tokenizer": "keyword",
                          "filter": [ "lowercase" ]
                        }
                      },
                      "tokenizer": {
                        "hotel_pinyin_tokenizer": {
                          "type": "pinyin",
                          "keep_first_letter": true,
                          "keep_separate_first_letter": false,
                          "keep_full_pinyin": true,
                          "keep_joined_full_pinyin": true,
                          "keep_original": true,
                          "keep_none_chinese": true,
                          "keep_none_chinese_together": true,
                          "none_chinese_pinyin_tokenize": false,
                          "lowercase": true,
                          "remove_duplicated_term": true
                        }
                      }
                    }
                  },
                  "mappings": {
                    "_meta": {
                      "mappingVersion": 5,
                      "indexAnalyzer": "ik_max_word",
                      "searchAnalyzer": "ik_smart",
                      "pinyinAnalyzer": "hotel_pinyin",
                      "pinyinSearchAnalyzer": "hotel_pinyin_search"
                    },
                    "properties": {
                      "id": { "type": "long" },
                      "name": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 },
                          "pinyin": { "type": "text", "analyzer": "hotel_pinyin", "search_analyzer": "hotel_pinyin_search" }
                        }
                      },
                      "address": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart",
                        "fields": {
                          "pinyin": { "type": "text", "analyzer": "hotel_pinyin", "search_analyzer": "hotel_pinyin_search" }
                        }
                      },
                      "city": {
                        "type": "keyword",
                        "fields": {
                          "text": {
                            "type": "text",
                            "analyzer": "ik_max_word",
                            "search_analyzer": "ik_smart"
                          },
                          "pinyin": { "type": "text", "analyzer": "hotel_pinyin", "search_analyzer": "hotel_pinyin_search" }
                        }
                      },
                      "brand": {
                        "type": "keyword",
                        "fields": {
                          "text": {
                            "type": "text",
                            "analyzer": "ik_max_word",
                            "search_analyzer": "ik_smart"
                          },
                          "pinyin": { "type": "text", "analyzer": "hotel_pinyin", "search_analyzer": "hotel_pinyin_search" }
                        }
                      },
                      "starName": {
                        "type": "keyword",
                        "fields": {
                          "text": {
                            "type": "text",
                            "analyzer": "ik_max_word",
                            "search_analyzer": "ik_smart"
                          },
                          "pinyin": { "type": "text", "analyzer": "hotel_pinyin", "search_analyzer": "hotel_pinyin_search" }
                        }
                      },
                      "business": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart",
                        "fields": {
                          "keyword": { "type": "keyword", "ignore_above": 256 },
                          "pinyin": { "type": "text", "analyzer": "hotel_pinyin", "search_analyzer": "hotel_pinyin_search" }
                        }
                      },
                      "description": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart",
                        "fields": {
                          "pinyin": { "type": "text", "analyzer": "hotel_pinyin", "search_analyzer": "hotel_pinyin_search" }
                        }
                      },
                      "pic": { "type": "keyword", "index": false },
                      "price": { "type": "double" },
                      "score": { "type": "integer" },
                      "all": {
                        "type": "text",
                        "analyzer": "ik_max_word",
                        "search_analyzer": "ik_smart",
                        "fields": {
                          "pinyin": { "type": "text", "analyzer": "hotel_pinyin", "search_analyzer": "hotel_pinyin_search" }
                        }
                      }
                    }
                  }
                }
                """, XContentType.JSON);
        restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
    }

    // 创建文档写入请求  -- 创建一条 ES 写入请求
    private IndexRequest toIndexRequest(TravelProduct product) {
        return new IndexRequest(HOTEL_INDEX)  //表示写入到 hotel 索引
                .id(String.valueOf(product.getId()))
                .opType(DocWriteRequest.OpType.INDEX)  // 表示执行 index 操作：有就覆盖，没有就新增
                .source(toHotelDocument(product), XContentType.JSON);  // 文档
    }

    // 数据文档
    private Map<String, Object> toHotelDocument(TravelProduct product) {
        String address = extractBetween(product.getDescription(), "地址：", "；");
        String business = extractBetween(product.getDescription(), "商圈：", "；");
        Integer score = parseScore(extractAfter(product.getDescription(), "评分："));
        String brand = firstTagPart(product.getTag());
        String starName = secondTagPart(product.getTag());

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", product.getId());
        doc.put("name", product.getName());
        doc.put("address", address);
        doc.put("city", product.getDestination());
        doc.put("brand", brand);
        doc.put("starName", starName);
        doc.put("business", business);
        doc.put("description", product.getDescription());
        doc.put("pic", product.getCoverUrl());
        doc.put("price", toDouble(product.getPrice()));
        doc.put("score", score);
        doc.put("all", String.join(" ",
                nullToEmpty(product.getName()),
                nullToEmpty(product.getDestination()),
                nullToEmpty(brand),
                nullToEmpty(starName),
                nullToEmpty(address),
                nullToEmpty(business),
                nullToEmpty(product.getDescription())));
        return doc;
    }

    // 提取字符串中间值
    private String extractBetween(String text, String prefix, String suffix) {
        String value = extractAfter(text, prefix);  // 从 text 中提取 value
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int end = value.indexOf(suffix);
        return end >= 0 ? value.substring(0, end) : value;
    }

    // 提取字符串后缀
    private String extractAfter(String text, String prefix) {
        if (!StringUtils.hasText(text) || !text.contains(prefix)) {
            return null;
        }
        return text.substring(text.indexOf(prefix) + prefix.length()).trim();
    }

    // 提取字符串前缀
    private String firstTagPart(String tag) {
        if (!StringUtils.hasText(tag)) {
            return null;
        }
        return tag.split("/", 2)[0].trim();
    }

    // 提取字符串后缀
    private String secondTagPart(String tag) {
        if (!StringUtils.hasText(tag) || !tag.contains("/")) {
            return null;
        }
        return tag.split("/", 2)[1].trim();
    }

    // 解析评分
    private Integer parseScore(String scoreText) {
        if (!StringUtils.hasText(scoreText)) {
            return null;
        }
        try {
            return Integer.parseInt(scoreText.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // BigDecimal 转 Double
    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    // null 转 ""
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
