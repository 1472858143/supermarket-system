# Report Statistics Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the upgraded `/reports` module with dashboard, inventory analysis, and purchase analysis while preserving the current shell and legacy report endpoints.

**Architecture:** Backend changes stay inside `com.supermarket.inventory.report`: immutable read models, report-specific query defaults, JDBC mapper SQL, service aggregation, and controller endpoints. Frontend changes keep the current `AdminLayout` shell and replace only the internal report page with scoped report components inspired by `system/pages` report prototypes.

**Tech Stack:** Spring Boot 3.3, Java 21, JdbcTemplate, JUnit 5 + Mockito + AssertJ, Vue 3 Composition API, Vite, Axios, native SVG/CSS charts.

---

## File Structure

Backend files to create:

- `system/backend/src/main/java/com/supermarket/inventory/report/dto/ReportQueryDefaults.java`: report-specific date, expiry threshold, and pagination normalization.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportMetricVO.java`: KPI card read model.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportTrendPointVO.java`: date-based trend point read model.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportWarningVO.java`: low-stock and expiry warning read model.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportDashboardVO.java`: dashboard aggregate read model.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportSummaryVO.java`: inventory summary table row.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportLedgerVO.java`: stock ledger table row.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportBatchVO.java`: batch expiry table row.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseReportOverviewVO.java`: purchase overview aggregate.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseSupplierReportVO.java`: supplier analysis table row.
- `system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseReportDetailVO.java`: purchase receipt batch detail row.
- `system/backend/src/test/java/com/supermarket/inventory/report/dto/ReportQueryDefaultsTest.java`: query default tests.
- `system/backend/src/test/java/com/supermarket/inventory/report/service/ReportServiceTest.java`: service aggregation tests.

Backend files to modify:

- `system/backend/src/main/java/com/supermarket/inventory/report/controller/ReportController.java`: add new GET endpoints and preserve old endpoints.
- `system/backend/src/main/java/com/supermarket/inventory/report/service/ReportService.java`: add dashboard, inventory, and purchase report methods.
- `system/backend/src/main/java/com/supermarket/inventory/report/mapper/ReportMapper.java`: add SQL queries and row mappers.
- `system/backend/src/test/java/com/supermarket/inventory/report/mapper/ReportMapperTest.java`: add SQL-contract tests and update stale inbound summary assertion.

Frontend files to create:

- `system/frontend/src/views/report/reportFormat.js`: report formatting and status helpers.
- `system/frontend/src/views/report/components/ReportKpiCard.vue`: prototype-inspired KPI card.
- `system/frontend/src/views/report/components/ReportTrendChart.vue`: native SVG chart.
- `system/frontend/src/views/report/components/DashboardPanel.vue`: dashboard tab.
- `system/frontend/src/views/report/components/InventoryAnalysisPanel.vue`: inventory analysis tab.
- `system/frontend/src/views/report/components/PurchaseAnalysisPanel.vue`: purchase analysis tab.
- `system/frontend/scripts/verify-report-statistics.mjs`: smoke script for `/reports`.

Frontend files to modify:

- `system/frontend/src/api/report.js`: add new report API functions, keep legacy functions.
- `system/frontend/src/views/report/ReportsView.vue`: replace current simple table page with tabbed report container and scoped internal report styling.

---

### Task 1: Backend Query Defaults and Read Models

**Files:**
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/dto/ReportQueryDefaults.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportMetricVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportTrendPointVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportWarningVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportDashboardVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportSummaryVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportLedgerVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportBatchVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseReportOverviewVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseSupplierReportVO.java`
- Create: `system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseReportDetailVO.java`
- Test: `system/backend/src/test/java/com/supermarket/inventory/report/dto/ReportQueryDefaultsTest.java`

- [ ] **Step 1: Write failing query default tests**

Create `system/backend/src/test/java/com/supermarket/inventory/report/dto/ReportQueryDefaultsTest.java`:

```java
package com.supermarket.inventory.report.dto;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ReportQueryDefaultsTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-04T02:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void normalizeDateRange_defaultsToRecentThirtyDaysIncludingToday() {
        ReportQueryDefaults.DateRange range = ReportQueryDefaults.normalizeDateRange(null, null, clock);

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 5, 6));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 6, 4));
    }

    @Test
    void normalizeDateRange_preservesExplicitRange() {
        ReportQueryDefaults.DateRange range = ReportQueryDefaults.normalizeDateRange(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3),
                clock
        );

        assertThat(range.startDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(range.endDate()).isEqualTo(LocalDate.of(2026, 6, 3));
    }

    @Test
    void normalizeExpiryWarningDays_defaultsInvalidValuesToThirty() {
        assertThat(ReportQueryDefaults.normalizeExpiryWarningDays(null)).isEqualTo(30);
        assertThat(ReportQueryDefaults.normalizeExpiryWarningDays(0)).isEqualTo(30);
        assertThat(ReportQueryDefaults.normalizeExpiryWarningDays(366)).isEqualTo(30);
        assertThat(ReportQueryDefaults.normalizeExpiryWarningDays(60)).isEqualTo(60);
    }

    @Test
    void normalizePageDefaultsToOneAndPageSizeTwentyWithMaxOneHundred() {
        assertThat(ReportQueryDefaults.normalizePage(null)).isEqualTo(1);
        assertThat(ReportQueryDefaults.normalizePage(-2)).isEqualTo(1);
        assertThat(ReportQueryDefaults.normalizePage(3)).isEqualTo(3);
        assertThat(ReportQueryDefaults.normalizePageSize(null)).isEqualTo(20);
        assertThat(ReportQueryDefaults.normalizePageSize(0)).isEqualTo(20);
        assertThat(ReportQueryDefaults.normalizePageSize(200)).isEqualTo(100);
        assertThat(ReportQueryDefaults.offset(3, 20)).isEqualTo(40);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run from `system/backend`:

```bash
mvn test -Dtest=ReportQueryDefaultsTest
```

Expected: FAIL with `cannot find symbol` for `ReportQueryDefaults`.

- [ ] **Step 3: Implement query defaults**

Create `system/backend/src/main/java/com/supermarket/inventory/report/dto/ReportQueryDefaults.java`:

```java
package com.supermarket.inventory.report.dto;

import java.time.Clock;
import java.time.LocalDate;

public final class ReportQueryDefaults {

    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_EXPIRY_WARNING_DAYS = 30;
    private static final int MAX_EXPIRY_WARNING_DAYS = 365;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private ReportQueryDefaults() {
    }

    public record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    public static DateRange normalizeDateRange(LocalDate startDate, LocalDate endDate, Clock clock) {
        LocalDate normalizedEnd = endDate == null ? LocalDate.now(clock) : endDate;
        LocalDate normalizedStart = startDate == null ? normalizedEnd.minusDays(DEFAULT_DAYS - 1) : startDate;
        if (normalizedStart.isAfter(normalizedEnd)) {
            return new DateRange(normalizedEnd, normalizedStart);
        }
        return new DateRange(normalizedStart, normalizedEnd);
    }

