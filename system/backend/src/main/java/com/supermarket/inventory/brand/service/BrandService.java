package com.supermarket.inventory.brand.service;

import com.supermarket.inventory.brand.dto.BrandRequest;
import com.supermarket.inventory.brand.entity.Brand;
import com.supermarket.inventory.brand.mapper.BrandMapper;
import com.supermarket.inventory.brand.vo.BrandVO;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandService {

    private static final String BRAND_CODE_PREFIX = "BRD";
    private static final int BRAND_CODE_SEQUENCE_LENGTH = 6;
    private static final int BRAND_CODE_MAX_SEQUENCE = 999999;

    private final BrandMapper brandMapper;

    public BrandService(BrandMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    public PageResult<BrandVO> list(String keyword, Integer status, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        long total = brandMapper.count(keyword, status);
        return new PageResult<>(
                brandMapper.findPage(keyword, status, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize)
                        .stream()
                        .map(this::toVO)
                        .toList(),
                total,
                normalizedPage,
                normalizedPageSize
        );
    }

    public List<BrandVO> options() {
        return brandMapper.findEnabledOptions()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public BrandVO create(BrandRequest request) {
        Brand brand = fromRequest(request);
        if (brandMapper.existsByName(brand.getBrandName())) {
            throw new BusinessException("品牌名称已存在");
        }
        brand.setBrandCode(nextBrandCode());
        Long id = brandMapper.insert(brand);
        return toVO(brandMapper.findById(id)
                .orElseThrow(() -> new BusinessException("品牌创建失败")));
    }

    @Transactional
    public BrandVO update(Long id, BrandRequest request) {
        Brand brand = brandMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "品牌不存在"));
        String brandName = requireText(request.getBrandName(), "品牌名称不能为空");
        if (brandMapper.existsByNameExcluding(brandName, id)) {
            throw new BusinessException("品牌名称已存在");
        }
        brand.setBrandName(brandName);
        brand.setStatus(normalizeStatus(request.getStatus()));
        brand.setRemark(trimToNull(request.getRemark()));
        brandMapper.update(brand);
        return toVO(brandMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "品牌不存在")));
    }

    @Transactional
    public void delete(Long id) {
        brandMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "品牌不存在"));
        if (brandMapper.countProductsByBrandId(id) > 0) {
            throw new BusinessException("该品牌下已有商品，无法删除");
        }
        brandMapper.delete(id);
    }

    public Brand requireEnabledBrand(Long id) {
        if (id == null) {
            throw new BusinessException("品牌不能为空");
        }
        Brand brand = brandMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "品牌不存在"));
        if (!Integer.valueOf(1).equals(brand.getStatus())) {
            throw new BusinessException("品牌已停用");
        }
        return brand;
    }

    public Brand getById(Long id) {
        return brandMapper.findById(id).orElse(null);
    }

    private Brand fromRequest(BrandRequest request) {
        Brand brand = new Brand();
        brand.setBrandName(requireText(request.getBrandName(), "品牌名称不能为空"));
        brand.setStatus(normalizeStatus(request.getStatus()));
        brand.setRemark(trimToNull(request.getRemark()));
        return brand;
    }

    private BrandVO toVO(Brand brand) {
        BrandVO vo = new BrandVO();
        vo.setId(brand.getId());
        vo.setBrandCode(brand.getBrandCode());
        vo.setBrandName(brand.getBrandName());
        vo.setStatus(brand.getStatus());
        vo.setRemark(brand.getRemark());
        vo.setCreateTime(brand.getCreateTime());
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

    private int normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status == 0 || status == 1) {
            return status;
        }
        throw new BusinessException("品牌状态不正确");
    }

    private String nextBrandCode() {
        String maxCode = brandMapper.findMaxCode(BRAND_CODE_PREFIX + "%");
        int sequence = 1;
        if (maxCode != null) {
            if (!maxCode.startsWith(BRAND_CODE_PREFIX)
                    || maxCode.length() != BRAND_CODE_PREFIX.length() + BRAND_CODE_SEQUENCE_LENGTH) {
                throw new BusinessException("品牌编码序号异常");
            }
            try {
                sequence = Integer.parseInt(maxCode.substring(BRAND_CODE_PREFIX.length())) + 1;
            } catch (NumberFormatException exception) {
                throw new BusinessException("品牌编码序号异常");
            }
        }
        if (sequence > BRAND_CODE_MAX_SEQUENCE) {
            throw new BusinessException("品牌编码序号已达上限");
        }
        return BRAND_CODE_PREFIX + String.format("%0" + BRAND_CODE_SEQUENCE_LENGTH + "d", sequence);
    }
}
