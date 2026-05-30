package com.supermarket.inventory.report.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ReportMapper {

    private final JdbcTemplate jdbcTemplate;

    public ReportMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> stockSummary() {
        return jdbcTemplate.queryForMap(
                """
                select
                  (select count(*) from product) as productCount,
                  (select count(*) from stock) as stockCount,
                  coalesce(sum(s.quantity), 0) as totalQuantity,
                  sum(case when s.quantity < s.min_stock then 1 else 0 end) as lowWarningCount,
                  sum(case when s.quantity > s.max_stock then 1 else 0 end) as highWarningCount
                from stock s
                """
        );
    }

    public Map<String, Object> inboundSummary() {
        return jdbcTemplate.queryForMap(
                """
                select count(*) as orderCount, coalesce(sum(quantity), 0) as totalQuantity
                from inbound_order
                """
        );
    }

    public Map<String, Object> outboundSummary() {
        return jdbcTemplate.queryForMap(
                """
                select count(*) as orderCount, coalesce(sum(quantity), 0) as totalQuantity
                from outbound_order
                """
        );
    }

    public List<Map<String, Object>> warningStocks() {
        return jdbcTemplate.queryForList(
                """
                select p.product_code as productCode,
                       p.product_name as productName,
                       c.name as category,
                       s.quantity,
                       s.min_stock as minStock,
                       s.max_stock as maxStock,
                       case
                         when s.quantity < s.min_stock then 'LOW'
                         when s.quantity > s.max_stock then 'HIGH'
                         else 'NORMAL'
                       end as warningStatus
                from stock s
                inner join product p on p.id = s.product_id
                left join category c on c.id = p.category_id
                where s.quantity < s.min_stock or s.quantity > s.max_stock
                order by s.update_time desc
                """
        );
    }

    public List<Map<String, Object>> stockTrend() {
        return jdbcTemplate.queryForList(
                """
                select date(create_time) as statDate,
                       change_type as changeType,
                       count(*) as changeCount,
                       coalesce(sum(change_quantity), 0) as changeQuantity
                from stock_log
                group by date(create_time), change_type
                order by statDate desc
                limit 30
                """
        );
    }
}