    public static int normalizeExpiryWarningDays(Integer expiryWarningDays) {
        if (expiryWarningDays == null || expiryWarningDays < 1 || expiryWarningDays > MAX_EXPIRY_WARNING_DAYS) {
            return DEFAULT_EXPIRY_WARNING_DAYS;
        }
        return expiryWarningDays;
    }

    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}
```

- [ ] **Step 4: Add read model records**

Use Java records for report-only immutable read models. Create these files with the exact fields below.

`system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportMetricVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.math.BigDecimal;

public record ReportMetricVO(
        String key,
        String label,
        BigDecimal amountValue,
        Long numberValue,
        String unit,
        String helperText,
        String level
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportTrendPointVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportTrendPointVO(
        LocalDate statDate,
        BigDecimal amountValue,
        BigDecimal plannedAmountValue,
        Long quantityValue
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportWarningVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.time.LocalDate;

public record ReportWarningVO(
        String warningType,
        String warningStatus,
        Long skuId,
        String skuCode,
        String skuName,
        String productCode,
        String productName,
        String category,
        Long currentQuantity,
        Long minStock,
        Long maxStock,
        String batchNo,
        LocalDate expireDate,
        Integer remainingDays
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/ReportDashboardVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.util.List;

public record ReportDashboardVO(
        List<ReportMetricVO> metrics,
        List<ReportTrendPointVO> purchaseAmountTrend,
        List<ReportTrendPointVO> outboundQuantityTrend,
        List<ReportWarningVO> lowStockWarnings,
        List<ReportWarningVO> expiryWarnings
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportSummaryVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.time.LocalDateTime;

public record InventoryReportSummaryVO(
        Long skuId,
        String skuCode,
        String skuName,
        String spec,
        String baseUnit,
        String productCode,
        String productName,
        String category,
        Long totalQuantity,
        Long availableQuantity,
        Long lockedQuantity,
        Long expiredQuantity,
        Long minStock,
        Long maxStock,
        String stockStatus,
        LocalDateTime updateTime
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportLedgerVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.time.LocalDateTime;

public record InventoryReportLedgerVO(
        Long id,
        Long skuId,
        String skuCode,
        String skuName,
        String productCode,
        String productName,
        String category,
        String changeType,
        Long changeQuantity,
        Long beforeQuantity,
        Long afterQuantity,
        LocalDateTime createTime
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/InventoryReportBatchVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventoryReportBatchVO(
        Long id,
        String batchNo,
        Long skuId,
        String skuCode,
        String skuName,
        String productCode,
        String productName,
        Long initialQuantity,
        Long quantity,
        String status,
        String expiryStatus,
        LocalDate productionDate,
        LocalDate expireDate,
        Integer remainingDays,
        BigDecimal purchasePrice,
        BigDecimal costPrice,
        LocalDateTime createTime
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseReportOverviewVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.math.BigDecimal;
import java.util.List;

public record PurchaseReportOverviewVO(
        BigDecimal actualAmount,
        BigDecimal plannedAmount,
        Long actualBaseQuantity,
        Long purchaseOrderCount,
        Long receiptCount,
        Long pendingOrderCount,
        Long partiallyInboundedOrderCount,
        Long inboundedOrderCount,
        List<ReportTrendPointVO> purchaseAmountTrend
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseSupplierReportVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PurchaseSupplierReportVO(
        Long supplierId,
        String supplierCode,
        String supplierName,
        BigDecimal actualAmount,
        BigDecimal plannedAmount,
        Long receiptCount,
        Long skuCount,
        BigDecimal amountRatio,
        LocalDateTime lastReceiptTime
) {
}
```

`system/backend/src/main/java/com/supermarket/inventory/report/vo/PurchaseReportDetailVO.java`:

```java
package com.supermarket.inventory.report.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseReportDetailVO(
        Long receiptBatchId,
        String receiptNo,
        String orderNo,
        Long supplierId,
        String supplierCode,
        String supplierName,
        Long skuId,
        String skuCode,
        String skuName,
        String productCode,
        String productName,
        Long quantity,
        Long baseQuantity,
        BigDecimal amount,
        BigDecimal purchasePriceSnapshot,
        BigDecimal costPriceSnapshot,
        LocalDate productionDate,
        LocalDate expireDate,
        LocalDateTime receiptTime
) {
}
```

- [ ] **Step 5: Run query default test**

Run from `system/backend`:

```bash
mvn test -Dtest=ReportQueryDefaultsTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add system/backend/src/main/java/com/supermarket/inventory/report/dto/ReportQueryDefaults.java system/backend/src/main/java/com/supermarket/inventory/report/vo system/backend/src/test/java/com/supermarket/inventory/report/dto/ReportQueryDefaultsTest.java
git commit -m "feat(report): add report query defaults and read models"
```

---

### Task 2: Report Mapper SQL Contracts

**Files:**
- Modify: `system/backend/src/test/java/com/supermarket/inventory/report/mapper/ReportMapperTest.java`
- Modify: `system/backend/src/main/java/com/supermarket/inventory/report/mapper/ReportMapper.java`

- [ ] **Step 1: Replace stale inbound summary assertion**

In `ReportMapperTest.inboundSummary_sumsPurchaseInboundTotalQuantity`, update the expected field from the removed historical column to the current column:

```java
assertThat(sql).contains("sum(inbound_total_quantity)");
assertThat(sql).doesNotContain("sum(total_quantity)");
```

- [ ] **Step 2: Add failing SQL-contract tests for dashboard queries**

Add tests to `ReportMapperTest`:

```java
@Test
void dashboardMetrics_usesActualReceiptAmountAndPlannedAmount() {
    reportMapper.dashboardMetrics(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 30);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).queryForMap(sqlCaptor.capture(), eq(LocalDate.of(2026, 6, 1)),
            eq(LocalDate.of(2026, 6, 30)), eq(30));

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("purchase_inbound_receipt");
    assertThat(sql).contains("sum(pir.total_amount)");
    assertThat(sql).contains("planned_total_amount");
    assertThat(sql).contains("stock_batch");
    assertThat(sql).contains("expire_date");
}

@Test
void purchaseAmountTrend_usesActualReceiptsAndPlannedAuxiliaryLine() {
    reportMapper.purchaseAmountTrend(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(LocalDate.of(2026, 6, 1)),
            eq(LocalDate.of(2026, 6, 30)), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30)));

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("purchase_inbound_receipt");
    assertThat(sql).contains("sum(pir.total_amount)");
    assertThat(sql).contains("sum(pi.planned_total_amount)");
}

@Test
void outboundQuantityTrend_usesOutboundBaseQuantity() {
    reportMapper.outboundQuantityTrend(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class),
            eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30)));

    assertThat(sqlCaptor.getValue()).contains("from outbound_order");
    assertThat(sqlCaptor.getValue()).contains("sum(base_quantity)");
}
```

Add imports:

```java
import org.springframework.jdbc.core.RowMapper;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
```

- [ ] **Step 3: Add failing SQL-contract tests for inventory and purchase table queries**

Add tests to `ReportMapperTest`:

```java
@Test
void inventoryLedger_queriesStockLogOnlyForLedger() {
    reportMapper.findInventoryLedger(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "OUTBOUND", "milk", 0, 20);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("from stock_log sl");
    assertThat(sql).contains("inner join sku k on k.id = sl.sku_id");
    assertThat(sql).doesNotContain("stock_batch_log");
}

@Test
void inventoryBatches_queriesStockBatchExpireDate() {
    reportMapper.findInventoryBatches(30, "NEAR_EXPIRY", "milk", 0, 20);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any(), any(), anyInt(), anyInt());

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("from stock_batch b");
    assertThat(sql).contains("b.expire_date");
    assertThat(sql).contains("b.quantity > 0");
}

@Test
void purchaseSuppliers_joinsReceiptsPurchaseInboundAndSupplier() {
    reportMapper.findPurchaseSuppliers(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 0, 20);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), anyInt(), anyInt());

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("purchase_inbound_receipt pir");
    assertThat(sql).contains("inner join purchase_inbound pi on pi.id = pir.purchase_inbound_id");
    assertThat(sql).contains("inner join supplier s on s.id = pi.supplier_id");
}

