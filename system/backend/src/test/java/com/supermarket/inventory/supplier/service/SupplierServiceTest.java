package com.supermarket.inventory.supplier.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.supplier.dto.SupplierRequest;
import com.supermarket.inventory.supplier.entity.Supplier;
import com.supermarket.inventory.supplier.mapper.SupplierMapper;
import com.supermarket.inventory.supplier.vo.SupplierVO;
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
class SupplierServiceTest {

    @Mock
    private SupplierMapper supplierMapper;

    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierService(supplierMapper);
    }

    @Test
    void list_normalizesPaginationAndReturnsPageResult() {
        Supplier supplier = supplier(7L, "S001", "Fresh Co", 1);
        when(supplierMapper.findPage("fresh", 0, 100)).thenReturn(List.of(supplier));
        when(supplierMapper.count("fresh")).thenReturn(12L);

        PageResult<SupplierVO> result = supplierService.list("fresh", 0, 120);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getTotal()).isEqualTo(12L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getSupplierCode()).isEqualTo("S001");
        assertThat(result.getItems().get(0).getSupplierName()).isEqualTo("Fresh Co");
    }

    @Test
    void create_generatesNextSupplierCodeAndDefaultsStatusToEnabled() {
        SupplierRequest request = request(null, " Fresh Co ", null);
        request.setContactPerson(" Alice ");
        request.setContactPhone(" 13800000000 ");
        request.setAddress(" Road 1 ");
        request.setRemark(" Main ");

        when(supplierMapper.findMaxCode("SUP%")).thenReturn("SUP000009");
        when(supplierMapper.insert(any(Supplier.class))).thenReturn(7L);
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, "SUP000010", "Fresh Co", 1)));

        SupplierVO result = supplierService.create(request);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getSupplierCode()).isEqualTo("SUP000010");
        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierMapper).insert(supplierCaptor.capture());
        Supplier inserted = supplierCaptor.getValue();
        assertThat(inserted.getSupplierCode()).isEqualTo("SUP000010");
        assertThat(inserted.getSupplierName()).isEqualTo("Fresh Co");
        assertThat(inserted.getContactPerson()).isEqualTo("Alice");
        assertThat(inserted.getContactPhone()).isEqualTo("13800000000");
        assertThat(inserted.getAddress()).isEqualTo("Road 1");
        assertThat(inserted.getRemark()).isEqualTo("Main");
        assertThat(inserted.getStatus()).isEqualTo(1);
    }

    @Test
    void create_generatesFirstSupplierCodeWhenNoExistingSupplier() {
        SupplierRequest request = request(null, "Fresh Co", 1);
        when(supplierMapper.findMaxCode("SUP%")).thenReturn(null);
        when(supplierMapper.insert(any(Supplier.class))).thenReturn(7L);
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, "SUP000001", "Fresh Co", 1)));

        supplierService.create(request);

        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierMapper).insert(supplierCaptor.capture());
        assertThat(supplierCaptor.getValue().getSupplierCode()).isEqualTo("SUP000001");
    }

    @Test
    void create_rejectsContactPhoneContainingLetters() {
        SupplierRequest request = request(null, "Fresh Co", 1);
        request.setContactPhone("phone123");

        assertThatThrownBy(() -> supplierService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("联系电话只能包含数字、空格、+或-");

        verify(supplierMapper, never()).insert(any(Supplier.class));
    }

    @Test
    void create_rejectsInvalidExistingSupplierCodeSequence() {
        SupplierRequest request = request(null, "Fresh Co", 1);
        when(supplierMapper.findMaxCode("SUP%")).thenReturn("SUPABCDEF");

        assertThatThrownBy(() -> supplierService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商编码序号异常");

        verify(supplierMapper, never()).insert(any(Supplier.class));
    }

    @Test
    void update_doesNotChangeSupplierCode() {
        Supplier existing = supplier(7L, "S001", "Old name", 1);
        Supplier updated = supplier(7L, "S001", "New name", 0);
        SupplierRequest request = request("CHANGED", " New name ", 0);
        request.setContactPerson(" Bob ");

        when(supplierMapper.findById(7L)).thenReturn(Optional.of(existing), Optional.of(updated));

        SupplierVO result = supplierService.update(7L, request);

        assertThat(result.getSupplierCode()).isEqualTo("S001");
        assertThat(result.getSupplierName()).isEqualTo("New name");
        assertThat(result.getStatus()).isEqualTo(0);

        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierMapper).update(supplierCaptor.capture());
        Supplier saved = supplierCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(7L);
        assertThat(saved.getSupplierCode()).isEqualTo("S001");
        assertThat(saved.getSupplierName()).isEqualTo("New name");
        assertThat(saved.getContactPerson()).isEqualTo("Bob");
        assertThat(saved.getStatus()).isEqualTo(0);
    }

    @Test
    void update_throwsBusinessExceptionWhenSupplierMissing() {
        when(supplierMapper.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.update(404L, request("S001", "Fresh Co", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商不存在");

        verify(supplierMapper, never()).update(any(Supplier.class));
    }

    @Test
    void update_rejectsInvalidStatus() {
        Supplier existing = supplier(7L, "S001", "Old name", 1);
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> supplierService.update(7L, request("S001", "Fresh Co", 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商状态不正确");

        verify(supplierMapper, never()).update(any(Supplier.class));
    }

    @Test
    void update_rejectsContactPhoneContainingText() {
        Supplier existing = supplier(7L, "S001", "Old name", 1);
        SupplierRequest request = request("S001", "Fresh Co", 1);
        request.setContactPhone("电话123");
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> supplierService.update(7L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("联系电话只能包含数字、空格、+或-");

        verify(supplierMapper, never()).update(any(Supplier.class));
    }

    @Test
    void delete_throwsBusinessExceptionWhenSupplierMissing() {
        when(supplierMapper.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.delete(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商不存在");

        verify(supplierMapper, never()).delete(404L);
    }

    @Test
    void delete_removesExistingSupplier() {
        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, "S001", "Fresh Co", 1)));

        supplierService.delete(7L);

        verify(supplierMapper).delete(7L);
    }

    private SupplierRequest request(String code, String name, Integer status) {
        SupplierRequest request = new SupplierRequest();
        request.setSupplierCode(code);
        request.setSupplierName(name);
        request.setStatus(status);
        return request;
    }

    private Supplier supplier(Long id, String code, String name, Integer status) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setSupplierCode(code);
        supplier.setSupplierName(name);
        supplier.setContactPerson("Alice");
        supplier.setContactPhone("13800000000");
        supplier.setAddress("Road 1");
        supplier.setRemark("Main");
        supplier.setStatus(status);
        supplier.setCreateTime(LocalDateTime.of(2026, 6, 1, 0, 0));
        return supplier;
    }
}
