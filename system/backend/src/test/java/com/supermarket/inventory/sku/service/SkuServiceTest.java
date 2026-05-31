package com.supermarket.inventory.sku.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.product.entity.Product;
import com.supermarket.inventory.product.mapper.ProductMapper;
import com.supermarket.inventory.sku.dto.SkuRequest;
import com.supermarket.inventory.sku.dto.UnitConversionRequest;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.entity.SkuUnitConversion;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.sku.mapper.SkuUsageMapper;
import com.supermarket.inventory.sku.mapper.UnitConversionMapper;
import com.supermarket.inventory.sku.vo.SkuVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuServiceTest {

    @Mock
    private SkuMapper skuMapper;

    @Mock
    private UnitConversionMapper unitConversionMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private SkuUsageMapper skuUsageMapper;

    private SkuService skuService;

    @BeforeEach
    void setUp() {
        skuService = new SkuService(skuMapper, unitConversionMapper, productMapper, skuUsageMapper);
    }

    @Test
    void create_generatesNextSkuCodeUnderProduct() {
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001")));
        when(skuMapper.countByProductId(7L)).thenReturn(1);
        when(skuMapper.findByCode("P001-002")).thenReturn(Optional.empty());
        when(skuMapper.insert(any(Sku.class))).thenReturn(20L);
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, 7L, "P001-002", 0)));

        SkuVO created = skuService.create(7L, skuRequest("500ml", "500ml/瓶", "6900000000010", "瓶", "8.00", "10.00", 1));

        ArgumentCaptor<Sku> skuCaptor = ArgumentCaptor.forClass(Sku.class);
        verify(skuMapper).insert(skuCaptor.capture());
        assertThat(skuCaptor.getValue().getSkuCode()).isEqualTo("P001-002");
        assertThat(created.getSkuCode()).isEqualTo("P001-002");
    }

    @Test
    void create_rejectsSalePriceLowerThanPurchasePrice() {
        SkuRequest request = skuRequest("500ml", "500ml/瓶", "6900000000010", "瓶", "10.00", "8.00", 1);
        when(productMapper.findById(7L)).thenReturn(Optional.of(product(7L, "P001")));

        assertThatThrownBy(() -> skuService.create(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU售价不能低于进价");
    }

    @Test
    void delete_rejectsDefaultSku() {
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, 7L, "P001-001", 1)));

        assertThatThrownBy(() -> skuService.delete(7L, 20L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("默认SKU不能删除");
    }

    @Test
    void createUnit_rejectsDuplicateUnitName() {
        UnitConversionRequest request = unitRequest("箱", 12);
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, 7L, "P001-001", 0)));
        when(unitConversionMapper.existsBySkuIdAndUnitName(20L, "箱")).thenReturn(true);

        assertThatThrownBy(() -> skuService.createUnit(7L, 20L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("单位名称已存在");
    }

    @Test
    void listUnits_rejectsSkuOutsideProduct() {
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, 8L, "P002-001", 0)));

        assertThatThrownBy(() -> skuService.listUnits(7L, 20L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SKU不存在");
    }

    @Test
    void updateUnit_allowsSameNameOnCurrentRecord() {
        UnitConversionRequest request = unitRequest("箱", 24);
        SkuUnitConversion existing = unitConversion(30L, 20L, "箱", 12);
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, 7L, "P001-001", 0)));
        when(unitConversionMapper.findById(30L)).thenReturn(Optional.of(existing));
        when(unitConversionMapper.existsOtherBySkuIdAndUnitName(20L, 30L, "箱")).thenReturn(false);

        skuService.updateUnit(7L, 20L, 30L, request);

        assertThat(existing.getConversionRate()).isEqualTo(24);
        verify(unitConversionMapper).update(existing);
    }

    private Product product(Long id, String productCode) {
        Product product = new Product();
        product.setId(id);
        product.setProductCode(productCode);
        product.setProductName("测试商品");
        product.setCategoryId(1L);
        product.setStatus(1);
        product.setCreateTime(LocalDateTime.of(2026, 5, 31, 0, 0));
        return product;
    }

    private SkuRequest skuRequest(
            String skuName,
            String spec,
            String barcode,
            String baseUnit,
            String purchasePrice,
            String salePrice,
            Integer status
    ) {
        SkuRequest request = new SkuRequest();
        request.setSkuName(skuName);
        request.setSpec(spec);
        request.setBarcode(barcode);
        request.setBaseUnit(baseUnit);
        request.setPurchasePrice(new BigDecimal(purchasePrice));
        request.setSalePrice(new BigDecimal(salePrice));
        request.setStatus(status);
        return request;
    }

    private UnitConversionRequest unitRequest(String unitName, Integer conversionRate) {
        UnitConversionRequest request = new UnitConversionRequest();
        request.setUnitName(unitName);
        request.setConversionRate(conversionRate);
        return request;
    }

    private Sku sku(Long id, Long productId, String skuCode, Integer isDefault) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setSkuCode(skuCode);
        sku.setSkuName("500ml");
        sku.setSpec("500ml/瓶");
        sku.setBarcode("6900000000010");
        sku.setBaseUnit("瓶");
        sku.setPurchasePrice(new BigDecimal("8.00"));
        sku.setSalePrice(new BigDecimal("10.00"));
        sku.setStatus(1);
        sku.setIsDefault(isDefault);
        sku.setCreateTime(LocalDateTime.of(2026, 5, 31, 0, 0));
        return sku;
    }

    private SkuUnitConversion unitConversion(Long id, Long skuId, String unitName, Integer conversionRate) {
        SkuUnitConversion conversion = new SkuUnitConversion();
        conversion.setId(id);
        conversion.setSkuId(skuId);
        conversion.setUnitName(unitName);
        conversion.setConversionRate(conversionRate);
        conversion.setCreateTime(LocalDateTime.of(2026, 5, 31, 0, 0));
        return conversion;
    }
}
