package com.supermarket.inventory.sku.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SkuUsageMapper {

    private final JdbcTemplate jdbcTemplate;

    public SkuUsageMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countBusinessReferences(Long skuId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select
                    (select count(*) from outbound_order where sku_id = ?) +
                    (select count(*) from stock_check where sku_id = ?) +
                    (select count(*) from stock_log where sku_id = ?) +
                    (select count(*) from purchase_inbound_item where sku_id = ?)
                """,
                Long.class,
                skuId,
                skuId,
                skuId,
                skuId
        );
        return count == null ? 0L : count;
    }

    public long countBusinessReferencesByProductId(Long productId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select
                    (select count(*) from outbound_order o where exists (
                        select 1 from sku s where s.product_id = ? and s.id = o.sku_id
                    )) +
                    (select count(*) from stock_check c where exists (
                        select 1 from sku s where s.product_id = ? and s.id = c.sku_id
                    )) +
                    (select count(*) from stock_log l where exists (
                        select 1 from sku s where s.product_id = ? and s.id = l.sku_id
                    )) +
                    (select count(*) from purchase_inbound_item pi where exists (
                        select 1 from sku s where s.product_id = ? and s.id = pi.sku_id
                    ))
                """,
                Long.class,
                productId,
                productId,
                productId,
                productId
        );
        return count == null ? 0L : count;
    }
}
