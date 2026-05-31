package com.supermarket.inventory.product.service;

import com.supermarket.inventory.category.entity.Category;
import com.supermarket.inventory.category.mapper.CategoryMapper;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.product.dto.ProductRequest;
import com.supermarket.inventory.product.entity.Product;
import com.supermarket.inventory.product.mapper.ProductMapper;
import com.supermarket.inventory.product.vo.ProductVO;
import com.supermarket.inventory.sku.mapper.SkuUsageMapper;
import com.supermarket.inventory.sku.service.SkuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SkuService skuService;

    @Mock
    private SkuUsageMapper skuUsageMapper;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productMapper, categoryMapper, skuService, skuUsageMapper);
    }

    @Test
    void create_insertsProductWithoutCreatingSku() {
        ProductRequest request = productRequest("P001", "Cola", 3L, 1);

        when(productMapper.findByCode("P001")).thenReturn(Optional.empty());
        when(productMapper.insert(any(Product.class))).thenReturn(7L);
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001", "Cola", 3L, 1)));
        when(categoryMapper.findById(3L)).thenReturn(Optional.of(category(3L, "Drink")));
        when(skuService.listByProductId(7L)).thenReturn(List.of());

        ProductVO vo = productService.create(request);

        assertThat(vo.getSkus()).isEmpty();
        verify(skuService, only()).listByProductId(7L);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        Product inserted = productCaptor.getValue();
        assertThat(inserted.getProductCode()).isEqualTo("P001");
        assertThat(inserted.getProductName()).isEqualTo("Cola");
        assertThat(inserted.getCategoryId()).isEqualTo(3L);
        assertThat(inserted.getStatus()).isEqualTo(1);
    }

    @Test
    void update_doesNotPassPricesToProductEntity() {
        Product existing = product(7L, "P001", "Old name", 3L, 1);
        Product updated = product(7L, "P001", "New name", 4L, 0);
        ProductRequest request = productRequest("P001", "New name", 4L, 0);

        when(productMapper.findById(7L)).thenReturn(Optional.of(existing), Optional.of(updated));
        when(categoryMapper.findById(4L)).thenReturn(Optional.of(category(4L, "Drink")));
        when(skuService.listByProductId(7L)).thenReturn(List.of());

        productService.update(7L, request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).update(productCaptor.capture());
        Product product = productCaptor.getValue();
        assertThat(product.getId()).isEqualTo(7L);
        assertThat(product.getProductCode()).isEqualTo("P001");
        assertThat(product.getProductName()).isEqualTo("New name");
        assertThat(product.getCategoryId()).isEqualTo(4L);
        assertThat(product.getStatus()).isEqualTo(0);
    }

    @Test
    void delete_rejectsProductWithBusinessReferences() {
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001", "Cola", 3L, 1)));
        when(skuUsageMapper.countBusinessReferencesByProductId(7L)).thenReturn(2L);

        assertThatThrownBy(() -> productService.delete(7L))
                .isInstanceOf(BusinessException.class);

        verify(skuService, never()).deleteAllByProductId(7L);
        verify(productMapper, never()).delete(7L);
    }

    @Test
    void delete_deletesSkuRecordsWithoutProductStockDelete() {
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001", "Cola", 3L, 1)));
        when(skuUsageMapper.countBusinessReferencesByProductId(7L)).thenReturn(0L);

        productService.delete(7L);

        verify(skuService).deleteAllByProductId(7L);
        verify(productMapper).delete(7L);
    }

    private ProductRequest productRequest(
            String productCode,
            String productName,
            Long categoryId,
            Integer status
    ) {
        ProductRequest request = new ProductRequest();
        request.setProductCode(productCode);
        request.setProductName(productName);
        request.setCategoryId(categoryId);
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

}
