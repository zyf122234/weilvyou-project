package com.travel.product.service;

import com.travel.product.entity.TravelProduct;

public interface HotelIndexService {

    void rebuildPublishedHotels();

    void savePublishedHotel(TravelProduct product);

    void deleteHotel(Long productId);
}
