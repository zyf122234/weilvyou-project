package com.travel.product.service.impl;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.utils.PageUtils;
import com.travel.product.service.HomeHotelCacheService;
import com.travel.product.service.HotelSearchService;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HotelSearchServiceImpl implements HotelSearchService {

    private static final String HOTEL_INDEX = "hotel";
    private static final List<String> PRICE_RANGES = List.of("100元以下", "100-300元", "300-600元", "600-1500元", "1500元以上");

    private final RestHighLevelClient restHighLevelClient;
    private final HomeHotelCacheService homeHotelCacheService;

    // 搜索ES请求构建
    @Override
    public HotelSearchResult search(String keyword,
                                    String city,
                                    String starName,
                                    String brand,
                                    String priceRange,
                                    Long current,
                                    Long size,
                                    Boolean all) {
        long safeCurrent = PageUtils.safeCurrent(current);
        long safeSize = PageUtils.safeSizeOrDefault(size, 20);
        boolean homeAllRequest = homeHotelCacheService.isHomeAllRequest(keyword, city, starName, brand, priceRange, current, all);
        if (homeAllRequest) {
            HotelSearchResult cachedResult = homeHotelCacheService.get();
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        // 创建查询
        SearchRequest request = new SearchRequest(HOTEL_INDEX);
        // 设置查询条件
        request.source(buildSearchSource(keyword, city, starName, brand, priceRange, safeCurrent, safeSize));

        try {
            if (Boolean.TRUE.equals(all)) {
                SearchRequest countRequest = new SearchRequest(HOTEL_INDEX);
                countRequest.source(buildSearchSource(keyword, city, starName, brand, priceRange, 1, 0));
                SearchResponse countResponse = restHighLevelClient.search(countRequest, RequestOptions.DEFAULT);
                long total = readTotal(countResponse);
                if (total == 0) {
                    return parseResult(countResponse, 1, 0);
                }

                SearchRequest allRequest = new SearchRequest(HOTEL_INDEX);
                allRequest.source(buildSearchSource(keyword, city, starName, brand, priceRange, 1, total));
                SearchResponse allResponse = restHighLevelClient.search(allRequest, RequestOptions.DEFAULT);
                HotelSearchResult result = parseResult(allResponse, 1, total);
                if (homeAllRequest) {
                    homeHotelCacheService.put(result);
                }
                return result;
            }
            // 执行查询
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

            return parseResult(response, safeCurrent, safeSize);
        } catch (IOException e) {
            throw new BusinessException("酒店搜索服务不可用，请确认 ES 的 hotel 索引已启动");
        }
    }

    // 构建查询条件
    private SearchSourceBuilder buildSearchSource(String keyword,
                                                  String city,
                                                  String starName,
                                                  String brand,
                                                  String priceRange,
                                                  long current,
                                                  long size) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();  // 创建bool查询
        if (StringUtils.hasText(keyword)) {
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword,
                            "name",
                            "city.text",
                            "brand.text",
                            "starName.text",
                            "name.pinyin",
                            "city.pinyin",
                            "brand.pinyin",
                            "starName.pinyin")
                    .field("name", 3.0f)
                    .field("brand.text", 2.0f)
                    .field("city.text", 2.0f)
                    .field("starName.text", 2.0f)
                    .field("name.pinyin", 1.8f)
                    .field("brand.pinyin", 1.5f)
                    .field("city.pinyin", 1.5f));  // 设置匹配字段
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        addTerm(boolQuery, "city", city);
        addTerm(boolQuery, "starName", starName);
        addTerm(boolQuery, "brand", brand);
        addPriceRange(boolQuery, priceRange);

        SearchSourceBuilder source = new SearchSourceBuilder()
                .query(boolQuery)
                .from((int) ((current - 1) * size))
                .size(toEsSize(size))
                .trackTotalHits(true)
                .sort("score", SortOrder.DESC)
                .aggregation(AggregationBuilders.terms("cities").field("city").size(20))
                .aggregation(AggregationBuilders.terms("starNames").field("starName").size(20))
                .aggregation(AggregationBuilders.terms("brands").field("brand").size(30))
                .aggregation(AggregationBuilders.range("prices")
                        .field("price")
                        .addUnboundedTo("100元以下", 100)
                        .addRange("100-300元", 100, 300)
                        .addRange("300-600元", 300, 600)
                        .addRange("600-1500元", 600, 1500)
                        .addUnboundedFrom("1500元以上", 1500));
        return source;
    }

    // 添加term条件
    private void addTerm(BoolQueryBuilder boolQuery, String field, String value) {
        if (StringUtils.hasText(value)) {
            boolQuery.filter(QueryBuilders.termQuery(field, value));
        }
    }

    private void addPriceRange(BoolQueryBuilder boolQuery, String priceRange) {
        if (!StringUtils.hasText(priceRange)) {
            return;
        }
        switch (priceRange) {
            case "100元以下" -> boolQuery.filter(QueryBuilders.rangeQuery("price").lt(100));
            case "100-300元" -> boolQuery.filter(QueryBuilders.rangeQuery("price").gte(100).lt(300));
            case "300-600元" -> boolQuery.filter(QueryBuilders.rangeQuery("price").gte(300).lt(600));
            case "600-1500元" -> boolQuery.filter(QueryBuilders.rangeQuery("price").gte(600).lt(1500));
            case "1500元以上" -> boolQuery.filter(QueryBuilders.rangeQuery("price").gte(1500));
            default -> {
            }
        }
    }

    //解析结果
    private HotelSearchResult parseResult(SearchResponse response, long current, long size) {
        long total = readTotal(response);
        List<HotelVO> hotels = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            hotels.add(toHotelVO(hit.getSourceAsMap(), hit.getId()));
        }

        HotelSearchResult result = new HotelSearchResult();
        result.setPage(PageResult.of(total, total == 0 ? 0 : (long) Math.ceil((double) total / size), current, size, hotels));
        result.setFacets(fillFacetFallbacks(parseFacets(response), hotels));
        return result;
    }

    private long readTotal(SearchResponse response) {
        return response.getHits().getTotalHits() == null ? 0 : response.getHits().getTotalHits().value;
    }

    private int toEsSize(long size) {
        if (size > Integer.MAX_VALUE) {
            throw new BusinessException("Too many hotels to load at once");
        }
        return (int) size;
    }

    private HotelFacets parseFacets(SearchResponse response) {
        HotelFacets facets = new HotelFacets();
        if (response.getAggregations() == null) {
            facets.setCities(new ArrayList<>());
            facets.setStarNames(new ArrayList<>());
            facets.setBrands(new ArrayList<>());
            facets.setPriceRanges(PRICE_RANGES);
            return facets;
        }
        facets.setCities(readTerms(response.getAggregations().get("cities")));
        facets.setStarNames(readTerms(response.getAggregations().get("starNames")));
        facets.setBrands(readTerms(response.getAggregations().get("brands")));
        List<String> prices = readRanges(response.getAggregations().get("prices"));
        facets.setPriceRanges(prices.isEmpty() ? PRICE_RANGES : prices);
        return facets;
    }

    private HotelFacets fillFacetFallbacks(HotelFacets facets, List<HotelVO> hotels) {
        if (facets.getCities() == null || facets.getCities().isEmpty()) {
            facets.setCities(distinctValues(hotels, HotelVO::getCity, 20));
        }
        if (facets.getStarNames() == null || facets.getStarNames().isEmpty()) {
            facets.setStarNames(distinctValues(hotels, HotelVO::getStarName, 20));
        }
        if (facets.getBrands() == null || facets.getBrands().isEmpty()) {
            facets.setBrands(distinctValues(hotels, HotelVO::getBrand, 30));
        }
        if (facets.getPriceRanges() == null || facets.getPriceRanges().isEmpty()) {
            facets.setPriceRanges(PRICE_RANGES);
        }
        return facets;
    }

    private List<String> distinctValues(List<HotelVO> hotels,
                                        java.util.function.Function<HotelVO, String> getter,
                                        int limit) {
        List<String> values = new ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (HotelVO hotel : hotels) {
            String value = getter.apply(hotel);
            if (StringUtils.hasText(value) && seen.add(value)) {
                values.add(value);
                if (values.size() >= limit) {
                    break;
                }
            }
        }
        return values;
    }

    private List<String> readTerms(Terms terms) {
        List<String> values = new ArrayList<>();
        if (terms == null) {
            return values;
        }
        for (Terms.Bucket bucket : terms.getBuckets()) {
            if (bucket.getDocCount() > 0 && StringUtils.hasText(bucket.getKeyAsString())) {
                values.add(bucket.getKeyAsString());
            }
        }
        return values;
    }

    private List<String> readRanges(Range range) {
        List<String> values = new ArrayList<>();
        if (range == null) {
            return values;
        }
        for (Range.Bucket bucket : range.getBuckets()) {
            if (bucket.getDocCount() > 0 && StringUtils.hasText(bucket.getKeyAsString())) {
                values.add(bucket.getKeyAsString());
            }
        }
        return values;
    }

    private HotelVO toHotelVO(Map<String, Object> source, String id) {
        HotelVO vo = new HotelVO();
        vo.setId(asLong(source.get("id"), id));
        vo.setName(asString(source.get("name")));
        vo.setAddress(asString(source.get("address")));
        vo.setCity(asString(source.get("city")));
        vo.setBrand(asString(source.get("brand")));
        vo.setStarName(asString(source.get("starName")));
        vo.setBusiness(asString(source.get("business")));
        vo.setPic(asString(source.get("pic")));
        vo.setPrice(asBigDecimal(source.get("price")));
        vo.setScore(asInteger(source.get("score")));
        return vo;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value, String fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return StringUtils.hasText(asString(value)) ? Long.parseLong(asString(value)) : Long.parseLong(fallback);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return StringUtils.hasText(asString(value)) ? new BigDecimal(asString(value)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return StringUtils.hasText(asString(value)) ? Integer.parseInt(asString(value)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
