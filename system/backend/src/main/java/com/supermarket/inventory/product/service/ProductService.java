package com.supermarket.inventory.product.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.product.dto.ProductRequest;
import com.supermarket.inventory.product.entity.Product;
import com.supermarket.inventory.category.mapper.CategoryMapper;
import com.supermarket.inventory.product.mapper.ProductMapper;
import com.supermarket.inventory.product.vo.ProductVO;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final StockService stockService;
    private final CategoryMapper categoryMapper;

    public ProductService(ProductMapper productMapper, StockService stockService, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.stockService = stockService;
        this.categoryMapper = categoryMapper;
    }

    public PageResult<ProductVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        long total = productMapper.count(keyword);
        return new PageResult<>(
                productMapper.findPage(keyword, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize)
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
        validatePrice(request);
        productMapper.findByCode(request.getProductCode()).ifPresent(product -> {
            throw new BusinessException("商品编号已存在");
        });
        Product product = fromRequest(request);
        Long productId = productMapper.insert(product);
        stockService.initializeStock(productId);
        return toVO(productMapper.findById(productId).orElseThrow(() -> new BusinessException("商品创建失败")));
    }

    @Transactional
    public ProductVO update(Long id, ProductRequest request) {
        validatePrice(request);
        Product product = productMapper.findById(id).orElseThrow(() -> new BusinessException(404, "商品不存在"));
        product.setProductName(request.getProductName());
        product.setCategoryId(request.getCategoryId());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setStatus(normalizeStatus(request.getStatus()));
        productMapper.update(product);
        return toVO(productMapper.findById(id).orElseThrow(() -> new BusinessException(404, "商品不存在")));
    }

    @Transactional
    public void delete(Long id) {
        productMapper.findById(id).orElseThrow(() -> new BusinessException(404, "商品不存在"));
        stockService.deleteStockByProductId(id);
        productMapper.delete(id);
    }

    private Product fromRequest(ProductRequest request) {
        Product product = new Product();
        product.setProductCode(request.getProductCode().trim());
        product.setProductName(request.getProductName());
        product.setCategoryId(request.getCategoryId());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSalePrice(request.getSalePrice());
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
        vo.setPurchasePrice(product.getPurchasePrice());
        vo.setSalePrice(product.getSalePrice());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private void validatePrice(ProductRequest request) {
        if (request.getSalePrice().compareTo(request.getPurchasePrice()) < 0) {
            throw new BusinessException("商品售价不能低于进价");
        }
    }

    private int normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }
}
