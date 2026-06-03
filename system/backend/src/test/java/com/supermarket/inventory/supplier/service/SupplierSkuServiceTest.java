package com.supermarket.inventory.supplier.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.entity.SkuUnitConversion;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.sku.mapper.UnitConversionMapper;
import com.supermarket.inventory.supplier.dto.SupplierSkuRequest;
import com.supermarket.inventory.supplier.entity.Supplier;
import com.supermarket.inventory.supplier.entity.SupplierSku;
import com.supermarket.inventory.supplier.mapper.SupplierMapper;
import com.supermarket.inventory.supplier.mapper.SupplierSkuMapper;
import com.supermarket.inventory.supplier.vo.SupplierSkuVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierSkuServiceTest {

    @Mock
    private SupplierSkuMapper supplierSkuMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SkuMapper skuMapper;

    @Mock
    private UnitConversionMapper unitConversionMapper;

    private SupplierSkuService supplierSkuService;

    @BeforeEach
    void setUp() {
        supplierSkuService = new SupplierSkuService(
                supplierSkuMapper,
                supplierMapper,
                skuMapper,
                unitConversionMapper
        );
    }

    @Test
    void create_trimsFieldsAndSavesDefaults() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        when(skuMapper.findById(11L)).thenReturn(Optional.of(sku(11L)));
        when(supplierSkuMapper.existsBySupplierIdAndSkuId(7L, 11L)).thenReturn(false);
        when(supplierSkuMapper.insert(any(SupplierSku.class))).thenReturn(3L);
        when(supplierSkuMapper.findVOByIdAndSupplierId(3L, 7L)).thenReturn(Optional.of(vo(3L, 7L, 11L)));
        when(unitConversionMapper.findBySkuId(11L)).thenReturn(List.of(unit(5L, 11L)));

        SupplierSkuRequest request = request();
        request.setSupplierSkuCode(" SUP-COLA ");
        request.setSupplierSkuName(" Supplier Cola ");
        request.setSupplierSpec(" 24 bottles ");
        request.setStatus(null);
        request.setMinPurchaseQuantity(null);

        SupplierSkuVO created = supplierSkuService.create(7L, request);

        ArgumentCaptor<SupplierSku> captor = ArgumentCaptor.forClass(SupplierSku.class);
        verify(supplierSkuMapper).insert(captor.capture());
        SupplierSku saved = captor.getValue();

        assertThat(saved.getSupplierId()).isEqualTo(7L);
        assertThat(saved.getSkuId()).isEqualTo(11L);
        assertThat(saved.getSupplierSkuCode()).isEqualTo("SUP-COLA");
        assertThat(saved.getSupplierSkuName()).isEqualTo("Supplier Cola");
        assertThat(saved.getSupplierSpec()).isEqualTo("24 bottles");
        assertThat(saved.getDefaultPurchasePrice()).isEqualByComparingTo("12.50");
        assertThat(saved.getMinPurchaseQuantity()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(created.getId()).isEqualTo(3L);
        assertThat(created.getUnits()).hasSize(1);
        assertThat(created.getUnits().get(0).getUnitName()).isEqualTo("箱");
        verify(supplierSkuMapper, never()).findBySupplierId(7L);
    }

    @Test
    void create_rejectsDuplicateBinding() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        when(skuMapper.findById(11L)).thenReturn(Optional.of(sku(11L)));
        when(supplierSkuMapper.existsBySupplierIdAndSkuId(7L, 11L)).thenReturn(true);

        assertThatThrownBy(() -> supplierSkuService.create(7L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该SKU已绑定当前供应商");

        verify(supplierSkuMapper, never()).insert(any(SupplierSku.class));
    }

    @Test
    void create_rejectsNegativeDefaultPurchasePrice() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        SupplierSkuRequest request = request();
        request.setDefaultPurchasePrice(new BigDecimal("-0.01"));

        assertThatThrownBy(() -> supplierSkuService.create(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("默认采购价不能小于0");

        verify(skuMapper, never()).findById(any());
        verify(supplierSkuMapper, never()).insert(any(SupplierSku.class));
    }

    @Test
    void create_rejectsDefaultPurchasePriceAboveDatabaseLimit() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        SupplierSkuRequest request = request();
        request.setDefaultPurchasePrice(new BigDecimal("100000000.00"));

        assertThatThrownBy(() -> supplierSkuService.create(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("默认采购价不能超过99999999.99");

        verify(skuMapper, never()).findById(any());
        verify(supplierSkuMapper, never()).insert(any(SupplierSku.class));
    }

    @Test
    void create_rejectsDefaultPurchasePriceScaleGreaterThanTwo() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        SupplierSkuRequest request = request();
        request.setDefaultPurchasePrice(new BigDecimal("12.345"));

        assertThatThrownBy(() -> supplierSkuService.create(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("默认采购价最多保留2位小数");

        verify(skuMapper, never()).findById(any());
        verify(supplierSkuMapper, never()).insert(any(SupplierSku.class));
    }

    @Test
    void update_rejectsBindingOutsideSupplier() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        SupplierSku binding = binding(3L, 8L, 11L, 1);
        when(supplierSkuMapper.findById(3L)).thenReturn(Optional.of(binding));

        assertThatThrownBy(() -> supplierSkuService.update(7L, 3L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商SKU绑定不存在");

        verify(supplierSkuMapper, never()).update(any(SupplierSku.class));
    }

    @Test
    void update_rejectsChangingSkuId() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        SupplierSku binding = binding(3L, 7L, 11L, 1);
        when(supplierSkuMapper.findById(3L)).thenReturn(Optional.of(binding));
        SupplierSkuRequest request = request();
        request.setSkuId(12L);

        assertThatThrownBy(() -> supplierSkuService.update(7L, 3L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("绑定SKU不能变更");

        verify(supplierSkuMapper, never()).update(any(SupplierSku.class));
    }

    @Test
    void delete_rejectsBindingReferencedByPurchaseInbound() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        SupplierSku binding = binding(3L, 7L, 11L, 1);
        when(supplierSkuMapper.findById(3L)).thenReturn(Optional.of(binding));
        when(supplierSkuMapper.countPurchaseInboundReferences(7L, 11L)).thenReturn(2L);

        assertThatThrownBy(() -> supplierSkuService.delete(7L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该供应商SKU已被采购入库引用，请改为禁用");

        verify(supplierSkuMapper, never()).delete(3L, 7L);
    }

    @Test
    void listEnabled_rejectsDisabledSupplier() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 0)));

        assertThatThrownBy(() -> supplierSkuService.listEnabled(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商已禁用");

        verify(supplierSkuMapper, never()).findEnabledBySupplierId(7L);
    }

    @Test
    void requireEnabledBinding_rejectsMissingBinding() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        when(supplierSkuMapper.findBySupplierIdAndSkuId(7L, 11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierSkuService.requireEnabledBinding(7L, 11L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该SKU未绑定当前供应商");
    }

    @Test
    void requireEnabledBinding_rejectsDisabledSupplier() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 0)));

        assertThatThrownBy(() -> supplierSkuService.requireEnabledBinding(7L, 11L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商已禁用");

        verify(supplierSkuMapper, never()).findBySupplierIdAndSkuId(7L, 11L);
    }

    @Test
    void requireEnabledBinding_rejectsDisabledBinding() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        when(supplierSkuMapper.findBySupplierIdAndSkuId(7L, 11L))
                .thenReturn(Optional.of(binding(3L, 7L, 11L, 0)));

        assertThatThrownBy(() -> supplierSkuService.requireEnabledBinding(7L, 11L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商SKU绑定已禁用");
    }

    @Test
    void requireEnabledBinding_returnsEnabledBinding() {
        SupplierSku binding = binding(3L, 7L, 11L, 1);
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        when(supplierSkuMapper.findBySupplierIdAndSkuId(7L, 11L)).thenReturn(Optional.of(binding));

        SupplierSku result = supplierSkuService.requireEnabledBinding(7L, 11L);

        assertThat(result).isSameAs(binding);
    }

    private SupplierSkuRequest request() {
        SupplierSkuRequest request = new SupplierSkuRequest();
        request.setSkuId(11L);
        request.setSupplierSkuCode("SUP-COLA");
        request.setSupplierSkuName("Supplier Cola");
        request.setSupplierSpec("24 bottles");
        request.setDefaultPurchasePrice(new BigDecimal("12.50"));
        request.setMinPurchaseQuantity(5);
        request.setStatus(1);
        return request;
    }

    private Supplier supplier(Long id, int status) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setStatus(status);
        return supplier;
    }

    private Sku sku(Long id) {
        Sku sku = new Sku();
        sku.setId(id);
        return sku;
    }

    private SupplierSku binding(Long id, Long supplierId, Long skuId, int status) {
        SupplierSku binding = new SupplierSku();
        binding.setId(id);
        binding.setSupplierId(supplierId);
        binding.setSkuId(skuId);
        binding.setStatus(status);
        return binding;
    }

    private SupplierSkuVO vo(Long id, Long supplierId, Long skuId) {
        SupplierSkuVO vo = new SupplierSkuVO();
        vo.setId(id);
        vo.setSupplierId(supplierId);
        vo.setSkuId(skuId);
        return vo;
    }

    private SkuUnitConversion unit(Long id, Long skuId) {
        SkuUnitConversion unit = new SkuUnitConversion();
        unit.setId(id);
        unit.setSkuId(skuId);
        unit.setUnitName("箱");
        unit.setConversionRate(12);
        return unit;
    }
}
