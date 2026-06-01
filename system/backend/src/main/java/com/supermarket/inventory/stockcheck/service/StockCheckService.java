package com.supermarket.inventory.stockcheck.service;

import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.stock.entity.Stock;
import com.supermarket.inventory.stock.mapper.StockMapper;
import com.supermarket.inventory.stockbatch.entity.StockBatch;
import com.supermarket.inventory.stockbatch.entity.StockBatchLog;
import com.supermarket.inventory.stockbatch.mapper.StockBatchMapper;
import com.supermarket.inventory.stockcheck.dto.StockCheckCreateRequest;
import com.supermarket.inventory.stockcheck.dto.StockCheckItemActualRequest;
import com.supermarket.inventory.stockcheck.dto.StockCheckItemsUpdateRequest;
import com.supermarket.inventory.stockcheck.entity.StockCheck;
import com.supermarket.inventory.stockcheck.entity.StockCheckItem;
import com.supermarket.inventory.stockcheck.mapper.StockCheckMapper;
import com.supermarket.inventory.stockcheck.vo.StockCheckVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class StockCheckService {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_CATEGORY_LEVEL1 = "CATEGORY_LEVEL1";
    private static final String SCOPE_CATEGORY_LEVEL2 = "CATEGORY_LEVEL2";
    private static final String SCOPE_SKU = "SKU";
    private static final String SKU_SELECT_ALL = "ALL";
    private static final String SKU_SELECT_MULTI = "MULTI";
    private static final String SKU_SELECT_SINGLE = "SINGLE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String ITEM_STATUS_PENDING = "PENDING";
    private static final String CHANGE_TYPE_CHECK = "CHECK";
    private static final String SOURCE_TYPE_STOCK_CHECK = "STOCK_CHECK";
    private static final String BATCH_STATUS_AVAILABLE = "AVAILABLE";
    private static final String BATCH_STATUS_EXPIRED = "EXPIRED";
    private static final String BATCH_STATUS_DEPLETED = "DEPLETED";
    private static final String BATCH_STATUS_LOCKED = "LOCKED";
    private static final String BATCH_STATUS_DAMAGED = "DAMAGED";
    private static final String BATCH_STATUS_CLOSED = "CLOSED";

    private final StockCheckMapper stockCheckMapper;
    private final StockMapper stockMapper;
    private final StockBatchMapper stockBatchMapper;

    public StockCheckService(
            StockCheckMapper stockCheckMapper,
            StockMapper stockMapper,
            StockBatchMapper stockBatchMapper
    ) {
        this.stockCheckMapper = stockCheckMapper;
        this.stockMapper = stockMapper;
        this.stockBatchMapper = stockBatchMapper;
    }

    public PageResult<StockCheckVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        return new PageResult<>(
                stockCheckMapper.findPage(keyword, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize),
                stockCheckMapper.count(keyword),
                normalizedPage,
                normalizedPageSize
        );
    }

    public StockCheckVO detail(Long id) {
        StockCheckVO vo = stockCheckMapper.findVOById(id)
                .orElseThrow(() -> new BusinessException(404, "盘点单不存在"));
        vo.setItems(stockCheckMapper.findItemVOsByCheckId(id));
        return vo;
    }

    @Transactional
    public StockCheckVO create(StockCheckCreateRequest request) {
        validateCreateRequest(request);
        List<Long> skuIds = resolveSkuIds(request);
        if (skuIds.isEmpty()) {
            throw new BusinessException("盘点范围内没有可盘点SKU");
        }
        List<StockBatch> batches = stockBatchMapper.findCheckableBySkuIds(skuIds);
        if (batches.isEmpty()) {
            throw new BusinessException("盘点范围内没有可盘点批次");
        }

        StockCheck stockCheck = new StockCheck();
        stockCheck.setCheckNo(nextCheckNo());
        stockCheck.setName(request.getName().trim());
        stockCheck.setScopeType(normalize(request.getScopeType()));
        stockCheck.setCategoryId(request.getCategoryId());
        stockCheck.setSkuSelectType(normalizeSkuSelectType(request.getSkuSelectType()));
        stockCheck.setStatus(STATUS_DRAFT);
        stockCheck.setTotalSkuCount((int) batches.stream().map(StockBatch::getSkuId).distinct().count());
        stockCheck.setTotalBatchCount(batches.size());
        stockCheck.setTotalDifference(0);

        Long stockCheckId = stockCheckMapper.insert(stockCheck);
        if (stockCheckId == null) {
            throw new BusinessException("盘点单保存失败");
        }

        for (StockBatch batch : batches) {
            StockCheckItem item = new StockCheckItem();
            item.setStockCheckId(stockCheckId);
            item.setSkuId(batch.getSkuId());
            item.setStockBatchId(batch.getId());
            item.setBatchNo(batch.getBatchNo());
            item.setSystemQuantity(batch.getQuantity());
            item.setStatus(ITEM_STATUS_PENDING);
            item.setExpireDate(batch.getExpireDate());
            stockCheckMapper.insertItem(item);
        }

        return detail(stockCheckId);
    }

    @Transactional
    public StockCheckVO updateItems(Long id, StockCheckItemsUpdateRequest request) {
        StockCheck stockCheck = lockDraftStockCheck(id);
        Set<Long> seenItemIds = new LinkedHashSet<>();
        for (StockCheckItemActualRequest itemRequest : request.getItems()) {
            if (!seenItemIds.add(itemRequest.getItemId())) {
                throw new BusinessException("盘点明细不能重复提交");
            }
            StockCheckItem item = stockCheckMapper.findItemByIdForUpdate(stockCheck.getId(), itemRequest.getItemId())
                    .orElseThrow(() -> new BusinessException(404, "盘点明细不存在"));
            int difference = itemRequest.getActualQuantity() - item.getSystemQuantity();
            stockCheckMapper.updateItemActualQuantity(
                    stockCheck.getId(),
                    item.getId(),
                    itemRequest.getActualQuantity(),
                    difference
            );
        }
        return detail(id);
    }

    @Transactional
    public StockCheckVO complete(Long id) {
        StockCheck stockCheck = lockDraftStockCheck(id);
        List<StockCheckItem> items = stockCheckMapper.findItemsByCheckIdForUpdate(stockCheck.getId());
        if (items.isEmpty()) {
            throw new BusinessException("盘点单没有明细，不能完成");
        }
        if (items.stream().anyMatch(item -> item.getActualQuantity() == null)) {
            throw new BusinessException("存在未录入实际数量的批次，不能完成盘点");
        }

        int totalDifference = 0;
        Map<Long, List<StockCheckItem>> itemsBySku = items.stream()
                .collect(Collectors.groupingBy(StockCheckItem::getSkuId, TreeMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<StockCheckItem>> entry : itemsBySku.entrySet()) {
            Long skuId = entry.getKey();
            Stock stock = lockStock(skuId);
            int stockBeforeQuantity = stock.getQuantity();

            for (StockCheckItem item : entry.getValue()) {
                StockBatch batch = stockBatchMapper.findByIdAndSkuIdForUpdate(item.getStockBatchId(), skuId)
                        .orElseThrow(() -> new BusinessException(404, "库存批次不存在"));
                ensureBatchCheckable(batch);
                int batchBeforeQuantity = batch.getQuantity();
                int batchActualQuantity = item.getActualQuantity();
                int batchDifference = batchActualQuantity - batchBeforeQuantity;
                String afterStatus = resolveBatchStatusAfterCheck(batch, batchActualQuantity);
                ensureUpdated(stockBatchMapper.updateRemainingQuantityAndStatus(
                        batch.getId(),
                        skuId,
                        batchActualQuantity,
                        afterStatus
                ));
                writeBatchCheckLog(batch, batchDifference, batchBeforeQuantity, batchActualQuantity, stockCheck.getId());
                totalDifference += batchDifference;
            }

            int stockAfterQuantity = stockBatchMapper.sumQuantityBySkuId(skuId);
            stockMapper.updateQuantity(skuId, stockAfterQuantity);
            stockMapper.insertLog(
                    skuId,
                    CHANGE_TYPE_CHECK,
                    stockAfterQuantity - stockBeforeQuantity,
                    stockBeforeQuantity,
                    stockAfterQuantity
            );
        }

        stockCheckMapper.complete(stockCheck.getId(), totalDifference);
        return detail(id);
    }

    private void validateCreateRequest(StockCheckCreateRequest request) {
        if (request == null) {
            throw new BusinessException("盘点单参数不能为空");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessException("盘点名称不能为空");
        }
        String scopeType = normalize(request.getScopeType());
        if (!List.of(SCOPE_ALL, SCOPE_CATEGORY_LEVEL1, SCOPE_CATEGORY_LEVEL2, SCOPE_SKU).contains(scopeType)) {
            throw new BusinessException("盘点范围不正确");
        }
        if ((SCOPE_CATEGORY_LEVEL1.equals(scopeType) || SCOPE_CATEGORY_LEVEL2.equals(scopeType))
                && request.getCategoryId() == null) {
            throw new BusinessException("分类盘点必须选择分类");
        }
        String skuSelectType = normalizeSkuSelectType(request.getSkuSelectType());
        if (!List.of(SKU_SELECT_ALL, SKU_SELECT_MULTI, SKU_SELECT_SINGLE).contains(skuSelectType)) {
            throw new BusinessException("SKU选择方式不正确");
        }
        if (SCOPE_SKU.equals(scopeType)) {
            if (request.getSkuIds() == null || request.getSkuIds().isEmpty()) {
                throw new BusinessException("指定SKU盘点必须选择SKU");
            }
            if (SKU_SELECT_SINGLE.equals(skuSelectType) && request.getSkuIds().size() != 1) {
                throw new BusinessException("单选SKU盘点只能选择一个SKU");
            }
        }
    }

    private List<Long> resolveSkuIds(StockCheckCreateRequest request) {
        String scopeType = normalize(request.getScopeType());
        List<Long> skuIds;
        if (SCOPE_ALL.equals(scopeType)) {
            skuIds = stockCheckMapper.findSkuIdsForAll();
        } else if (SCOPE_CATEGORY_LEVEL1.equals(scopeType)) {
            skuIds = stockCheckMapper.findSkuIdsByLevel1Category(request.getCategoryId());
        } else if (SCOPE_CATEGORY_LEVEL2.equals(scopeType)) {
            skuIds = stockCheckMapper.findSkuIdsByLevel2Category(request.getCategoryId());
        } else {
            skuIds = stockCheckMapper.filterActiveSkuIds(new ArrayList<>(new LinkedHashSet<>(request.getSkuIds())));
            if (skuIds.size() != new LinkedHashSet<>(request.getSkuIds()).size()) {
                throw new BusinessException("存在无效或停用的SKU");
            }
        }
        return skuIds;
    }

    private StockCheck lockDraftStockCheck(Long id) {
        if (id == null) {
            throw new BusinessException("盘点单ID不能为空");
        }
        StockCheck stockCheck = stockCheckMapper.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(404, "盘点单不存在"));
        if (!STATUS_DRAFT.equals(stockCheck.getStatus())) {
            throw new BusinessException("只有草稿状态的盘点单允许操作");
        }
        return stockCheck;
    }

    private Stock lockStock(Long skuId) {
        return stockMapper.findBySkuIdForUpdate(skuId)
                .orElseThrow(() -> new BusinessException(404, "库存记录不存在"));
    }

    private void ensureBatchCheckable(StockBatch batch) {
        if (BATCH_STATUS_CLOSED.equals(batch.getStatus())) {
            throw new BusinessException("已关闭批次不能盘点");
        }
    }

    private String resolveBatchStatusAfterCheck(StockBatch batch, int actualQuantity) {
        if (actualQuantity == 0) {
            return BATCH_STATUS_DEPLETED;
        }
        if (BATCH_STATUS_LOCKED.equals(batch.getStatus()) || BATCH_STATUS_DAMAGED.equals(batch.getStatus())) {
            return batch.getStatus();
        }
        if (batch.getExpireDate() != null && batch.getExpireDate().isBefore(LocalDate.now())) {
            return BATCH_STATUS_EXPIRED;
        }
        return BATCH_STATUS_AVAILABLE;
    }

    private void writeBatchCheckLog(
            StockBatch batch,
            int changeQuantity,
            int beforeQuantity,
            int afterQuantity,
            Long stockCheckId
    ) {
        StockBatchLog log = new StockBatchLog();
        log.setStockBatchId(batch.getId());
        log.setSkuId(batch.getSkuId());
        log.setChangeType(CHANGE_TYPE_CHECK);
        log.setChangeQuantity(changeQuantity);
        log.setBeforeQuantity(beforeQuantity);
        log.setAfterQuantity(afterQuantity);
        log.setSourceType(SOURCE_TYPE_STOCK_CHECK);
        log.setSourceId(stockCheckId);
        stockBatchMapper.insertLog(log);
    }

    private void ensureUpdated(int updatedRows) {
        if (updatedRows != 1) {
            throw new BusinessException("库存批次数量更新失败");
        }
    }

    private String nextCheckNo() {
        String prefix = "PC" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String maxCheckNo = stockCheckMapper.findMaxCheckNo(prefix + "%");
        int sequence = 1;
        if (maxCheckNo != null && maxCheckNo.length() >= prefix.length() + 4) {
            try {
                sequence = Integer.parseInt(maxCheckNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException exception) {
                throw new BusinessException("盘点单号序号异常");
            }
        }
        if (sequence > 9999) {
            throw new BusinessException("盘点单号当日序号已达上限");
        }
        return prefix + String.format("%04d", sequence);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeSkuSelectType(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? SKU_SELECT_ALL : normalized;
    }
}
