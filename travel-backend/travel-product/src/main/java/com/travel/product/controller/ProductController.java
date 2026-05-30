package com.travel.product.controller;

import com.travel.api.product.ProductDTO;
import com.travel.api.product.ProductVO;
import com.travel.common.result.PageResult;
import com.travel.common.result.R;
import com.travel.common.security.LoginUser;
import com.travel.product.service.HotelSearchService;
import com.travel.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final HotelSearchService hotelSearchService;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    @Value("${travel.upload.product-dir}")
    private String productImageDir;

    @GetMapping("/published")
    public R<PageResult<ProductVO>> listPublished(@RequestParam(name = "current", defaultValue = "1") Long current,
                                                  @RequestParam(name = "size", defaultValue = "10") Long size,
                                                  @RequestParam(name = "keyword", required = false) String keyword) {
        return R.ok(productService.listPublished(current, size, keyword));
    }

    @GetMapping("/published/{id}")
    public R<ProductVO> getPublishedDetail(@PathVariable("id") Long id) {
        return R.ok(productService.getPublishedDetail(id));
    }

    @GetMapping("/hotel/search")
    public R<HotelSearchService.HotelSearchResult> searchHotels(@RequestParam(name = "keyword", required = false) String keyword,
                                                                @RequestParam(name = "city", required = false) String city,
                                                                @RequestParam(name = "starName", required = false) String starName,
                                                                @RequestParam(name = "brand", required = false) String brand,
                                                                @RequestParam(name = "priceRange", required = false) String priceRange,
                                                                @RequestParam(name = "current", defaultValue = "1") Long current,
                                                                @RequestParam(name = "size", defaultValue = "100") Long size,
                                                                @RequestParam(name = "all", defaultValue = "false") Boolean all) {
        return R.ok(hotelSearchService.search(keyword, city, starName, brand, priceRange, current, size, all));
    }

    @GetMapping("/merchant")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public R<PageResult<ProductVO>> listMerchantProducts(@AuthenticationPrincipal LoginUser loginUser,
                                                         @RequestParam(name = "current", defaultValue = "1") Long current,
                                                         @RequestParam(name = "size", defaultValue = "10") Long size,
                                                         @RequestParam(name = "keyword", required = false) String keyword) {
        return R.ok(productService.listMerchantProducts(loginUser.userId(), current, size, keyword));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<ProductVO>> listAdminProducts(@AuthenticationPrincipal LoginUser loginUser,
                                                      @RequestParam(name = "current", defaultValue = "1") Long current,
                                                      @RequestParam(name = "size", defaultValue = "10") Long size,
                                                      @RequestParam(name = "keyword", required = false) String keyword,
                                                      @RequestParam(name = "merchantName", required = false) String merchantName) {
        return R.ok(productService.listAdminProducts(loginUser.userId(), current, size, keyword, merchantName));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public R<ProductVO> createProduct(@AuthenticationPrincipal LoginUser loginUser,
                                      @RequestBody @Valid ProductDTO dto) {
        return R.ok(productService.createProduct(loginUser.userId(), dto));
    }

    // 上传产品封面
    @PostMapping("/cover")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public R<String> uploadProductCover(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return R.fail(400, "请选择封面图片");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            return R.fail(400, "仅支持 JPG、PNG、WebP、GIF 格式图片");
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (!StringUtils.hasText(extension)) {
            extension = switch (file.getContentType()) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/gif" -> "gif";
                default -> "jpg";
            };
        }

        try {
            // 获取图片文件绝对路径
            Path imagePath = Paths.get(productImageDir).toAbsolutePath().normalize();
            Files.createDirectories(imagePath); //如果该文件夹不存在就创建
            String filename = UUID.randomUUID() + "." + extension.toLowerCase();
            // resolve = 解析、拼接路径 , 把 目录路径 + 文件名 拼接成完整路径
            Path target = imagePath.resolve(filename).normalize();  //获取图片文件绝对路径
            file.transferTo(target); //保存图片文件地址
            return R.ok("/api/uploads/products/" + filename);
        } catch (IOException e) {
            return R.fail(500, "封面上传失败");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public R<ProductVO> updateProduct(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable("id") Long id,
                                      @RequestBody @Valid ProductDTO dto) {
        return R.ok(productService.updateProduct(loginUser.userId(), id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public R<ProductVO> updateStatus(@AuthenticationPrincipal LoginUser loginUser,
                                     @PathVariable("id") Long id,
                                     @RequestParam("status") Integer status) {
        return R.ok(productService.updateStatus(loginUser.userId(), id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    public R<Void> deleteProduct(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable("id") Long id) {
        productService.deleteProduct(loginUser.userId(), id);
        return R.ok();
    }
}