@Test
void purchaseDetails_usesReceiptBatchAsDetailSource() {
    reportMapper.findPurchaseDetails(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 3L, "milk", 0, 20);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("from purchase_inbound_receipt_batch rb");
    assertThat(sql).contains("inner join purchase_inbound_receipt pir on pir.id = rb.receipt_id");
    assertThat(sql).contains("inner join purchase_inbound pi on pi.id = rb.purchase_inbound_id");
}
```

Add import:

```java
import static org.mockito.ArgumentMatchers.anyInt;
```

- [ ] **Step 4: Run mapper tests to verify they fail**

Run from `system/backend`:

```bash
mvn test -Dtest=ReportMapperTest
```

Expected: FAIL with missing `ReportMapper` methods.

- [ ] **Step 5: Implement mapper method signatures and row mappers**

Modify `ReportMapper.java`. Add imports:

```java
import com.supermarket.inventory.report.vo.InventoryReportBatchVO;
import com.supermarket.inventory.report.vo.InventoryReportLedgerVO;
import com.supermarket.inventory.report.vo.InventoryReportSummaryVO;
import com.supermarket.inventory.report.vo.PurchaseReportDetailVO;
import com.supermarket.inventory.report.vo.PurchaseSupplierReportVO;
import com.supermarket.inventory.report.vo.ReportTrendPointVO;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
```

Add helper methods:

```java
private LocalDate toLocalDate(Date date) {
    return date == null ? null : date.toLocalDate();
}

private LocalDateTime toLocalDateTime(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
}

private BigDecimal decimal(Object value) {
    return value == null ? BigDecimal.ZERO : (BigDecimal) value;
}

private String like(String keyword) {
    return "%" + (keyword == null ? "" : keyword.trim()) + "%";
}
```

Add row mappers for the new VO records. The implementation must map SQL aliases exactly to record constructor arguments. Example for trend:

```java
private final RowMapper<ReportTrendPointVO> trendPointRowMapper = (rs, rowNum) -> new ReportTrendPointVO(
        toLocalDate(rs.getDate("statDate")),
        rs.getBigDecimal("amountValue"),
        rs.getBigDecimal("plannedAmountValue"),
        rs.getObject("quantityValue", Long.class)
);
```

Implement all mapper methods named in the tests. Add methods with these signatures:

```java
public Map<String, Object> dashboardMetrics(LocalDate startDate, LocalDate endDate, int expiryWarningDays)
public List<ReportTrendPointVO> purchaseAmountTrend(LocalDate startDate, LocalDate endDate)
public List<ReportTrendPointVO> outboundQuantityTrend(LocalDate startDate, LocalDate endDate)
public List<Map<String, Object>> lowStockWarnings(int limit)
public List<Map<String, Object>> expiryWarnings(int expiryWarningDays, int limit)
public long countInventorySummary(String keyword, String category, String stockStatus)
public List<InventoryReportSummaryVO> findInventorySummary(String keyword, String category, String stockStatus, int offset, int pageSize)
public long countInventoryLedger(LocalDate startDate, LocalDate endDate, String changeType, String keyword)
public List<InventoryReportLedgerVO> findInventoryLedger(LocalDate startDate, LocalDate endDate, String changeType, String keyword, int offset, int pageSize)
public long countInventoryBatches(int expiryWarningDays, String status, String keyword)
public List<InventoryReportBatchVO> findInventoryBatches(int expiryWarningDays, String status, String keyword, int offset, int pageSize)
public Map<String, Object> purchaseOverview(LocalDate startDate, LocalDate endDate)
public long countPurchaseSuppliers(LocalDate startDate, LocalDate endDate)
public List<PurchaseSupplierReportVO> findPurchaseSuppliers(LocalDate startDate, LocalDate endDate, int offset, int pageSize)
public long countPurchaseDetails(LocalDate startDate, LocalDate endDate, Long supplierId, String keyword)
public List<PurchaseReportDetailVO> findPurchaseDetails(LocalDate startDate, LocalDate endDate, Long supplierId, String keyword, int offset, int pageSize)
```

Use these SQL source rules:

- Actual purchase amount: `purchase_inbound_receipt pir`, `sum(pir.total_amount)`.
- Planned purchase amount: `purchase_inbound pi`, `sum(pi.planned_total_amount)`.
- Outbound trend quantity: `outbound_order`, `sum(base_quantity)`.
- Ledger source: `stock_log sl`.
- Batch expiry source: `stock_batch b`, `b.expire_date`, `b.quantity > 0`.
- Supplier analysis source: `purchase_inbound_receipt pir inner join purchase_inbound pi inner join supplier s`.
- Purchase detail source: `purchase_inbound_receipt_batch rb`.

- [ ] **Step 6: Run mapper tests**

Run from `system/backend`:

```bash
mvn test -Dtest=ReportMapperTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add system/backend/src/main/java/com/supermarket/inventory/report/mapper/ReportMapper.java system/backend/src/test/java/com/supermarket/inventory/report/mapper/ReportMapperTest.java
git commit -m "feat(report): add report SQL queries"
```

---

### Task 3: Report Service Aggregation and Controller Endpoints

**Files:**
- Create: `system/backend/src/test/java/com/supermarket/inventory/report/service/ReportServiceTest.java`
- Modify: `system/backend/src/main/java/com/supermarket/inventory/report/service/ReportService.java`
- Modify: `system/backend/src/main/java/com/supermarket/inventory/report/controller/ReportController.java`

- [ ] **Step 1: Write failing service tests**

Create `ReportServiceTest.java`:

```java
package com.supermarket.inventory.report.service;

