package com.supermarket.inventory.category.service;

import com.supermarket.inventory.category.dto.CategorySortOrderRequest;
import com.supermarket.inventory.category.entity.Category;
import com.supermarket.inventory.category.mapper.CategoryMapper;
import com.supermarket.inventory.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryMapper);
    }

    @Test
    void updateSortOrders_updatesOnlySortOrderWithoutCategoryName() {
        CategorySortOrderRequest first = sortOrderRequest(2L, 10);
        CategorySortOrderRequest second = sortOrderRequest(1L, 20);
        when(categoryMapper.findById(2L)).thenReturn(Optional.of(category(2L, "Dairy", null, 2)));
        when(categoryMapper.findById(1L)).thenReturn(Optional.of(category(1L, "Fresh", null, 1)));

        categoryService.updateSortOrders(List.of(first, second));

        InOrder inOrder = inOrder(categoryMapper);
        inOrder.verify(categoryMapper).findById(2L);
        inOrder.verify(categoryMapper).updateSortOrder(2L, 10);
        inOrder.verify(categoryMapper).findById(1L);
        inOrder.verify(categoryMapper).updateSortOrder(1L, 20);
        verify(categoryMapper, never()).update(org.mockito.ArgumentMatchers.any(Category.class));
    }

    @Test
    void updateSortOrders_rejectsMissingCategory() {
        when(categoryMapper.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateSortOrders(List.of(sortOrderRequest(404L, 10))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类不存在");

        verify(categoryMapper, never()).updateSortOrder(404L, 10);
    }

    private CategorySortOrderRequest sortOrderRequest(Long id, Integer sortOrder) {
        CategorySortOrderRequest request = new CategorySortOrderRequest();
        request.setId(id);
        request.setSortOrder(sortOrder);
        return request;
    }

    private Category category(Long id, String name, Long parentId, Integer sortOrder) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setSortOrder(sortOrder);
        return category;
    }
}
