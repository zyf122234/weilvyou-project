package com.travel.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.api.product.ProductDTO;
import com.travel.api.product.ProductVO;
import com.travel.api.user.UserFeignClient;
import com.travel.api.user.UserVO;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.result.R;
import com.travel.common.utils.PageUtils;
import com.travel.product.entity.TravelProduct;
import com.travel.product.mapper.TravelProductMapper;
import com.travel.product.service.HomeHotelCacheService;
import com.travel.product.service.HotelIndexService;
import com.travel.product.service.ProductService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final TravelProductMapper productMapper;
    private final UserFeignClient userFeignClient;
    private final HotelIndexService hotelIndexService;
    private final HomeHotelCacheService homeHotelCacheService;

    @Override
    public PageResult<ProductVO> listPublished(Long current, Long size, String keyword) {
        List<Long> matchedMerchantIds = findMerchantIdsByName(keyword);
        LambdaQueryWrapper<TravelProduct> wrapper = new LambdaQueryWrapper<TravelProduct>()
                .eq(TravelProduct::getStatus, 1)
                .and(StringUtils.hasText(keyword), item -> item
                        .like(TravelProduct::getName, keyword)
                        .or()
                        .like(TravelProduct::getDestination, keyword)
                        .or()
                        .like(TravelProduct::getTag, keyword)
                        .or(!matchedMerchantIds.isEmpty())
                        .in(!matchedMerchantIds.isEmpty(), TravelProduct::getMerchantId, matchedMerchantIds))
                .orderByDesc(TravelProduct::getUpdateTime);
        return page(wrapper, current, size);
    }

    @Override
    public ProductVO getPublishedDetail(Long productId) {
        TravelProduct product = productMapper.selectOne(new LambdaQueryWrapper<TravelProduct>()
                .eq(TravelProduct::getId, productId)
                .eq(TravelProduct::getStatus, 1));
        if (product == null) {
            throw new BusinessException(404, "商品不存在或未发布");
        }
        return toVO(product);
    }

    @Override
    public PageResult<ProductVO> listMerchantProducts(Long merchantId, Long current, Long size, String keyword) {
        UserVO merchant = requireUser(merchantId);
        assertMerchantOrAdmin(merchant);

        LambdaQueryWrapper<TravelProduct> wrapper = buildProductSearchWrapper(keyword, null);
        wrapper.eq(TravelProduct::getMerchantId, merchantId);
        return page(wrapper, current, size);
    }

    // 商户查看所有产品
    @Override
    public PageResult<ProductVO> listAdminProducts(Long adminId, Long current, Long size, String keyword, String merchantName) {
        UserVO admin = requireUser(adminId);
        assertAdmin(admin);  // 需要管理员
        return page(buildProductSearchWrapper(keyword, merchantName), current, size);
    }

    // 创建新增商品
    @Override
    @Transactional
    public ProductVO createProduct(Long merchantId, ProductDTO dto) {
        UserVO merchant = requireUser(merchantId);
        assertMerchantOrAdmin(merchant);

        TravelProduct product = new TravelProduct();
        fillProduct(product, dto);
        product.setMerchantId(merchantId);
        product.setStatus(0);
        productMapper.insert(product);
        TravelProduct saved = productMapper.selectById(product.getId()); // 查询保存后的商品
        hotelIndexService.savePublishedHotel(saved); // 同步更新索引库中的文档
        homeHotelCacheService.evict();
        return toVO(saved);
    }

    // 更新商品
    @Override
    @Transactional
    public ProductVO updateProduct(Long merchantId, Long productId, ProductDTO dto) {
        assertProductOwnerOrAdmin(merchantId, productId);
        TravelProduct product = productMapper.selectById(productId);
        fillProduct(product, dto);
        productMapper.updateById(product);
        TravelProduct saved = productMapper.selectById(productId);
        hotelIndexService.savePublishedHotel(saved);
        homeHotelCacheService.evict();
        return toVO(saved);
    }

    // 更新商品状态
    @Override
    @Transactional  // 开启事务
    public ProductVO updateStatus(Long merchantId, Long productId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("商品状态只能是上架或下架");
        }
        // 校验商品所有者或管理员
        assertProductOwnerOrAdmin(merchantId, productId);
        TravelProduct product = productMapper.selectById(productId);
        product.setStatus(status);
        productMapper.updateById(product);
        TravelProduct saved = productMapper.selectById(productId);
        hotelIndexService.savePublishedHotel(saved);  // 同步更新索引库中的文档
        homeHotelCacheService.evict();
        return toVO(saved);
    }

    // 删除商品
    @Override
    @Transactional
    public void deleteProduct(Long merchantId, Long productId) {
        assertProductOwnerOrAdmin(merchantId, productId);  // 验证权限
        productMapper.deleteById(productId);
        hotelIndexService.deleteHotel(productId);
        homeHotelCacheService.evict();
    }

    private PageResult<ProductVO> page(LambdaQueryWrapper<TravelProduct> wrapper, Long current, Long size) {
        IPage<TravelProduct> page = productMapper.selectPage(new Page<>(PageUtils.safeCurrent(current), PageUtils.safeSize(size)), wrapper);
        Map<Long, UserVO> merchantMap = loadMerchantMap(page.getRecords());
        List<ProductVO> records = page.getRecords().stream()
                .map(product -> toVO(product, merchantMap))
                .toList();
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    private void fillProduct(TravelProduct product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setDestination(resolveCity(dto));
        product.setCoverUrl(dto.getCoverUrl());
        product.setTag(resolveTag(dto));
        product.setPrice(dto.getPrice());
    }

    // 权限校验
    private void assertProductOwnerOrAdmin(Long userId, Long productId) {
        UserVO user = requireUser(userId);
        // 商户权限校验
        assertMerchantOrAdmin(user);

        TravelProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getMerchantId() == null) {
            throw new BusinessException("商品未绑定商家");
        }
        if (!hasRole(user, "ROLE_ADMIN") && !userId.equals(product.getMerchantId())) {
            throw new AccessDeniedException("无权操作该商品");
        }
    }

    // 判断用户是否是管理员
    private void assertMerchantOrAdmin(UserVO user) {
        if (!hasRole(user, "ROLE_MERCHANT") && !hasRole(user, "ROLE_ADMIN")) {
            throw new AccessDeniedException("需要商家或管理员权限");
        }
    }

    // 获取用户
    private UserVO requireUser(Long userId) {
        R<UserVO> response;
        try {
            response = userFeignClient.getUserById(userId);
        } catch (FeignException.Forbidden e) {
            throw new AccessDeniedException("无法读取用户权限信息");
        } catch (FeignException e) {
            throw new BusinessException("用户服务不可用或用户不存在");
        }
        if (response == null || response.getCode() == null || response.getCode() != 200 || response.getData() == null) {
            throw new BusinessException("用户服务不可用或用户不存在");
        }
        return response.getData();
    }

    private boolean hasRole(UserVO user, String roleCode) {
        return user.getRoles() != null && user.getRoles().contains(roleCode);
    }

    private void assertAdmin(UserVO user) {
        if (!hasRole(user, "ROLE_ADMIN")) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }

    // 将商品数据转换成VO，传递给前端
    private ProductVO toVO(TravelProduct product) {
        return toVO(product, Map.of());
    }

    private ProductVO toVO(TravelProduct product, Map<Long, UserVO> merchantMap) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setDescription(product.getDescription());
        vo.setDestination(product.getDestination());
        vo.setCity(product.getDestination());
        vo.setCoverUrl(product.getCoverUrl());
        vo.setTag(product.getTag());
        vo.setBrand(firstTagPart(product.getTag()));
        vo.setStarName(secondTagPart(product.getTag()));
        vo.setPrice(product.getPrice());
        vo.setStatus(product.getStatus());
        vo.setMerchantId(product.getMerchantId());
        vo.setMerchantName(resolveMerchantName(product.getMerchantId(), merchantMap)); // 获取商户名称
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }

    // 构建查询条件
    private LambdaQueryWrapper<TravelProduct> buildProductSearchWrapper(String keyword, String merchantName) {
        List<Long> keywordMerchantIds = findMerchantIdsByName(keyword);
        List<Long> merchantNameIds = findMerchantIdsByName(merchantName);
        LambdaQueryWrapper<TravelProduct> wrapper = new LambdaQueryWrapper<TravelProduct>()
                .and(StringUtils.hasText(keyword), item -> item
                        .like(TravelProduct::getName, keyword)
                        .or()
                        .like(TravelProduct::getDestination, keyword)
                        .or()
                        .like(TravelProduct::getTag, keyword)
                        .or(!keywordMerchantIds.isEmpty())
                        .in(!keywordMerchantIds.isEmpty(), TravelProduct::getMerchantId, keywordMerchantIds));

        if (StringUtils.hasText(merchantName)) {
            if (merchantNameIds.isEmpty()) {
                wrapper.eq(TravelProduct::getMerchantId, -1L);
            } else {
                wrapper.in(TravelProduct::getMerchantId, merchantNameIds);
            }
        }
        return wrapper.orderByDesc(TravelProduct::getUpdateTime);
    }

    // 获取商户名称
    private String resolveMerchantName(Long merchantId, Map<Long, UserVO> merchantMap) {
        if (merchantId == null) {
            return null;
        }
        UserVO merchant = merchantMap.get(merchantId);
        if (merchant != null) {
            return StringUtils.hasText(merchant.getNickname()) ? merchant.getNickname() : merchant.getUsername();
        }
        try {
            R<UserVO> response = userFeignClient.getUserById(merchantId);
            if (response != null && response.getCode() != null && response.getCode() == 200 && response.getData() != null) {
                UserVO user = response.getData();
                return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    // 获取商户名称
    private List<Long> findMerchantIdsByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        try {
            R<List<UserVO>> response = userFeignClient.listMerchants(null, keyword, null);
            if (response != null && response.getCode() != null && response.getCode() == 200 && response.getData() != null) {
                return response.getData().stream()
                        .map(UserVO::getId)
                        .toList();
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    private Map<Long, UserVO> loadMerchantMap(List<TravelProduct> products) {
        Set<Long> merchantIds = products.stream()
                .map(TravelProduct::getMerchantId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (merchantIds.isEmpty()) {
            return Map.of();
        }
        try {
            R<List<UserVO>> response = userFeignClient.listMerchants(null, null, List.copyOf(merchantIds));
            if (response != null && response.getCode() != null && response.getCode() == 200 && response.getData() != null) {
                return response.getData().stream()
                        .collect(Collectors.toMap(UserVO::getId, Function.identity(), (left, right) -> left));
            }
        } catch (Exception ignored) {
            return Map.of();
        }
        return Map.of();
    }

    private String resolveCity(ProductDTO dto) {
        return StringUtils.hasText(dto.getCity()) ? dto.getCity() : dto.getDestination();
    }

    private String resolveTag(ProductDTO dto) {
        if (StringUtils.hasText(dto.getBrand()) || StringUtils.hasText(dto.getStarName())) {
            return String.join("/",
                    StringUtils.hasText(dto.getBrand()) ? dto.getBrand().trim() : "",
                    StringUtils.hasText(dto.getStarName()) ? dto.getStarName().trim() : "");
        }
        return dto.getTag();
    }

    private String firstTagPart(String tag) {
        if (!StringUtils.hasText(tag)) {
            return null;
        }
        return tag.split("/", 2)[0].trim();
    }

    private String secondTagPart(String tag) {
        if (!StringUtils.hasText(tag) || !tag.contains("/")) {
            return null;
        }
        return tag.split("/", 2)[1].trim();
    }
}
