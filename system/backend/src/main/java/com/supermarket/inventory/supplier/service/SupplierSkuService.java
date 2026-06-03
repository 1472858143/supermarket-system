package com.supermarket.inventory.supplier.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.sku.entity.SkuUnitConversion;
import com.supermarket.inventory.sku.mapper.SkuMapper;
import com.supermarket.inventory.sku.mapper.UnitConversionMapper;
import com.supermarket.inventory.sku.vo.UnitConversionVO;
import com.supermarket.inventory.supplier.dto.SupplierSkuRequest;
import com.supermarket.inventory.supplier.entity.Supplier;
import com.supermarket.inventory.supplier.entity.SupplierSku;
import com.supermarket.inventory.supplier.mapper.SupplierMapper;
import com.supermarket.inventory.supplier.mapper.SupplierSkuMapper;
import com.supermarket.inventory.supplier.vo.SupplierSkuVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class SupplierSkuService {

    private static final BigDecimal MAX_DEFAULT_PURCHASE_PRICE = new BigDecimal("99999999.99");

    private final SupplierSkuMapper supplierSkuMapper;
    private final SupplierMapper supplierMapper;
    private final SkuMapper skuMapper;
    private final UnitConversionMapper unitConversionMapper;

    public SupplierSkuService(
            SupplierSkuMapper supplierSkuMapper,
            SupplierMapper supplierMapper,
            SkuMapper skuMapper,
            UnitConversionMapper unitConversionMapper
    ) {
        this.supplierSkuMapper = supplierSkuMapper;
        this.supplierMapper = supplierMapper;
        this.skuMapper = skuMapper;
        this.unitConversionMapper = unitConversionMapper;
    }

    public List<SupplierSkuVO> list(Long supplierId) {
        requireSupplier(supplierId);
        return fillUnits(supplierSkuMapper.findBySupplierId(supplierId));
    }

    public List<SupplierSkuVO> listEnabled(Long supplierId) {
        Supplier supplier = requireSupplier(supplierId);
        if (!Integer.valueOf(1).equals(supplier.getStatus())) {
            throw new BusinessException("供应商已禁用");
        }
        return fillUnits(supplierSkuMapper.findEnabledBySupplierId(supplierId));
    }

    @Transactional
    public SupplierSkuVO create(Long supplierId, SupplierSkuRequest request) {
        requireSupplier(supplierId);
        validateCreateRequest(request);
        skuMapper.findById(request.getSkuId())
                .orElseThrow(() -> new BusinessException(404, "SKU不存在"));
        if (supplierSkuMapper.existsBySupplierIdAndSkuId(supplierId, request.getSkuId())) {
            throw new BusinessException("该SKU已绑定当前供应商");
        }

        SupplierSku binding = new SupplierSku();
        binding.setSupplierId(supplierId);
        binding.setSkuId(request.getSkuId());
        applyRequest(binding, request);

        Long id = supplierSkuMapper.insert(binding);
        return requireVO(supplierId, id);
    }

    @Transactional
    public SupplierSkuVO update(Long supplierId, Long bindingId, SupplierSkuRequest request) {
        requireSupplier(supplierId);
        SupplierSku binding = requireBindingInSupplier(supplierId, bindingId);
        if (request.getSkuId() != null && !Objects.equals(request.getSkuId(), binding.getSkuId())) {
            throw new BusinessException("绑定SKU不能变更");
        }
        validateUpdateRequest(request);
        applyRequest(binding, request);
        supplierSkuMapper.update(binding);
        return requireVO(supplierId, bindingId);
    }

    @Transactional
    public void delete(Long supplierId, Long bindingId) {
        requireSupplier(supplierId);
        SupplierSku binding = requireBindingInSupplier(supplierId, bindingId);
        if (supplierSkuMapper.countPurchaseInboundReferences(supplierId, binding.getSkuId()) > 0) {
            throw new BusinessException("该供应商SKU已被采购入库引用，请改为禁用");
        }
        supplierSkuMapper.delete(bindingId, supplierId);
    }

    public SupplierSku requireEnabledBinding(Long supplierId, Long skuId) {
        Supplier supplier = requireSupplier(supplierId);
        if (!Integer.valueOf(1).equals(supplier.getStatus())) {
            throw new BusinessException("供应商已禁用");
        }
        SupplierSku binding = supplierSkuMapper.findBySupplierIdAndSkuId(supplierId, skuId)
                .orElseThrow(() -> new BusinessException("该SKU未绑定当前供应商"));
        if (!Integer.valueOf(1).equals(binding.getStatus())) {
            throw new BusinessException("供应商SKU绑定已禁用");
        }
        return binding;
    }

    private Supplier requireSupplier(Long supplierId) {
        return supplierMapper.findById(supplierId)
                .orElseThrow(() -> new BusinessException(404, "供应商不存在"));
    }

    private SupplierSku requireBindingInSupplier(Long supplierId, Long bindingId) {
        SupplierSku binding = supplierSkuMapper.findById(bindingId)
                .orElseThrow(() -> new BusinessException(404, "供应商SKU绑定不存在"));
        if (!Objects.equals(supplierId, binding.getSupplierId())) {
            throw new BusinessException(404, "供应商SKU绑定不存在");
        }
        return binding;
    }

    private SupplierSkuVO requireVO(Long supplierId, Long bindingId) {
        SupplierSkuVO vo = supplierSkuMapper.findVOByIdAndSupplierId(bindingId, supplierId)
                .orElseThrow(() -> new BusinessException(404, "供应商SKU绑定不存在"));
        fillUnits(vo);
        return vo;
    }

    private List<SupplierSkuVO> fillUnits(List<SupplierSkuVO> bindings) {
        bindings.forEach(binding -> binding.setUnits(listUnitsForSku(binding.getSkuId())));
        return bindings;
    }

    private void fillUnits(SupplierSkuVO binding) {
        binding.setUnits(listUnitsForSku(binding.getSkuId()));
    }

    private List<UnitConversionVO> listUnitsForSku(Long skuId) {
        return unitConversionMapper.findBySkuId(skuId).stream()
                .map(this::toVO)
                .toList();
    }

    private UnitConversionVO toVO(SkuUnitConversion conversion) {
        UnitConversionVO vo = new UnitConversionVO();
        vo.setId(conversion.getId());
        vo.setSkuId(conversion.getSkuId());
        vo.setUnitName(conversion.getUnitName());
        vo.setConversionRate(conversion.getConversionRate());
        return vo;
    }

    private void validateCreateRequest(SupplierSkuRequest request) {
        if (request.getSkuId() == null) {
            throw new BusinessException("SKU不能为空");
        }
        validateUpdateRequest(request);
    }

    private void validateUpdateRequest(SupplierSkuRequest request) {
        requireText(request.getSupplierSkuCode(), "供应商SKU编码不能为空");
        requireText(request.getSupplierSkuName(), "供应商SKU名称不能为空");
        validatePrice(request.getDefaultPurchasePrice());
        if (request.getMinPurchaseQuantity() != null && request.getMinPurchaseQuantity() < 1) {
            throw new BusinessException("最小采购数量不能小于1");
        }
        normalizeStatus(request.getStatus());
    }

    private void applyRequest(SupplierSku binding, SupplierSkuRequest request) {
        binding.setSupplierSkuCode(requireText(request.getSupplierSkuCode(), "供应商SKU编码不能为空"));
        binding.setSupplierSkuName(requireText(request.getSupplierSkuName(), "供应商SKU名称不能为空"));
        binding.setSupplierSpec(blankToNull(request.getSupplierSpec()));
        binding.setDefaultPurchasePrice(request.getDefaultPurchasePrice());
        binding.setMinPurchaseQuantity(request.getMinPurchaseQuantity() == null ? 1 : request.getMinPurchaseQuantity());
        binding.setStatus(normalizeStatus(request.getStatus()));
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessException("默认采购价不能为空");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("默认采购价不能小于0");
        }
        if (price.scale() > 2) {
            throw new BusinessException("默认采购价最多保留2位小数");
        }
        if (price.compareTo(MAX_DEFAULT_PURCHASE_PRICE) > 0) {
            throw new BusinessException("默认采购价不能超过99999999.99");
        }
    }

    private int normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status == 0 || status == 1) {
            return status;
        }
        throw new BusinessException("状态不正确");
    }

    private String requireText(String value, String message) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new BusinessException(message);
        }
        return trimmed;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
