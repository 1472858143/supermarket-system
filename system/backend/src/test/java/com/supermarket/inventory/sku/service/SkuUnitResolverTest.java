package com.supermarket.inventory.sku.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.entity.SkuUnitConversion;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.sku.mapper.UnitConversionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuUnitResolverTest {

    @Mock
    private SkuMapper skuMapper;

    @Mock
    private UnitConversionMapper unitConversionMapper;

    private SkuUnitResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SkuUnitResolver(skuMapper, unitConversionMapper);
    }

    @Test
    void resolve_usesBaseUnitWhenRequestUnitIsBlank() {
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, "瓶")));

        SkuUnitResolver.ResolvedUnit resolved = resolver.resolve(20L, " ");

        assertThat(resolved.sku().getId()).isEqualTo(20L);
        assertThat(resolved.unit()).isEqualTo("瓶");
        assertThat(resolved.conversionRate()).isEqualTo(1);
        verify(unitConversionMapper, never()).findBySkuId(20L);
    }

    @Test
    void resolve_usesRateOneWhenRequestUnitIsBaseUnit() {
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, "瓶")));

        SkuUnitResolver.ResolvedUnit resolved = resolver.resolve(20L, "瓶");

        assertThat(resolved.unit()).isEqualTo("瓶");
        assertThat(resolved.conversionRate()).isEqualTo(1);
        verify(unitConversionMapper, never()).findBySkuId(20L);
    }

    @Test
    void resolve_usesConfiguredConversionUnit() {
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, "瓶")));
        when(unitConversionMapper.findBySkuId(20L)).thenReturn(List.of(conversion(20L, "箱", 24)));

        SkuUnitResolver.ResolvedUnit resolved = resolver.resolve(20L, "箱");

        assertThat(resolved.unit()).isEqualTo("箱");
        assertThat(resolved.conversionRate()).isEqualTo(24);
    }

    @Test
    void resolve_rejectsUnknownSku() {
        when(skuMapper.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(20L, "箱"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("SKU不存在");
    }

    @Test
    void resolve_rejectsUnitWithoutConfiguration() {
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, "瓶")));
        when(unitConversionMapper.findBySkuId(20L)).thenReturn(List.of(conversion(20L, "箱", 24)));

        assertThatThrownBy(() -> resolver.resolve(20L, "盒"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未配置该单位换算");
    }

    @Test
    void resolve_rejectsInvalidConversionRate() {
        when(skuMapper.findById(20L)).thenReturn(Optional.of(sku(20L, "瓶")));
        when(unitConversionMapper.findBySkuId(20L)).thenReturn(List.of(conversion(20L, "箱", 0)));

        assertThatThrownBy(() -> resolver.resolve(20L, "箱"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("单位换算率配置无效");
    }

    private Sku sku(Long id, String baseUnit) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setBaseUnit(baseUnit);
        return sku;
    }

    private SkuUnitConversion conversion(Long skuId, String unitName, Integer conversionRate) {
        SkuUnitConversion conversion = new SkuUnitConversion();
        conversion.setSkuId(skuId);
        conversion.setUnitName(unitName);
        conversion.setConversionRate(conversionRate);
        return conversion;
    }
}
