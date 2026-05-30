package com.supermarket.inventory.category.service;

import com.supermarket.inventory.category.dto.CategoryRequest;
import com.supermarket.inventory.category.entity.Category;
import com.supermarket.inventory.category.mapper.CategoryMapper;
import com.supermarket.inventory.category.vo.CategoryVO;
import com.supermarket.inventory.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryVO> getCategoryTree() {
        List<Category> all = categoryMapper.findAll();
        List<CategoryVO> roots = new ArrayList<>();
        List<CategoryVO> allVOs = all.stream().map(this::toVO).toList();

        for (CategoryVO vo : allVOs) {
            if (vo.getParentId() == null) {
                roots.add(vo);
            }
        }
        for (CategoryVO root : roots) {
            for (CategoryVO vo : allVOs) {
                if (root.getId().equals(vo.getParentId())) {
                    root.getChildren().add(vo);
                }
            }
        }
        return roots;
    }

    @Transactional
    public CategoryVO create(CategoryRequest request) {
        if (request.getParentId() != null) {
            Category parent = categoryMapper.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException("父分类不存在"));
            if (parent.getParentId() != null) {
                throw new BusinessException("不允许创建三级分类");
            }
        }
        if (categoryMapper.existsByParentIdAndName(request.getParentId(), request.getName().trim())) {
            throw new BusinessException("该分类名称已存在");
        }

        Category category = new Category();
        category.setName(request.getName().trim());
        category.setParentId(request.getParentId());
        category.setSortOrder(categoryMapper.maxSortOrder(request.getParentId()) + 1);

        Long id = categoryMapper.insert(category);
        return toVO(categoryMapper.findById(id)
                .orElseThrow(() -> new BusinessException("分类创建失败")));
    }

    @Transactional
    public CategoryVO update(Long id, CategoryRequest request) {
        Category category = categoryMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "分类不存在"));

        if (categoryMapper.existsByParentIdAndNameExcluding(category.getParentId(), request.getName().trim(), id)) {
            throw new BusinessException("该分类名称已存在");
        }

        category.setName(request.getName().trim());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        categoryMapper.update(category);
        return toVO(categoryMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "分类不存在")));
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "分类不存在"));

        if (category.getParentId() == null) {
            if (categoryMapper.countChildren(id) > 0) {
                throw new BusinessException("该分类下有子分类，无法删除");
            }
        } else {
            if (categoryMapper.countProductsByCategory(id) > 0) {
                throw new BusinessException("该分类下有商品，无法删除");
            }
        }
        categoryMapper.delete(id);
    }

    private CategoryVO toVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setParentId(category.getParentId());
        vo.setSortOrder(category.getSortOrder());
        return vo;
    }
}
