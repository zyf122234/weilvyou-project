package com.travel.product.service;

import com.travel.common.result.PageResult;

import java.math.BigDecimal;
import java.util.List;

public interface HotelSearchService {

    HotelSearchResult search(String keyword,
                             String city,
                             String starName,
                             String brand,
                             String priceRange,
                             Long current,
                             Long size,
                             Boolean all);

    class HotelSearchResult {
        private PageResult<HotelVO> page;
        private HotelFacets facets;

        public PageResult<HotelVO> getPage() {
            return page;
        }

        public void setPage(PageResult<HotelVO> page) {
            this.page = page;
        }

        public HotelFacets getFacets() {
            return facets;
        }

        public void setFacets(HotelFacets facets) {
            this.facets = facets;
        }
    }

    class HotelFacets {
        private List<String> cities;
        private List<String> starNames;
        private List<String> brands;
        private List<String> priceRanges;

        public List<String> getCities() {
            return cities;
        }

        public void setCities(List<String> cities) {
            this.cities = cities;
        }

        public List<String> getStarNames() {
            return starNames;
        }

        public void setStarNames(List<String> starNames) {
            this.starNames = starNames;
        }

        public List<String> getBrands() {
            return brands;
        }

        public void setBrands(List<String> brands) {
            this.brands = brands;
        }

        public List<String> getPriceRanges() {
            return priceRanges;
        }

        public void setPriceRanges(List<String> priceRanges) {
            this.priceRanges = priceRanges;
        }
    }

    class HotelVO {
        private Long id;
        private String name;
        private String address;
        private String city;
        private String brand;
        private String starName;
        private String business;
        private String pic;
        private BigDecimal price;
        private Integer score;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }

        public String getStarName() {
            return starName;
        }

        public void setStarName(String starName) {
            this.starName = starName;
        }

        public String getBusiness() {
            return business;
        }

        public void setBusiness(String business) {
            this.business = business;
        }

        public String getPic() {
            return pic;
        }

        public void setPic(String pic) {
            this.pic = pic;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }
}