import com.supermarket.inventory.report.mapper.ReportMapper;
import com.supermarket.inventory.report.vo.ReportDashboardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportMapper reportMapper;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportMapper);
    }

    @Test
    void dashboard_defaultsExpiryDaysAndBuildsMetricCards() {
        when(reportMapper.dashboardMetrics(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 30))
                .thenReturn(Map.of(
                        "actualPurchaseAmount", BigDecimal.valueOf(1200),
                        "plannedPurchaseAmount", BigDecimal.valueOf(1500),
                        "outboundQuantity", 80L,
                        "stockSkuCount", 20L,
                        "lowStockCount", 3L,
                        "nearExpiryBatchCount", 2L,
                        "expiredBatchCount", 1L
                ));
        when(reportMapper.purchaseAmountTrend(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of());
        when(reportMapper.outboundQuantityTrend(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of());
        when(reportMapper.lowStockWarnings(10)).thenReturn(List.of());
        when(reportMapper.expiryWarnings(30, 10)).thenReturn(List.of());

        ReportDashboardVO dashboard = reportService.dashboard(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null
        );

        assertThat(dashboard.metrics()).hasSize(5);
        assertThat(dashboard.metrics().get(0).key()).isEqualTo("actualPurchaseAmount");
        assertThat(dashboard.metrics().get(0).amountValue()).isEqualByComparingTo("1200");
        verify(reportMapper).expiryWarnings(30, 10);
    }

    @Test
    void inventorySummaryUsesReportPaginationDefaultTwenty() {
        when(reportMapper.countInventorySummary("", null, null)).thenReturn(0L);
        when(reportMapper.findInventorySummary("", null, null, 0, 20)).thenReturn(List.of());

        var result = reportService.inventorySummary("", null, null, null, null);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(20);
        verify(reportMapper).findInventorySummary("", null, null, 0, 20);
    }
}
```

- [ ] **Step 2: Run service tests to verify they fail**

Run from `system/backend`:

```bash
mvn test -Dtest=ReportServiceTest
```

Expected: FAIL with missing service methods.

- [ ] **Step 3: Implement service aggregation methods**

Modify `ReportService.java`. Keep existing methods unchanged for legacy endpoints. Add imports for `PageResult`, `ReportQueryDefaults`, VO records, `BigDecimal`, and `LocalDate`.

Add public methods with these signatures:

```java
public ReportDashboardVO dashboard(LocalDate startDate, LocalDate endDate, Integer expiryWarningDays)
public PageResult<InventoryReportSummaryVO> inventorySummary(String keyword, String category, String stockStatus, Integer page, Integer pageSize)
public PageResult<InventoryReportLedgerVO> inventoryLedger(LocalDate startDate, LocalDate endDate, String changeType, String keyword, Integer page, Integer pageSize)
public PageResult<InventoryReportBatchVO> inventoryBatches(Integer expiryWarningDays, String status, String keyword, Integer page, Integer pageSize)
public PurchaseReportOverviewVO purchaseOverview(LocalDate startDate, LocalDate endDate)
public PageResult<PurchaseSupplierReportVO> purchaseSuppliers(LocalDate startDate, LocalDate endDate, Integer page, Integer pageSize)
public PageResult<PurchaseReportDetailVO> purchaseDetails(LocalDate startDate, LocalDate endDate, Long supplierId, String keyword, Integer page, Integer pageSize)
```

Use `ReportQueryDefaults.normalizePageSize`, not global `PageUtils.normalizePageSize`, because report pagination default is 20.

Build dashboard metrics in this exact order:

```java
List<ReportMetricVO> metrics = List.of(
        new ReportMetricVO("actualPurchaseAmount", "实际采购金额", decimal(data.get("actualPurchaseAmount")), null, "元", "计划 " + decimal(data.get("plannedPurchaseAmount")), "info"),
        new ReportMetricVO("outboundQuantity", "出库总数量", null, number(data.get("outboundQuantity")), "基础单位", "按出库基础数量汇总", "success"),
        new ReportMetricVO("stockSkuCount", "库存 SKU", null, number(data.get("stockSkuCount")), "个", "低库存 " + number(data.get("lowStockCount")), "warning"),
        new ReportMetricVO("nearExpiryBatchCount", "临期批次", null, number(data.get("nearExpiryBatchCount")), "批", expiryDays + " 天内到期", "warning"),
        new ReportMetricVO("expiredBatchCount", "过期批次", null, number(data.get("expiredBatchCount")), "批", "需优先处理", "danger")
);
```

Add private conversion helpers:

```java
private BigDecimal decimal(Object value) {
    return value instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO;
}

private Long number(Object value) {
    return value instanceof Number number ? number.longValue() : 0L;
}
```

- [ ] **Step 4: Add controller endpoints**

Modify `ReportController.java`. Add imports:

```java
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.report.vo.InventoryReportBatchVO;
import com.supermarket.inventory.report.vo.InventoryReportLedgerVO;
import com.supermarket.inventory.report.vo.InventoryReportSummaryVO;
import com.supermarket.inventory.report.vo.PurchaseReportDetailVO;
import com.supermarket.inventory.report.vo.PurchaseReportOverviewVO;
import com.supermarket.inventory.report.vo.PurchaseSupplierReportVO;
import com.supermarket.inventory.report.vo.ReportDashboardVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
```

Add endpoints:

```java
@GetMapping("/dashboard")
public ApiResponse<ReportDashboardVO> dashboard(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Integer expiryWarningDays
) {
    return ApiResponse.success(reportService.dashboard(startDate, endDate, expiryWarningDays));
}

@GetMapping("/inventory/summary")
public ApiResponse<PageResult<InventoryReportSummaryVO>> inventorySummary(
        @RequestParam(required = false, defaultValue = "") String keyword,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String stockStatus,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer pageSize
) {
    return ApiResponse.success(reportService.inventorySummary(keyword, category, stockStatus, page, pageSize));
}

@GetMapping("/inventory/ledger")
public ApiResponse<PageResult<InventoryReportLedgerVO>> inventoryLedger(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String changeType,
        @RequestParam(required = false, defaultValue = "") String keyword,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer pageSize
) {
    return ApiResponse.success(reportService.inventoryLedger(startDate, endDate, changeType, keyword, page, pageSize));
}

@GetMapping("/inventory/batches")
public ApiResponse<PageResult<InventoryReportBatchVO>> inventoryBatches(
        @RequestParam(required = false) Integer expiryWarningDays,
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "") String keyword,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer pageSize
) {
    return ApiResponse.success(reportService.inventoryBatches(expiryWarningDays, status, keyword, page, pageSize));
}

