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
import com.supermarket.inventory.sku.vo.UnitConversionVO;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class SkuService {

    private final SkuMapper skuMapper;
    private final UnitConversionMapper unitConversionMapper;
    private final ProductMapper productMapper;
    private final SkuUsageMapper skuUsageMapper;
    private final StockService stockService;

    public SkuService(
            SkuMapper skuMapper,
            UnitConversionMapper unitConversionMapper,
            ProductMapper productMapper,
            SkuUsageMapper skuUsageMapper,
            StockService stockService
    ) {
        this.skuMapper = skuMapper;
        this.unitConversionMapper = unitConversionMapper;
        this.productMapper = productMapper;
        this.skuUsageMapper = skuUsageMapper;
        this.stockService = stockService;
    }

    public List<SkuVO> listByProductId(Long productId) {
        return skuMapper.findByProductId(productId).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public SkuVO create(Long productId, SkuRequest request) {
        Product product = productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        validatePrice(request.getPurchasePrice(), request.getSalePrice());

        int existingSkuCount = skuMapper.countByProductId(product.getId());
        Sku sku = new Sku();
        sku.setProductId(product.getId());
        sku.setSkuCode(generateSkuCode(product, existingSkuCount + 1));
        applyRequest(sku, request);
        sku.setIsDefault(existingSkuCount == 0 ? 1 : 0);

        Long id = skuMapper.insert(sku);
        Sku created = skuMapper.findById(id).orElse(sku);
        if (created.getId() == null) {
            created.setId(id);
        }
        stockService.initializeStock(id);
        return toVO(created);
    }

    @Transactional
    public SkuVO update(Long productId, Long skuId, SkuRequest request) {
        Sku sku = requireSkuInProduct(productId, skuId);
        validatePrice(request.getPurchasePrice(), request.getSalePrice());

        applyRequest(sku, request);
        skuMapper.update(sku);
        return toVO(sku);
    }

    @Transactional
    public void delete(Long productId, Long skuId) {
        Sku sku = requireSkuInProduct(productId, skuId);
        if (Integer.valueOf(1).equals(sku.getIsDefault())) {
            throw new BusinessException("默认SKU不能删除");
        }
        if (skuUsageMapper.countBusinessReferences(skuId) > 0) {
            throw new BusinessException("SKU已有业务记录，不能删除");
        }

        stockService.deleteStockBySkuId(skuId);
        unitConversionMapper.deleteBySkuId(skuId);
        skuMapper.delete(skuId);
    }

    @Transactional
    public void deleteAllByProductId(Long productId) {
        skuMapper.findByProductId(productId)
                .forEach(sku -> stockService.deleteStockBySkuId(sku.getId()));
        unitConversionMapper.deleteByProductId(productId);
        skuMapper.deleteByProductId(productId);
    }

    public List<UnitConversionVO> listUnits(Long productId, Long skuId) {
        requireSkuInProduct(productId, skuId);
        return listUnitsForSku(skuId);
    }

    private List<UnitConversionVO> listUnitsForSku(Long skuId) {
        return unitConversionMapper.findBySkuId(skuId).stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public UnitConversionVO createUnit(Long productId, Long skuId, UnitConversionRequest request) {
        requireSkuInProduct(productId, skuId);
        String unitName = trim(request.getUnitName());
        if (unitConversionMapper.existsBySkuIdAndUnitName(skuId, unitName)) {
            throw new BusinessException("单位名称已存在");
        }

        SkuUnitConversion conversion = new SkuUnitConversion();
        conversion.setSkuId(skuId);
        conversion.setUnitName(unitName);
        conversion.setConversionRate(request.getConversionRate());
        Long id = unitConversionMapper.insert(conversion);
        SkuUnitConversion created = unitConversionMapper.findById(id).orElse(conversion);
        if (created.getId() == null) {
            created.setId(id);
        }
        return toVO(created);
    }

    @Transactional
    public UnitConversionVO updateUnit(Long productId, Long skuId, Long unitId, UnitConversionRequest request) {
        requireSkuInProduct(productId, skuId);
        SkuUnitConversion conversion = requireUnitInSku(skuId, unitId);
        String unitName = trim(request.getUnitName());
        if (unitConversionMapper.existsOtherBySkuIdAndUnitName(skuId, unitId, unitName)) {
            throw new BusinessException("单位名称已存在");
        }

        conversion.setUnitName(unitName);
        conversion.setConversionRate(request.getConversionRate());
        unitConversionMapper.update(conversion);
        return toVO(conversion);
    }

    @Transactional
    public void deleteUnit(Long productId, Long skuId, Long unitId) {
        requireSkuInProduct(productId, skuId);
        requireUnitInSku(skuId, unitId);
        unitConversionMapper.delete(unitId);
    }

    private Sku requireSkuInProduct(Long productId, Long skuId) {
        Sku sku = skuMapper.findById(skuId)
                .orElseThrow(() -> new BusinessException(404, "SKU不存在"));
        if (!Objects.equals(productId, sku.getProductId())) {
            throw new BusinessException(404, "SKU不存在");
        }
        return sku;
    }

    private SkuUnitConversion requireUnitInSku(Long skuId, Long unitId) {
        SkuUnitConversion conversion = unitConversionMapper.findById(unitId)
                .orElseThrow(() -> new BusinessException(404, "单位换算不存在"));
        if (!Objects.equals(skuId, conversion.getSkuId())) {
            throw new BusinessException(404, "单位换算不存在");
        }
        return conversion;
    }

    private String generateSkuCode(Product product, int initialSequence) {
        int next = initialSequence;
        String code;
        do {
            code = product.getProductCode() + "-" + String.format("%03d", next);
            next++;
        } while (skuMapper.findByCode(code).isPresent());
        return code;
    }

    private void applyRequest(Sku sku, SkuRequest request) {
        sku.setSkuName(trim(request.getSkuName()));
        sku.setSpec(trim(request.getSpec()));
        sku.setBarcode(blankToNull(request.getBarcode()));
        sku.setBaseUnit(trim(request.getBaseUnit()));
        sku.setPurchasePrice(request.getPurchasePrice());
        sku.setSalePrice(request.getSalePrice());
        sku.setStatus(normalizeStatus(request.getStatus()));
    }

    private void validatePrice(BigDecimal purchasePrice, BigDecimal salePrice) {
        if (salePrice != null && purchasePrice != null && salePrice.compareTo(purchasePrice) < 0) {
            throw new BusinessException("SKU售价不能低于进价");
        }
    }

    private int normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }

    private SkuVO toVO(Sku sku) {
        SkuVO vo = new SkuVO();
        vo.setId(sku.getId());
        vo.setProductId(sku.getProductId());
        vo.setSkuCode(sku.getSkuCode());
        vo.setSkuName(sku.getSkuName());
        vo.setSpec(sku.getSpec());
        vo.setBarcode(sku.getBarcode());
        vo.setBaseUnit(sku.getBaseUnit());
        vo.setPurchasePrice(sku.getPurchasePrice());
        vo.setSalePrice(sku.getSalePrice());
        vo.setStatus(sku.getStatus());
        vo.setIsDefault(sku.getIsDefault());
        vo.setCreateTime(sku.getCreateTime());
        vo.setUnits(listUnitsForSku(sku.getId()));
        return vo;
    }

    private UnitConversionVO toVO(SkuUnitConversion conversion) {
        UnitConversionVO vo = new UnitConversionVO();
        vo.setId(conversion.getId());
        vo.setSkuId(conversion.getSkuId());
        vo.setUnitName(conversion.getUnitName());
        vo.setConversionRate(conversion.getConversionRate());
        return vo;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
