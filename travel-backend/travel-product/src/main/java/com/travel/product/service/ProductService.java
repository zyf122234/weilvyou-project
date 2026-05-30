package com.travel.product.service;

import com.travel.api.product.ProductDTO;
import com.travel.api.product.ProductVO;
import com.travel.common.result.PageResult;

public interface ProductService {

    PageResult<ProductVO> listPublished(Long current, Long size, String keyword);

    ProductVO getPublishedDetail(Long productId);

    PageResult<ProductVO> listMerchantProducts(Long merchantId, Long current, Long size, String keyword);

    PageResult<ProductVO> listAdminProducts(Long adminId, Long current, Long size, String keyword, String merchantName);

    ProductVO createProduct(Long merchantId, ProductDTO dto);

    ProductVO updateProduct(Long merchantId, Long productId, ProductDTO dto);

    ProductVO updateStatus(Long merchantId, Long productId, Integer status);

    void deleteProduct(Long merchantId, Long productId);
}