@GetMapping("/purchase/overview")
public ApiResponse<PurchaseReportOverviewVO> purchaseOverview(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
) {
    return ApiResponse.success(reportService.purchaseOverview(startDate, endDate));
}

@GetMapping("/purchase/suppliers")
public ApiResponse<PageResult<PurchaseSupplierReportVO>> purchaseSuppliers(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer pageSize
) {
    return ApiResponse.success(reportService.purchaseSuppliers(startDate, endDate, page, pageSize));
}

@GetMapping("/purchase/details")
public ApiResponse<PageResult<PurchaseReportDetailVO>> purchaseDetails(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Long supplierId,
        @RequestParam(required = false, defaultValue = "") String keyword,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer pageSize
) {
    return ApiResponse.success(reportService.purchaseDetails(startDate, endDate, supplierId, keyword, page, pageSize));
}
```

- [ ] **Step 5: Run service and mapper tests**

Run from `system/backend`:

```bash
mvn test -Dtest=ReportServiceTest,ReportMapperTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add system/backend/src/main/java/com/supermarket/inventory/report/controller/ReportController.java system/backend/src/main/java/com/supermarket/inventory/report/service/ReportService.java system/backend/src/test/java/com/supermarket/inventory/report/service/ReportServiceTest.java
git commit -m "feat(report): expose report analysis endpoints"
```

---

### Task 4: Frontend API and Formatting Utilities

**Files:**
- Modify: `system/frontend/src/api/report.js`
- Create: `system/frontend/src/views/report/reportFormat.js`

- [ ] **Step 1: Add report API functions**

Modify `system/frontend/src/api/report.js`. Keep existing exports and append:

```javascript
export function getReportDashboard(params) {
  return request.get('/reports/dashboard', { params })
}

export function getInventoryReportSummary(params) {
  return request.get('/reports/inventory/summary', { params })
}

export function getInventoryReportLedger(params) {
  return request.get('/reports/inventory/ledger', { params })
}

export function getInventoryReportBatches(params) {
  return request.get('/reports/inventory/batches', { params })
}

export function getPurchaseReportOverview(params) {
  return request.get('/reports/purchase/overview', { params })
}

export function getPurchaseReportSuppliers(params) {
  return request.get('/reports/purchase/suppliers', { params })
}

export function getPurchaseReportDetails(params) {
  return request.get('/reports/purchase/details', { params })
}
```

- [ ] **Step 2: Add formatting helpers**

Create `system/frontend/src/views/report/reportFormat.js`:

```javascript
export function formatNumber(value) {
  const number = Number(value ?? 0)
  return Number.isFinite(number) ? number.toLocaleString('zh-CN') : '0'
}

