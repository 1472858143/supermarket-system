package com.supermarket.inventory.brand.mapper;

import com.supermarket.inventory.brand.entity.Brand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandMapperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private BrandMapper brandMapper;

    @BeforeEach
    void setUp() {
        brandMapper = new BrandMapper(jdbcTemplate);
    }

    @Test
    void findPage_searchesBrandCodeAndNameFiltersStatusAndSortsByNewest() {
        brandMapper.findPage("fresh", 1, 20, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq("%fresh%"),
                eq("%fresh%"),
                eq(1),
                eq(10),
                eq(20)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from brand");
        assertThat(sql).contains("brand_code like ?");
        assertThat(sql).contains("brand_name like ?");
        assertThat(sql).contains("status = ?");
        assertThat(sql).contains("order by id desc");
        assertThat(sql).contains("limit ? offset ?");
    }

    @Test
    void count_usesSameSearchScopeAndOptionalStatusFilter() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object[].class))).thenReturn(0L);

        brandMapper.count("fresh", 0);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                eq(Long.class),
                eq("%fresh%"),
                eq("%fresh%"),
                eq(0)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from brand");
        assertThat(sql).contains("brand_code like ?");
        assertThat(sql).contains("brand_name like ?");
        assertThat(sql).contains("status = ?");
    }

    @Test
    void findPage_omitsStatusFilterWhenStatusIsNull() {
        brandMapper.findPage("fresh", null, 0, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                eq("%fresh%"),
                eq("%fresh%"),
                eq(10),
                eq(0)
        );

        assertThat(sqlCaptor.getValue()).doesNotContain("status = ?");
    }

    @Test
    void findEnabledOptions_returnsEnabledBrandsSortedByNameAndId() {
        brandMapper.findEnabledOptions();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from brand");
        assertThat(sql).contains("where status = 1");
        assertThat(sql).contains("order by brand_name asc, id asc");
    }

    @Test
    void existsByNameExcludingChecksSameNameOutsideCurrentBrand() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object[].class))).thenReturn(0L);

        brandMapper.existsByNameExcluding("Fresh Co", 7L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(
                sqlCaptor.capture(),
                eq(Long.class),
                eq("Fresh Co"),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("from brand");
        assertThat(sql).contains("brand_name = ?");
        assertThat(sql).contains("id <> ?");
    }

    @Test
    void countProductsByBrandIdCountsProductRows() {
        brandMapper.countProductsByBrandId(7L);

        verify(jdbcTemplate).queryForObject(
                "select count(*) from product where brand_id = ?",
                Long.class,
                7L
        );
    }

    @Test
    void update_doesNotModifyBrandCode() {
        Brand brand = new Brand();
        brand.setId(7L);
        brand.setBrandCode("BRD000007");
        brand.setBrandName("Fresh Co");
        brand.setRemark("Main");
        brand.setStatus(1);

        brandMapper.update(brand);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(),
                eq("Fresh Co"),
                eq(1),
                eq("Main"),
                eq(7L)
        );

        String sql = sqlCaptor.getValue();

        assertThat(sql).contains("update brand");
        assertThat(sql).contains("brand_name = ?");
        assertThat(sql).contains("status = ?");
        assertThat(sql).contains("remark = ?");
        assertThat(sql).doesNotContain("brand_code");
    }

    @Test
    void findMaxCode_usesPatternAndMaxBrandCode() {
        brandMapper.findMaxCode("BRD%");

        verify(jdbcTemplate).queryForObject(
                "select max(brand_code) from brand where brand_code like ?",
                String.class,
                "BRD%"
        );
    }
}
