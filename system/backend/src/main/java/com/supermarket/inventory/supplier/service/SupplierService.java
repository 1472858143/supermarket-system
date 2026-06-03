package com.supermarket.inventory.supplier.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.supplier.dto.SupplierRequest;
import com.supermarket.inventory.supplier.entity.Supplier;
import com.supermarket.inventory.supplier.mapper.SupplierMapper;
import com.supermarket.inventory.supplier.vo.SupplierVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private static final String SUPPLIER_CODE_PREFIX = "SUP";
    private static final int SUPPLIER_CODE_SEQUENCE_LENGTH = 6;
    private static final String CONTACT_PHONE_PATTERN = "^[0-9+\\-\\s]+$";

    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    public PageResult<SupplierVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        return new PageResult<>(
                supplierMapper.findPage(keyword, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize)
                        .stream()
                        .map(this::toVO)
                        .toList(),
                supplierMapper.count(keyword),
                normalizedPage,
                normalizedPageSize
        );
    }

    @Transactional
    public SupplierVO create(SupplierRequest request) {
        Supplier supplier = fromRequest(request);
        supplier.setSupplierCode(nextSupplierCode());
        Long id = supplierMapper.insert(supplier);
        return toVO(supplierMapper.findById(id)
                .orElseThrow(() -> new BusinessException("供应商创建失败")));
    }

    @Transactional
    public SupplierVO update(Long id, SupplierRequest request) {
        Supplier supplier = supplierMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "供应商不存在"));
        supplier.setSupplierName(requireText(request.getSupplierName(), "供应商名称不能为空"));
        supplier.setContactPerson(trimToNull(request.getContactPerson()));
        supplier.setContactPhone(normalizeContactPhone(request.getContactPhone()));
        supplier.setAddress(trimToNull(request.getAddress()));
        supplier.setRemark(trimToNull(request.getRemark()));
        supplier.setStatus(normalizeStatus(request.getStatus()));
        supplierMapper.update(supplier);
        return toVO(supplierMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "供应商不存在")));
    }

    @Transactional
    public void delete(Long id) {
        supplierMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "供应商不存在"));
        supplierMapper.delete(id);
    }

    private Supplier fromRequest(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setSupplierName(requireText(request.getSupplierName(), "供应商名称不能为空"));
        supplier.setContactPerson(trimToNull(request.getContactPerson()));
        supplier.setContactPhone(normalizeContactPhone(request.getContactPhone()));
        supplier.setAddress(trimToNull(request.getAddress()));
        supplier.setRemark(trimToNull(request.getRemark()));
        supplier.setStatus(normalizeStatus(request.getStatus()));
        return supplier;
    }

    private SupplierVO toVO(Supplier supplier) {
        SupplierVO vo = new SupplierVO();
        vo.setId(supplier.getId());
        vo.setSupplierCode(supplier.getSupplierCode());
        vo.setSupplierName(supplier.getSupplierName());
        vo.setContactPerson(supplier.getContactPerson());
        vo.setContactPhone(supplier.getContactPhone());
        vo.setAddress(supplier.getAddress());
        vo.setRemark(supplier.getRemark());
        vo.setStatus(supplier.getStatus());
        vo.setCreateTime(supplier.getCreateTime());
        return vo;
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeContactPhone(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (!trimmed.matches(CONTACT_PHONE_PATTERN)) {
            throw new BusinessException("联系电话只能包含数字、空格、+或-");
        }
        return trimmed;
    }

    private int normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status == 0 || status == 1) {
            return status;
        }
        throw new BusinessException("供应商状态不正确");
    }

    private String nextSupplierCode() {
        String maxCode = supplierMapper.findMaxCode(SUPPLIER_CODE_PREFIX + "%");
        int sequence = 1;
        if (maxCode != null) {
            if (!maxCode.startsWith(SUPPLIER_CODE_PREFIX)
                    || maxCode.length() != SUPPLIER_CODE_PREFIX.length() + SUPPLIER_CODE_SEQUENCE_LENGTH) {
                throw new BusinessException("供应商编码序号异常");
            }
            try {
                sequence = Integer.parseInt(maxCode.substring(SUPPLIER_CODE_PREFIX.length())) + 1;
            } catch (NumberFormatException exception) {
                throw new BusinessException("供应商编码序号异常");
            }
        }
        return SUPPLIER_CODE_PREFIX + String.format("%0" + SUPPLIER_CODE_SEQUENCE_LENGTH + "d", sequence);
    }
}
