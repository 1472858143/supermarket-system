package com.supermarket.inventory.product.service;

import com.supermarket.inventory.brand.entity.Brand;
import com.supermarket.inventory.brand.service.BrandService;
import com.supermarket.inventory.category.mapper.CategoryMapper;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.product.dto.ProductRequest;
import com.supermarket.inventory.product.entity.Product;
import com.supermarket.inventory.product.mapper.ProductMapper;
import com.supermarket.inventory.product.vo.ProductVO;
import com.supermarket.inventory.sku.mapper.SkuUsageMapper;
import com.supermarket.inventory.sku.service.SkuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final BrandService brandService;
    private final SkuService skuService;
    private final SkuUsageMapper skuUsageMapper;

    public ProductService(
            ProductMapper productMapper,
            CategoryMapper categoryMapper,
            BrandService brandService,
            SkuService skuService,
            SkuUsageMapper skuUsageMapper
    ) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.brandService = brandService;
        this.skuService = skuService;
        this.skuUsageMapper = skuUsageMapper;
    }

    public PageResult<ProductVO> list(String keyword, Long brandId, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        long total = productMapper.count(keyword, brandId);
        return new PageResult<>(
                productMapper.findPage(keyword, brandId, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize)
                        .stream()
                        .map(this::toVO)
                        .toList(),
                total,
                normalizedPage,
                normalizedPageSize
        );
    }

    @Transactional
    public ProductVO create(ProductRequest request) {
        productMapper.findByCode(request.getProductCode()).ifPresent(product -> {
            throw new BusinessException("商品编号已存在");
        });
        Product product = fromRequest(request);
        Long productId = productMapper.insert(product);
        Product created = productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException("商品创建失败"));
        return toVO(created);
    }

    @Transactional
    public ProductVO update(Long id, ProductRequest request) {
        Product product = productMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        brandService.requireEnabledBrand(request.getBrandId());
        product.setProductName(request.getProductName());
        product.setCategoryId(request.getCategoryId());
        product.setBrandId(request.getBrandId());
        product.setStatus(normalizeStatus(request.getStatus()));
        productMapper.update(product);
        return toVO(productMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在")));
    }

    @Transactional
    public void delete(Long id) {
        productMapper.findById(id).orElseThrow(() -> new BusinessException(404, "商品不存在"));
        if (skuUsageMapper.countBusinessReferencesByProductId(id) > 0) {
            throw new BusinessException("商品已有业务记录，不能删除");
        }
        skuService.deleteAllByProductId(id);
        productMapper.delete(id);
    }

    private Product fromRequest(ProductRequest request) {
        Product product = new Product();
        product.setProductCode(request.getProductCode().trim());
        product.setProductName(request.getProductName());
        product.setCategoryId(request.getCategoryId());
        brandService.requireEnabledBrand(request.getBrandId());
        product.setBrandId(request.getBrandId());
        product.setStatus(normalizeStatus(request.getStatus()));
        return product;
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setProductCode(product.getProductCode());
        vo.setProductName(product.getProductName());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(
                categoryMapper.findById(product.getCategoryId())
                        .map(c -> c.getName())
                        .orElse("")
        );
        vo.setBrandId(product.getBrandId());
        Brand brand = brandService.getById(product.getBrandId());
        if (brand != null) {
            vo.setBrandCode(brand.getBrandCode());
            vo.setBrandName(brand.getBrandName());
        }
        vo.setSkus(skuService.listByProductId(product.getId()));
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private int normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }
}
