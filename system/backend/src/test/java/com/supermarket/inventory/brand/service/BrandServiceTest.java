package com.supermarket.inventory.brand.service;

import com.supermarket.inventory.brand.dto.BrandRequest;
import com.supermarket.inventory.brand.entity.Brand;
import com.supermarket.inventory.brand.mapper.BrandMapper;
import com.supermarket.inventory.brand.vo.BrandVO;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandMapper brandMapper;

    private BrandService brandService;

    @BeforeEach
    void setUp() {
        brandService = new BrandService(brandMapper);
    }

    @Test
    void list_normalizesPaginationAndFiltersByStatus() {
        Brand brand = brand(7L, "BRD000007", "Fresh Co", 1);
        when(brandMapper.findPage("fresh", 1, 0, 100)).thenReturn(List.of(brand));
        when(brandMapper.count("fresh", 1)).thenReturn(12L);

        PageResult<BrandVO> result = brandService.list("fresh", 1, 0, 120);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getTotal()).isEqualTo(12L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getBrandCode()).isEqualTo("BRD000007");
        assertThat(result.getItems().get(0).getBrandName()).isEqualTo("Fresh Co");
    }

    @Test
    void options_returnsEnabledBrandsOnlyFromMapper() {
        when(brandMapper.findEnabledOptions())
                .thenReturn(List.of(brand(7L, "BRD000007", "Fresh Co", 1)));

        List<BrandVO> result = brandService.options();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(1);
        verify(brandMapper).findEnabledOptions();
    }

    @Test
    void create_trimsFieldsGeneratesNextCodeAndDefaultsStatusToEnabled() {
        BrandRequest request = request(" Fresh Co ", null, " Main ");

        when(brandMapper.existsByName("Fresh Co")).thenReturn(false);
        when(brandMapper.findMaxCode("BRD%")).thenReturn("BRD000009");
        when(brandMapper.insert(any(Brand.class))).thenReturn(7L);
        when(brandMapper.findById(7L))
                .thenReturn(Optional.of(brand(7L, "BRD000010", "Fresh Co", 1)));

        BrandVO result = brandService.create(request);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getBrandCode()).isEqualTo("BRD000010");

        ArgumentCaptor<Brand> brandCaptor = ArgumentCaptor.forClass(Brand.class);
        verify(brandMapper).insert(brandCaptor.capture());
        Brand inserted = brandCaptor.getValue();
        assertThat(inserted.getBrandCode()).isEqualTo("BRD000010");
        assertThat(inserted.getBrandName()).isEqualTo("Fresh Co");
        assertThat(inserted.getRemark()).isEqualTo("Main");
        assertThat(inserted.getStatus()).isEqualTo(1);
    }

    @Test
    void create_generatesFirstCodeWhenNoExistingBrand() {
        BrandRequest request = request("Fresh Co", 1, null);

        when(brandMapper.existsByName("Fresh Co")).thenReturn(false);
        when(brandMapper.findMaxCode("BRD%")).thenReturn(null);
        when(brandMapper.insert(any(Brand.class))).thenReturn(7L);
        when(brandMapper.findById(7L))
                .thenReturn(Optional.of(brand(7L, "BRD000001", "Fresh Co", 1)));

        brandService.create(request);

        ArgumentCaptor<Brand> brandCaptor = ArgumentCaptor.forClass(Brand.class);
        verify(brandMapper).insert(brandCaptor.capture());
        assertThat(brandCaptor.getValue().getBrandCode()).isEqualTo("BRD000001");
    }

    @Test
    void create_rejectsDuplicateName() {
        BrandRequest request = request("Fresh Co", 1, null);
        when(brandMapper.existsByName("Fresh Co")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("品牌名称已存在");

        verify(brandMapper, never()).insert(any(Brand.class));
    }

    @Test
    void create_rejectsInvalidStatus() {
        BrandRequest request = request("Fresh Co", 2, null);

        assertThatThrownBy(() -> brandService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("品牌状态不正确");

        verify(brandMapper, never()).insert(any(Brand.class));
    }

    @Test
    void update_doesNotChangeBrandCodeAndRejectsDuplicateNameAgainstOtherRows() {
        Brand existing = brand(7L, "BRD000007", "Old name", 1);
        Brand updated = brand(7L, "BRD000007", "New name", 0);
        BrandRequest request = request(" New name ", 0, " Updated ");

        when(brandMapper.findById(7L)).thenReturn(Optional.of(existing), Optional.of(updated));
        when(brandMapper.existsByNameExcluding("New name", 7L)).thenReturn(false);

        BrandVO result = brandService.update(7L, request);

        assertThat(result.getBrandCode()).isEqualTo("BRD000007");
        assertThat(result.getBrandName()).isEqualTo("New name");
        assertThat(result.getStatus()).isEqualTo(0);

        ArgumentCaptor<Brand> brandCaptor = ArgumentCaptor.forClass(Brand.class);
        verify(brandMapper).update(brandCaptor.capture());
        Brand saved = brandCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(7L);
        assertThat(saved.getBrandCode()).isEqualTo("BRD000007");
        assertThat(saved.getBrandName()).isEqualTo("New name");
        assertThat(saved.getRemark()).isEqualTo("Updated");
        assertThat(saved.getStatus()).isEqualTo(0);
    }

    @Test
    void update_throws404WhenBrandMissing() {
        when(brandMapper.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.update(404L, request("Fresh Co", 1, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("品牌不存在")
                .extracting("code")
                .isEqualTo(404);

        verify(brandMapper, never()).update(any(Brand.class));
    }

    @Test
    void update_rejectsDuplicateName() {
        when(brandMapper.findById(7L))
                .thenReturn(Optional.of(brand(7L, "BRD000007", "Old name", 1)));
        when(brandMapper.existsByNameExcluding("Fresh Co", 7L)).thenReturn(true);

        assertThatThrownBy(() -> brandService.update(7L, request("Fresh Co", 1, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("品牌名称已存在");

        verify(brandMapper, never()).update(any(Brand.class));
    }

    @Test
    void delete_rejectsBrandReferencedByProducts() {
        when(brandMapper.findById(7L))
                .thenReturn(Optional.of(brand(7L, "BRD000007", "Fresh Co", 1)));
        when(brandMapper.countProductsByBrandId(7L)).thenReturn(2L);

        assertThatThrownBy(() -> brandService.delete(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该品牌下已有商品，无法删除");

        verify(brandMapper, never()).delete(7L);
    }

    @Test
    void delete_removesUnreferencedBrand() {
        when(brandMapper.findById(7L))
                .thenReturn(Optional.of(brand(7L, "BRD000007", "Fresh Co", 1)));
        when(brandMapper.countProductsByBrandId(7L)).thenReturn(0L);

        brandService.delete(7L);

        verify(brandMapper).delete(7L);
    }

    @Test
    void requireEnabledBrand_validatesNullMissingAndDisabledBrand() {
        assertThatThrownBy(() -> brandService.requireEnabledBrand(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("品牌不能为空");

        when(brandMapper.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> brandService.requireEnabledBrand(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("品牌不存在")
                .extracting("code")
                .isEqualTo(404);

        when(brandMapper.findById(8L))
                .thenReturn(Optional.of(brand(8L, "BRD000008", "Disabled", 0)));
        assertThatThrownBy(() -> brandService.requireEnabledBrand(8L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("品牌已停用");
    }

    @Test
    void requireEnabledBrand_returnsEnabledBrand() {
        Brand enabled = brand(7L, "BRD000007", "Fresh Co", 1);
        when(brandMapper.findById(7L)).thenReturn(Optional.of(enabled));

        Brand result = brandService.requireEnabledBrand(7L);

        assertThat(result).isSameAs(enabled);
    }

    @Test
    void getById_returnsNullWhenBrandMissing() {
        when(brandMapper.findById(7L)).thenReturn(Optional.empty());

        assertThat(brandService.getById(7L)).isNull();
    }

    private BrandRequest request(String name, Integer status, String remark) {
        BrandRequest request = new BrandRequest();
        request.setBrandName(name);
        request.setStatus(status);
        request.setRemark(remark);
        return request;
    }

    private Brand brand(Long id, String code, String name, Integer status) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setBrandCode(code);
        brand.setBrandName(name);
        brand.setRemark("Main");
        brand.setStatus(status);
        brand.setCreateTime(LocalDateTime.of(2026, 6, 1, 0, 0));
        return brand;
    }
}