export function formatMoney(value) {
  const number = Number(value ?? 0)
  if (!Number.isFinite(number)) {
    return '¥ 0.00'
  }
  return `¥ ${number.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export function formatDate(value) {
  if (!value) {
    return '-'
  }
  return String(value).slice(0, 10)
}

export function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

export function normalizePageResult(value) {
  return {
    items: Array.isArray(value?.items) ? value.items : [],
    total: Number(value?.total ?? 0),
    page: Number(value?.page ?? 1),
    pageSize: Number(value?.pageSize ?? 20)
  }
}

export function stockStatusLabel(value) {
  const labels = {
    OUT: '已售罄',
    LOW: '低库存',
    HIGH: '超上限',
    NORMAL: '正常'
  }
  return labels[value] || value || '-'
}

export function expiryStatusLabel(value) {
  const labels = {
    EXPIRED: '过期',
    NEAR_EXPIRY: '临期',
    NORMAL: '正常'
  }
  return labels[value] || value || '-'
}

export function warningClass(value) {
  if (['EXPIRED', 'HIGH', 'OUT'].includes(value)) {
    return 'danger'
  }
  if (['NEAR_EXPIRY', 'LOW'].includes(value)) {
    return 'warn'
  }
  return 'ok'
}

export function dateRangeByKey(key) {
  const end = new Date()
  const start = new Date(end)
  if (key === '7d') {
    start.setDate(end.getDate() - 6)
  } else if (key === 'quarter') {
    start.setDate(end.getDate() - 89)
  } else if (key === 'year') {
    start.setDate(end.getDate() - 364)
  } else {
    start.setDate(end.getDate() - 29)
  }
  return {
    startDate: start.toISOString().slice(0, 10),
    endDate: end.toISOString().slice(0, 10)
  }
}
```

- [ ] **Step 3: Run frontend build**

Run from `system/frontend`:

```bash
npm run build
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add system/frontend/src/api/report.js system/frontend/src/views/report/reportFormat.js
git commit -m "feat(report): add report frontend API utilities"
```

---

### Task 5: Shared Report UI Components

**Files:**
- Create: `system/frontend/src/views/report/components/ReportKpiCard.vue`
- Create: `system/frontend/src/views/report/components/ReportTrendChart.vue`

- [ ] **Step 1: Create KPI card component**

Create `ReportKpiCard.vue`:

```vue
<template>
  <article class="report-kpi" :style="{ '--accent': accent }">
    <div class="label">{{ label }}</div>
    <div class="value">
      <span v-if="amount">{{ amount }}</span>
      <span v-else>{{ value }}</span>
      <span class="unit">{{ unit }}</span>
    </div>
    <div class="delta" :class="level">{{ helperText }}</div>
    <div class="icon-wrap" aria-hidden="true">
      <slot name="icon">◆</slot>
    </div>
  </article>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: String, default: '0' },
  amount: { type: String, default: '' },
  unit: { type: String, default: '' },
  helperText: { type: String, default: '' },
  level: { type: String, default: 'info' },
  accent: { type: String, default: '#4d9bff' }
})
</script>
```

- [ ] **Step 2: Create trend chart component**

Create `ReportTrendChart.vue`:

```vue
<template>
  <div class="report-chart-host">
    <div v-if="!points.length" class="report-empty">暂无趋势数据</div>
    <svg v-else class="report-trend-chart" viewBox="0 0 720 260" preserveAspectRatio="none">
      <line
        v-for="line in gridLines"
        :key="line"
        class="grid-line"
        x1="36"
        x2="700"
        :y1="line"
        :y2="line"
      />
      <path v-if="areaPath" class="area-main" :d="areaPath" />
      <path class="line-main" :d="mainPath" />
      <path v-if="subPath" class="line-sub" :d="subPath" />
      <text x="36" y="246">{{ firstLabel }}</text>
      <text x="650" y="246">{{ lastLabel }}</text>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  points: { type: Array, default: () => [] },
  mainKey: { type: String, default: 'amountValue' },
  subKey: { type: String, default: '' },
  labelKey: { type: String, default: 'statDate' }
})

const gridLines = [38, 92, 146, 200]
const chart = { left: 36, right: 700, top: 30, bottom: 220 }

const maxValue = computed(() => {
  const values = props.points.flatMap((point) => [
    Number(point[props.mainKey] ?? 0),
    props.subKey ? Number(point[props.subKey] ?? 0) : 0
  ])
  return Math.max(...values, 1)
})

function x(index) {
  if (props.points.length === 1) {
    return chart.left
  }
  return chart.left + ((chart.right - chart.left) * index) / (props.points.length - 1)
}

function y(value) {
  return chart.bottom - ((chart.bottom - chart.top) * Number(value ?? 0)) / maxValue.value
}

function pathFor(key) {
  if (!key || !props.points.length) {
    return ''
  }
  return props.points.map((point, index) => `${index === 0 ? 'M' : 'L'}${x(index).toFixed(1)},${y(point[key]).toFixed(1)}`).join(' ')
}

const mainPath = computed(() => pathFor(props.mainKey))
const subPath = computed(() => pathFor(props.subKey))
const areaPath = computed(() => {
  if (!mainPath.value || !props.points.length) {
    return ''
  }
  return `${mainPath.value} L${x(props.points.length - 1).toFixed(1)},${chart.bottom} L${chart.left},${chart.bottom} Z`
})
const firstLabel = computed(() => props.points[0]?.[props.labelKey] || '')
const lastLabel = computed(() => props.points[props.points.length - 1]?.[props.labelKey] || '')
</script>
```

- [ ] **Step 3: Add component styles in `ReportsView.vue` later, not inside components**

Keep these components style-light. The page container will own scoped report styles so the prototype-inspired theme is centralized.

- [ ] **Step 4: Run frontend build**

Run from `system/frontend`:

```bash
npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add system/frontend/src/views/report/components/ReportKpiCard.vue system/frontend/src/views/report/components/ReportTrendChart.vue
git commit -m "feat(report): add report UI primitives"
```

---

### Task 6: Dashboard, Inventory, and Purchase Panels

**Files:**
- Create: `system/frontend/src/views/report/components/DashboardPanel.vue`
- Create: `system/frontend/src/views/report/components/InventoryAnalysisPanel.vue`
- Create: `system/frontend/src/views/report/components/PurchaseAnalysisPanel.vue`

- [ ] **Step 1: Create dashboard panel**

Create `DashboardPanel.vue`. It receives `dashboard`, `loading`, and `expiryWarningDays` from the parent and renders:

```vue
<template>
  <div class="report-panel">
    <section class="report-kpi-row">
      <ReportKpiCard
        v-for="metric in metrics"
        :key="metric.key"
        :label="metric.label"
        :amount="metric.amountValue != null ? formatMoney(metric.amountValue) : ''"
        :value="metric.numberValue != null ? formatNumber(metric.numberValue) : '0'"
        :unit="metric.unit"
        :helper-text="metric.helperText"
        :level="metric.level"
        :accent="accentFor(metric.level)"
      />
    </section>

    <section class="report-grid-2">
      <article class="report-card">
        <div class="report-card-head">
          <div class="title-block"><h3>采购金额趋势</h3><div class="sub">ACTUAL VS PLANNED</div></div>
        </div>
        <ReportTrendChart :points="dashboard.purchaseAmountTrend || []" main-key="amountValue" sub-key="plannedAmountValue" />
      </article>
      <article class="report-card">
        <div class="report-card-head">
          <div class="title-block"><h3>出库数量趋势</h3><div class="sub">OUTBOUND QUANTITY</div></div>
        </div>
        <ReportTrendChart :points="dashboard.outboundQuantityTrend || []" main-key="quantityValue" />
      </article>
    </section>

    <section class="report-grid-2">
      <article class="report-card">
        <div class="report-card-head"><div class="title-block"><h3>低库存预警</h3><div class="sub">REALTIME LOW STOCK</div></div></div>
        <div v-if="!(dashboard.lowStockWarnings || []).length" class="report-empty">暂无低库存预警</div>
        <div v-else class="report-warning-list">
          <div v-for="item in dashboard.lowStockWarnings" :key="item.skuCode" class="report-warning-item">
            <div><b>{{ item.productName }}</b><span>{{ item.skuCode }} · 当前 {{ formatNumber(item.currentQuantity) }} / 下限 {{ formatNumber(item.minStock) }}</span></div>
            <span class="report-pill warn">低库存</span>
          </div>
        </div>
      </article>
      <article class="report-card">
        <div class="report-card-head"><div class="title-block"><h3>临期 / 过期预警</h3><div class="sub">{{ expiryWarningDays }} DAYS EXPIRY WARNING</div></div></div>
        <div v-if="!(dashboard.expiryWarnings || []).length" class="report-empty">暂无临期或过期批次</div>
        <div v-else class="report-warning-list">
          <div v-for="item in dashboard.expiryWarnings" :key="item.batchNo" class="report-warning-item">
            <div><b>{{ item.productName }}</b><span>{{ item.batchNo }} · {{ formatDate(item.expireDate) }}</span></div>
            <span class="report-pill" :class="warningClass(item.warningStatus)">{{ expiryStatusLabel(item.warningStatus) }}</span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
```

Use imports:

```javascript
import ReportKpiCard from './ReportKpiCard.vue'
import ReportTrendChart from './ReportTrendChart.vue'
import { expiryStatusLabel, formatDate, formatMoney, formatNumber, warningClass } from '../reportFormat'
```

- [ ] **Step 2: Create inventory analysis panel**

Create `InventoryAnalysisPanel.vue` with secondary tabs:

```javascript
const tabs = [
  { key: 'summary', label: '库存汇总' },
  { key: 'ledger', label: '库存流水' },
  { key: 'batches', label: '批次效期' }
]
```

The component owns local page state for each tab:

```javascript
const activeTab = ref('summary')
const query = reactive({
  keyword: '',
  category: '',
  stockStatus: '',
  changeType: '',
  batchStatus: '',
  page: 1,
  pageSize: 20
})
```

Use API functions:

```javascript
import {
  getInventoryReportBatches,
  getInventoryReportLedger,
  getInventoryReportSummary
} from '../../../api/report'
```

Render each tab with a `report-card`, a compact filter bar, and a plain table using `report-table-host` + `report-table`. Do not reuse global `BaseTable` here because the visual requirement is to match the prototype table style inside the report page.

- [ ] **Step 3: Create purchase analysis panel**

Create `PurchaseAnalysisPanel.vue` with secondary tabs:

```javascript
const tabs = [
  { key: 'overview', label: '采购概况' },
  { key: 'suppliers', label: '供应商分析' },
  { key: 'details', label: '采购明细' }
]
```

Use API functions:

```javascript
import {
  getPurchaseReportDetails,
  getPurchaseReportOverview,
  getPurchaseReportSuppliers
} from '../../../api/report'
```

For `overview`, render actual amount, planned amount, actual quantity, receipt count, and purchase amount trend. For `suppliers` and `details`, render paged report tables.

- [ ] **Step 4: Run frontend build**

Run from `system/frontend`:

```bash
npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add system/frontend/src/views/report/components/DashboardPanel.vue system/frontend/src/views/report/components/InventoryAnalysisPanel.vue system/frontend/src/views/report/components/PurchaseAnalysisPanel.vue
git commit -m "feat(report): add report analysis panels"
```

---

### Task 7: Reports Page Container and Scoped Prototype-Inspired Styles

**Files:**
- Modify: `system/frontend/src/views/report/ReportsView.vue`

- [ ] **Step 1: Replace page template with tabbed container**

Replace current `ReportsView.vue` with a container that keeps the system shell intact and applies report-only internal styles:

```vue
<template>
  <div class="report-page">
    <div class="page-header report-page-head">
      <div>
        <h1 class="page-title">报表统计</h1>
        <p class="page-desc">数据看板 · 库存分析 · 采购分析</p>
      </div>
      <div class="report-actions">
        <div class="report-date-range">
          <button v-for="item in rangeOptions" :key="item.key" type="button" :class="{ on: rangeKey === item.key }" @click="setRange(item.key)">
            {{ item.label }}
          </button>
        </div>
        <select v-model.number="expiryWarningDays" class="select report-threshold" @change="reloadCurrent">
          <option :value="7">临期 7 天</option>
          <option :value="15">临期 15 天</option>
          <option :value="30">临期 30 天</option>
          <option :value="60">临期 60 天</option>
        </select>
        <button class="btn btn-primary" type="button" :disabled="loading" @click="reloadCurrent">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div v-if="message" class="message message-error">{{ message }}</div>

    <nav class="report-sub-tabs">
      <button v-for="tab in tabs" :key="tab.key" type="button" :class="{ on: activeTab === tab.key }" @click="activeTab = tab.key">
        {{ tab.label }}
      </button>
    </nav>

    <DashboardPanel
      v-if="activeTab === 'dashboard'"
      :dashboard="dashboard"
      :loading="loading"
      :expiry-warning-days="expiryWarningDays"
    />
    <InventoryAnalysisPanel
      v-else-if="activeTab === 'inventory'"
      :date-range="dateRange"
      :expiry-warning-days="expiryWarningDays"
    />
    <PurchaseAnalysisPanel
      v-else
      :date-range="dateRange"
    />
  </div>
</template>
```

- [ ] **Step 2: Add script setup**

Use this script structure:

```vue
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getReportDashboard } from '../../api/report'
import DashboardPanel from './components/DashboardPanel.vue'
import InventoryAnalysisPanel from './components/InventoryAnalysisPanel.vue'
import PurchaseAnalysisPanel from './components/PurchaseAnalysisPanel.vue'
import { dateRangeByKey } from './reportFormat'

const tabs = [
  { key: 'dashboard', label: '数据看板' },
  { key: 'inventory', label: '库存分析' },
  { key: 'purchase', label: '采购分析' }
]
const rangeOptions = [
  { key: '7d', label: '7天' },
  { key: '30d', label: '30天' },
  { key: 'quarter', label: '季度' },
  { key: 'year', label: '年' }
]

const activeTab = ref('dashboard')
const rangeKey = ref('30d')
const expiryWarningDays = ref(30)
const dashboard = ref({ metrics: [], purchaseAmountTrend: [], outboundQuantityTrend: [], lowStockWarnings: [], expiryWarnings: [] })
const loading = ref(false)
const message = ref('')

const dateRange = computed(() => dateRangeByKey(rangeKey.value))

function setRange(key) {
  rangeKey.value = key
}

async function loadDashboard() {
  loading.value = true
  message.value = ''
  try {
    dashboard.value = await getReportDashboard({
      ...dateRange.value,
      expiryWarningDays: expiryWarningDays.value
    })
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}

function reloadCurrent() {
  if (activeTab.value === 'dashboard') {
    return loadDashboard()
  }
  return Promise.resolve()
}

watch([rangeKey, expiryWarningDays], () => {
  if (activeTab.value === 'dashboard') {
    loadDashboard()
  }
})

watch(activeTab, () => {
  if (activeTab.value === 'dashboard' && !dashboard.value.metrics.length) {
    loadDashboard()
  }
})

onMounted(loadDashboard)
</script>
```

- [ ] **Step 3: Add scoped report styles**

Add `<style scoped>` with report-specific classes. Use names prefixed by `report-`:

```css
.report-page {
  --report-bg-card: rgba(12, 30, 72, 0.06);
  --report-line: rgba(31, 41, 51, 0.10);
  --report-brand: #2f7cff;
  --report-brand-bright: #4d9bff;
  --report-ok: #1f7a4d;
  --report-warn: #b7791f;
  --report-danger: #b42318;
  --report-mute: #66727f;
}

.report-page-head {
  align-items: flex-end;
}

.report-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.report-date-range {
  display: inline-flex;
  padding: 3px;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.report-date-range button {
  background: transparent;
  border: 0;
  color: var(--muted);
  font: inherit;
  font-size: 12px;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
}

.report-date-range button.on {
  background: #fff;
  color: var(--report-brand);
  box-shadow: 0 1px 2px rgba(20, 40, 80, 0.06);
}

.report-sub-tabs {
  display: flex;
  align-items: center;
  gap: 2px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 18px;
  overflow-x: auto;
}

.report-sub-tabs button {
  background: transparent;
  color: var(--muted);
  padding: 14px 16px;
  border-bottom: 2px solid transparent;
  font-size: 13.5px;
  cursor: pointer;
}

.report-sub-tabs button.on {
  color: var(--report-brand);
  border-bottom-color: var(--report-brand);
  font-weight: 700;
}
```

Add these additional scoped style blocks in the same `<style scoped>` section:

```css
.report-kpi-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.report-kpi {
  position: relative;
  min-height: 116px;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}

.report-kpi .label {
  color: var(--muted);
  font-size: 12px;
  letter-spacing: 0.6px;
}

.report-kpi .value {
  margin-top: 9px;
  font-family: "Orbitron", "DM Mono", Consolas, monospace;
  font-size: 25px;
  font-weight: 700;
}

.report-kpi .unit {
  margin-left: 4px;
  color: var(--muted);
  font-family: inherit;
  font-size: 12px;
}

.report-kpi .delta {
  display: inline-block;
  margin-top: 10px;
  color: var(--report-brand);
  font-size: 12px;
}

.report-kpi .delta.warning,
.report-kpi .delta.warn {
  color: var(--report-warn);
}

.report-kpi .delta.danger {
  color: var(--report-danger);
}

.report-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: var(--shadow-sm);
}

.report-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.report-card-head .title-block h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.report-card-head .title-block h3::before {
  content: "";
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--report-brand-bright), var(--report-brand));
}

.report-card-head .sub {
  margin-top: 2px;
  padding-left: 11px;
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 0.3px;
}

.report-grid-2 {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 16px;
  margin-bottom: 18px;
}

.report-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.report-trend-chart {
  width: 100%;
  height: 260px;
}

.report-trend-chart .grid-line {
  stroke: rgba(31, 41, 51, 0.10);
  stroke-width: 1;
  stroke-dasharray: 3 3;
}

.report-trend-chart text {
  fill: var(--muted);
  font-family: "DM Mono", Consolas, monospace;
  font-size: 11px;
}

.report-trend-chart .area-main {
  fill: rgba(77, 155, 255, 0.18);
}

.report-trend-chart .line-main {
  fill: none;
  stroke: #4d9bff;
  stroke-width: 2.4;
}

.report-trend-chart .line-sub {
  fill: none;
  stroke: #34d399;
  stroke-width: 2;
  stroke-dasharray: 4 3;
}

.report-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  border: 1px solid currentColor;
  font-size: 11px;
  font-weight: 700;
}

.report-pill.warn {
  color: var(--report-warn);
  background: #fff8e8;
}

.report-pill.danger {
  color: var(--report-danger);
  background: #fdeaea;
}

.report-pill.ok {
  color: var(--report-ok);
  background: #e7f6ee;
}

.report-table-host {
  overflow: auto;
}

.report-table {
  width: 100%;
  min-width: 880px;
  border-collapse: collapse;
  font-size: 13px;
}

.report-table th,
.report-table td {
  text-align: left;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
  white-space: nowrap;
}

.report-table th {
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.2px;
  background: #f8fbfb;
}

.report-warning-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.report-warning-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fbfdfd;
}

.report-warning-item b {
  display: block;
  font-size: 13px;
}

.report-warning-item span {
  display: block;
  margin-top: 3px;
  color: var(--muted);
  font-size: 11px;
}

.report-empty {
  min-height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  border: 1px dashed var(--border);
  border-radius: 10px;
  background: #fbfdfd;
}

@media (max-width: 1100px) {
  .report-kpi-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .report-grid-2,
  .report-grid-3 {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .report-actions,
  .report-date-range {
    width: 100%;
  }

  .report-date-range button {
    flex: 1;
  }

  .report-kpi-row {
    grid-template-columns: 1fr;
  }
}
```

Keep page background, sidebar, and header untouched.

- [ ] **Step 4: Run frontend build**

Run from `system/frontend`:

```bash
npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add system/frontend/src/views/report/ReportsView.vue
git commit -m "feat(report): upgrade report statistics page"
```

---

### Task 8: Smoke Verification Script and Full Verification

**Files:**
- Create: `system/frontend/scripts/verify-report-statistics.mjs`

- [ ] **Step 1: Create smoke script**

Create `verify-report-statistics.mjs`:

```javascript
import { chromium } from 'playwright'

const baseUrl = process.env.APP_URL || 'http://127.0.0.1:5173'

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })

await page.goto(`${baseUrl}/reports`, { waitUntil: 'networkidle' })

await page.waitForSelector('.report-page')
await page.waitForSelector('.report-sub-tabs')
await page.waitForSelector('.report-kpi-row')

const tabText = await page.locator('.report-sub-tabs').innerText()
if (!tabText.includes('数据看板') || !tabText.includes('库存分析') || !tabText.includes('采购分析')) {
  throw new Error(`Report tabs missing: ${tabText}`)
}

const chartCount = await page.locator('svg.report-trend-chart').count()
if (chartCount < 1) {
  throw new Error('Expected at least one report SVG trend chart')
}

await page.getByRole('button', { name: '库存分析' }).click()
await page.waitForSelector('.report-table-host, .report-empty')

await page.getByRole('button', { name: '采购分析' }).click()
await page.waitForSelector('.report-table-host, .report-empty, .report-card')

await browser.close()
console.log('Report statistics smoke verification passed')
```

If this project does not have Playwright installed in `system/frontend/node_modules`, install it only after user approval during execution. Do not add Playwright as a production dependency.

- [ ] **Step 2: Run backend full tests**

Run from `system/backend`:

```bash
mvn test
```

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run from `system/frontend`:

```bash
npm run build
```

Expected: PASS.

- [ ] **Step 4: Run smoke script against local dev server**

Start frontend dev server from `system/frontend`:

```bash
npm run dev -- --host 127.0.0.1
```

In another terminal from `system/frontend`, run:

```bash
node scripts/verify-report-statistics.mjs
```

Expected: prints `Report statistics smoke verification passed`.

- [ ] **Step 5: Commit**

```bash
git add system/frontend/scripts/verify-report-statistics.mjs
git commit -m "test(report): add report statistics smoke verification"
```

---

## Self-Review

Spec coverage:

- Data dashboard is covered by Tasks 2, 3, 6, and 7.
- Core KPI cards / 核心指标卡片 are covered by Tasks 1, 3, 5, 6, and 7.
- Purchase amount trend is covered by Tasks 2, 3, 5, 6, and 7.
- Outbound quantity trend is covered by Tasks 2, 3, 5, 6, and 7.
- Low-stock warning is covered by Tasks 2, 3, 6, and 7.
- Near-expiry and expired warnings are covered by Tasks 2, 3, 6, and 7.
- Inventory summary is covered by Tasks 1, 2, 3, 6, and 7.
- Inventory ledger is covered by Tasks 1, 2, 3, 6, and 7.
- Batch expiry is covered by Tasks 1, 2, 3, 6, and 7.
- Purchase overview is covered by Tasks 1, 2, 3, 6, and 7.
- Supplier analysis is covered by Tasks 1, 2, 3, 6, and 7.
- Purchase details are covered by Tasks 1, 2, 3, 6, and 7.
- Visual boundary is covered by Task 7: current shell remains unchanged and report internals use scoped prototype-inspired styles.
- Verification is covered by Task 8.

Placeholder scan:

- The plan contains no open placeholder markers.
- The mapper task intentionally lists method names and SQL source rules because the SQL is long and must be implemented inside `ReportMapper.java`; tests pin the critical table and column contracts before implementation.

Type consistency:

- Backend endpoint names match frontend API paths.
- VO record names match the service/controller return types.
- Frontend `dashboard.purchaseAmountTrend`, `dashboard.outboundQuantityTrend`, `dashboard.lowStockWarnings`, and `dashboard.expiryWarnings` match `ReportDashboardVO`.
- Trend point fields `amountValue`, `plannedAmountValue`, `quantityValue`, and `statDate` match `ReportTrendPointVO` and `ReportTrendChart.vue`.
