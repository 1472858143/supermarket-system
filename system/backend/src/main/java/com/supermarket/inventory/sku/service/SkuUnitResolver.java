package com.supermarket.inventory.sku.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.entity.SkuUnitConversion;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.sku.mapper.UnitConversionMapper;
import org.springframework.stereotype.Service;

@Service
public class SkuUnitResolver {

    private final SkuMapper skuMapper;
    private final UnitConversionMapper unitConversionMapper;

    public SkuUnitResolver(SkuMapper skuMapper, UnitConversionMapper unitConversionMapper) {
        this.skuMapper = skuMapper;
        this.unitConversionMapper = unitConversionMapper;
    }

    public ResolvedUnit resolve(Long skuId, String requestedUnit) {
        Sku sku = skuMapper.findById(skuId)
                .orElseThrow(() -> new BusinessException(404, "SKU不存在"));
        String baseUnit = trim(sku.getBaseUnit());
        String unit = trim(requestedUnit);
        if (unit == null || unit.isBlank()) {
            unit = baseUnit;
        }
        if (unit == null || unit.isBlank()) {
            throw new BusinessException("SKU未配置基础单位");
        }
        final String resolvedUnit = unit;
        if (resolvedUnit.equals(baseUnit)) {
            return new ResolvedUnit(sku, resolvedUnit, 1);
        }
        SkuUnitConversion conversion = unitConversionMapper.findBySkuId(skuId).stream()
                .filter(item -> resolvedUnit.equals(item.getUnitName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未配置该单位换算"));
        Integer conversionRate = conversion.getConversionRate();
        if (conversionRate == null || conversionRate < 1) {
            throw new BusinessException("单位换算率配置无效");
        }
        return new ResolvedUnit(sku, conversion.getUnitName(), conversionRate);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    public record ResolvedUnit(Sku sku, String unit, int conversionRate) {
    }
}
