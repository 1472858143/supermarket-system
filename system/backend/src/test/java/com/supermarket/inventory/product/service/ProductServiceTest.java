package com.supermarket.inventory.product.service;

import com.supermarket.inventory.brand.entity.Brand;
import com.supermarket.inventory.brand.service.BrandService;
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
    private BrandService brandService;

    @Mock
    private SkuService skuService;

    @Mock
    private SkuUsageMapper skuUsageMapper;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productMapper, categoryMapper, brandService, skuService, skuUsageMapper);
    }

    @Test
    void create_insertsProductWithoutCreatingSku() {
        ProductRequest request = productRequest("P001", "Cola", 3L, 5L, 1);

        when(productMapper.findMaxCode("SPU%")).thenReturn(null);
        when(brandService.requireEnabledBrand(5L)).thenReturn(brand(5L, "BRD000005", "Coke", 1));
        when(productMapper.insert(any(Product.class))).thenReturn(7L);
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "SPU000001", "Cola", 3L, 5L, 1)));
        when(categoryMapper.findById(3L)).thenReturn(Optional.of(category(3L, "Drink")));
        when(brandService.getById(5L)).thenReturn(brand(5L, "BRD000005", "Coke", 1));
        when(skuService.listByProductId(7L)).thenReturn(List.of());

        ProductVO vo = productService.create(request);

        assertThat(vo.getProductCode()).isEqualTo("SPU000001");
        assertThat(vo.getSkus()).isEmpty();
        assertThat(vo.getBrandId()).isEqualTo(5L);
        assertThat(vo.getBrandCode()).isEqualTo("BRD000005");
        assertThat(vo.getBrandName()).isEqualTo("Coke");
        verify(skuService, only()).listByProductId(7L);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        Product inserted = productCaptor.getValue();
        assertThat(inserted.getProductCode()).isEqualTo("SPU000001");
        assertThat(inserted.getProductName()).isEqualTo("Cola");
        assertThat(inserted.getCategoryId()).isEqualTo(3L);
        assertThat(inserted.getBrandId()).isEqualTo(5L);
        assertThat(inserted.getStatus()).isEqualTo(1);
    }

    @Test
    void create_generatesNextSpuCode() {
        ProductRequest request = productRequest(null, "Cola", 3L, 5L, 1);

        when(productMapper.findMaxCode("SPU%")).thenReturn(null);
        when(brandService.requireEnabledBrand(5L)).thenReturn(brand(5L, "BRD000005", "Coke", 1));
        when(productMapper.insert(any(Product.class))).thenReturn(7L);
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "SPU000001", "Cola", 3L, 5L, 1)));
        when(categoryMapper.findById(3L)).thenReturn(Optional.of(category(3L, "Drink")));
        when(brandService.getById(5L)).thenReturn(brand(5L, "BRD000005", "Coke", 1));
        when(skuService.listByProductId(7L)).thenReturn(List.of());

        ProductVO vo = productService.create(request);

        assertThat(vo.getProductCode()).isEqualTo("SPU000001");
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        assertThat(productCaptor.getValue().getProductCode()).isEqualTo("SPU000001");
    }

    @Test
    void create_rejectsDisabledBrand() {
        ProductRequest request = productRequest("P001", "Cola", 3L, 5L, 1);

        when(brandService.requireEnabledBrand(5L)).thenThrow(new BusinessException("品牌已停用"));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("品牌已停用");

        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void update_updatesBrandIdAndReturnsBrandFields() {
        Product existing = product(7L, "P001", "Old name", 3L, 5L, 1);
        Product updated = product(7L, "P001", "New name", 4L, 6L, 0);
        ProductRequest request = productRequest("P001", "New name", 4L, 6L, 0);

        when(productMapper.findById(7L)).thenReturn(Optional.of(existing), Optional.of(updated));
        when(brandService.requireEnabledBrand(6L)).thenReturn(brand(6L, "BRD000006", "New Brand", 1));
        when(categoryMapper.findById(4L)).thenReturn(Optional.of(category(4L, "Drink")));
        when(brandService.getById(6L)).thenReturn(brand(6L, "BRD000006", "New Brand", 1));
        when(skuService.listByProductId(7L)).thenReturn(List.of());

        ProductVO vo = productService.update(7L, request);

        assertThat(vo.getBrandId()).isEqualTo(6L);
        assertThat(vo.getBrandCode()).isEqualTo("BRD000006");
        assertThat(vo.getBrandName()).isEqualTo("New Brand");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).update(productCaptor.capture());
        Product product = productCaptor.getValue();
        assertThat(product.getId()).isEqualTo(7L);
        assertThat(product.getProductCode()).isEqualTo("P001");
        assertThat(product.getProductName()).isEqualTo("New name");
        assertThat(product.getCategoryId()).isEqualTo(4L);
        assertThat(product.getBrandId()).isEqualTo(6L);
        assertThat(product.getStatus()).isEqualTo(0);
    }

    @Test
    void delete_rejectsProductWithBusinessReferences() {
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001", "Cola", 3L, 5L, 1)));
        when(skuUsageMapper.countBusinessReferencesByProductId(7L)).thenReturn(2L);

        assertThatThrownBy(() -> productService.delete(7L))
                .isInstanceOf(BusinessException.class);

        verify(skuService, never()).deleteAllByProductId(7L);
        verify(productMapper, never()).delete(7L);
    }

    @Test
    void delete_deletesSkuRecordsWithoutProductStockDelete() {
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001", "Cola", 3L, 5L, 1)));
        when(skuUsageMapper.countBusinessReferencesByProductId(7L)).thenReturn(0L);

        productService.delete(7L);

        verify(skuService).deleteAllByProductId(7L);
        verify(productMapper).delete(7L);
    }

    private ProductRequest productRequest(
            String productCode,
            String productName,
            Long categoryId,
            Long brandId,
            Integer status
    ) {
        ProductRequest request = new ProductRequest();
        request.setProductCode(productCode);
        request.setProductName(productName);
        request.setCategoryId(categoryId);
        request.setBrandId(brandId);
        request.setStatus(status);
        return request;
    }

    private Product product(Long id, String productCode, String productName, Long categoryId, Long brandId, Integer status) {
        Product product = new Product();
        product.setId(id);
        product.setProductCode(productCode);
        product.setProductName(productName);
        product.setCategoryId(categoryId);
        product.setBrandId(brandId);
        product.setStatus(status);
        product.setCreateTime(LocalDateTime.of(2026, 5, 31, 0, 0));
        return product;
    }

    private Brand brand(Long id, String code, String name, Integer status) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setBrandCode(code);
        brand.setBrandName(name);
        brand.setStatus(status);
        return brand;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

}
