package com.supermarket.inventory.product.service;

import com.supermarket.inventory.category.entity.Category;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.category.mapper.CategoryMapper;
import com.supermarket.inventory.product.dto.ProductRequest;
import com.supermarket.inventory.product.entity.Product;
import com.supermarket.inventory.product.mapper.ProductMapper;
import com.supermarket.inventory.product.vo.ProductVO;
import com.supermarket.inventory.sku.mapper.SkuUsageMapper;
import com.supermarket.inventory.sku.service.SkuService;
import com.supermarket.inventory.sku.vo.SkuVO;
import com.supermarket.inventory.stock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StockService stockService;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SkuService skuService;

    @Mock
    private SkuUsageMapper skuUsageMapper;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productMapper, stockService, categoryMapper, skuService, skuUsageMapper);
    }

    @Test
    void create_insertsProductWithoutPricesAndCreatesDefaultSku() {
        ProductRequest request = productRequest("P001", "可乐", 3L, "2.00", "3.50", 1);
        SkuVO defaultSku = defaultSku();

        when(productMapper.findByCode("P001")).thenReturn(Optional.empty());
        when(productMapper.insert(any(Product.class))).thenReturn(7L);
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001", "可乐", 3L, 1)));
        when(categoryMapper.findById(3L)).thenReturn(Optional.of(category(3L, "饮料")));
        when(skuService.createDefaultForProduct(
                any(Product.class),
                eq(new BigDecimal("2.00")),
                eq(new BigDecimal("3.50"))
        )).thenReturn(defaultSku);
        when(skuService.listByProductId(7L)).thenReturn(List.of(defaultSku));

        ProductVO vo = productService.create(request);

        assertThat(vo.getSkus()).hasSize(1);
        verify(stockService).initializeStock(7L);
        verify(skuService).createDefaultForProduct(
                any(Product.class),
                eq(new BigDecimal("2.00")),
                eq(new BigDecimal("3.50"))
        );

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        Product inserted = productCaptor.getValue();
        assertThat(inserted.getProductCode()).isEqualTo("P001");
        assertThat(inserted.getProductName()).isEqualTo("可乐");
        assertThat(inserted.getCategoryId()).isEqualTo(3L);
        assertThat(inserted.getStatus()).isEqualTo(1);
    }

    @Test
    void update_doesNotPassPricesToProductEntity() {
        Product existing = product(7L, "P001", "旧名称", 3L, 1);
        Product updated = product(7L, "P001", "新名称", 4L, 0);
        ProductRequest request = productRequest("P001", "新名称", 4L, "9.99", "19.99", 0);

        when(productMapper.findById(7L)).thenReturn(Optional.of(existing), Optional.of(updated));
        when(categoryMapper.findById(4L)).thenReturn(Optional.of(category(4L, "饮料")));
        when(skuService.listByProductId(7L)).thenReturn(List.of());

        productService.update(7L, request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).update(productCaptor.capture());
        Product product = productCaptor.getValue();
        assertThat(product.getId()).isEqualTo(7L);
        assertThat(product.getProductCode()).isEqualTo("P001");
        assertThat(product.getProductName()).isEqualTo("新名称");
        assertThat(product.getCategoryId()).isEqualTo(4L);
        assertThat(product.getStatus()).isEqualTo(0);
    }

    @Test
    void delete_rejectsProductWithBusinessReferences() {
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001", "可乐", 3L, 1)));
        when(skuUsageMapper.countBusinessReferencesByProductId(7L)).thenReturn(2L);

        assertThatThrownBy(() -> productService.delete(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品已有业务记录");

        verify(skuService, never()).deleteAllByProductId(7L);
        verify(stockService, never()).deleteStockByProductId(7L);
        verify(productMapper, never()).delete(7L);
    }

    private ProductRequest productRequest(
            String productCode,
            String productName,
            Long categoryId,
            String purchasePrice,
            String salePrice,
            Integer status
    ) {
        ProductRequest request = new ProductRequest();
        request.setProductCode(productCode);
        request.setProductName(productName);
        request.setCategoryId(categoryId);
        request.setPurchasePrice(new BigDecimal(purchasePrice));
        request.setSalePrice(new BigDecimal(salePrice));
        request.setStatus(status);
        return request;
    }

    private Product product(Long id, String productCode, String productName, Long categoryId, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setProductCode(productCode);
        product.setProductName(productName);
        product.setCategoryId(categoryId);
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 5, 31, 0, 0));
        return product;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private SkuVO defaultSku() {
        SkuVO sku = new SkuVO();
        sku.setId(10L);
        sku.setProductId(7L);
        sku.setSkuCode("P001-001");
        sku.setSkuName("可乐");
        sku.setSpec("默认规格");
        sku.setBaseUnit("个");
        sku.setPurchasePrice(new BigDecimal("2.00"));
        sku.setSalePrice(new BigDecimal("3.50"));
        sku.setStatus(1);
        sku.setIsDefault(1);
        return sku;
    }
}
