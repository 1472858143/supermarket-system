package com.supermarket.inventory.purchaseinbound.service;

import com.supermarket.inventory.auth.security.CurrentUser;
import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.purchaseinbound.domain.PurchaseInboundStatus;
import com.supermarket.inventory.purchaseinbound.domain.PurchaseInboundWorkflow;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundReceiptBatchRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundReceiptItemRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundReceiptRequest;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundReceipt;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundReceiptBatch;
import com.supermarket.inventory.purchaseinbound.mapper.PurchaseInboundMapper;
import com.supermarket.inventory.purchaseinbound.mapper.PurchaseInboundReceiptMapper;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundItemVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import com.supermarket.inventory.stock.dto.StockIncreaseCommand;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PurchaseInboundReceiptService {

    private static final String SOURCE_TYPE_RECEIPT_BATCH = "PURCHASE_INBOUND_RECEIPT_BATCH";

    private final PurchaseInboundMapper purchaseInboundMapper;
    private final PurchaseInboundReceiptMapper receiptMapper;
    private final StockService stockService;

    public PurchaseInboundReceiptService(
            PurchaseInboundMapper purchaseInboundMapper,
            PurchaseInboundReceiptMapper receiptMapper,
            StockService stockService
    ) {
        this.purchaseInboundMapper = purchaseInboundMapper;
        this.receiptMapper = receiptMapper;
        this.stockService = stockService;
    }

    @Transactional
    public void receive(Long id, PurchaseInboundReceiptRequest request) {
        PurchaseInboundVO inbound = purchaseInboundMapper.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(404, "采购入库单不存在"));
        PurchaseInboundStatus currentStatus = parseStatus(inbound.getStatus());
        if (!PurchaseInboundWorkflow.isReceivable(currentStatus)) {
            throw new BusinessException("当前采购单状态不允许执行入库");
        }

        List<PurchaseInboundItemVO> planItems = purchaseInboundMapper.findItemsByInboundIdForUpdate(id);
        PreparedReceipt preparedReceipt = prepareReceipt(id, request, planItems, inbound);
        CurrentUser currentUser = CurrentUserContext.get();

        PurchaseInboundReceipt receipt = new PurchaseInboundReceipt();
        receipt.setReceiptNo(nextReceiptNo());
        receipt.setPurchaseInboundId(id);
        receipt.setOperatorUserId(currentUser.getUserId());
        receipt.setOperatorUsername(currentUser.getUsername());
        receipt.setTotalBaseQuantity(preparedReceipt.totalBaseQuantity());
        receipt.setTotalAmount(preparedReceipt.totalAmount());
        receipt.setRemark(request.getRemark());
        Long receiptId = receiptMapper.insertReceipt(receipt);

        for (PreparedReceiptBatch preparedBatch : preparedReceipt.batches()) {
            PurchaseInboundReceiptBatch batch = preparedBatch.batch();
            batch.setReceiptId(receiptId);
            Long receiptBatchId = receiptMapper.insertReceiptBatch(batch);
            stockService.increase(toStockIncreaseCommand(id, receiptBatchId, batch));
        }

        for (Map.Entry<Long, ItemInboundTotals> entry : preparedReceipt.itemTotals().entrySet()) {
            ItemInboundTotals totals = entry.getValue();
            purchaseInboundMapper.updateItemInboundTotals(entry.getKey(), totals.quantity(), totals.amount());
        }
        purchaseInboundMapper.updateInboundTotals(id, preparedReceipt.newInboundTotalQuantity(), preparedReceipt.newInboundTotalAmount());
        purchaseInboundMapper.updateStatus(id, preparedReceipt.newStatus().name());
    }

    private PreparedReceipt prepareReceipt(
            Long inboundId,
            PurchaseInboundReceiptRequest request,
            List<PurchaseInboundItemVO> planItems,
            PurchaseInboundVO inbound
    ) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("实际入库明细不能为空");
        }

        Map<Long, PurchaseInboundItemVO> planItemById = new HashMap<>();
        for (PurchaseInboundItemVO planItem : planItems) {
            planItemById.put(planItem.getId(), planItem);
        }

        Set<Long> receiptItemIds = new HashSet<>();
        Map<Long, ItemInboundTotals> itemTotals = new LinkedHashMap<>();
        List<PreparedReceiptBatch> batches = new ArrayList<>();
        int totalBaseQuantity = 0;
        BigDecimal totalAmount = zeroAmount();

        for (PurchaseInboundReceiptItemRequest requestItem : request.getItems()) {
            if (requestItem == null) {
                throw new BusinessException("实际入库明细不能为空");
            }
            Long itemId = requestItem.getPurchaseInboundItemId();
            if (itemId == null) {
                throw new BusinessException("采购计划明细ID不能为空");
            }
            if (!receiptItemIds.add(itemId)) {
                throw new BusinessException("实际入库明细不能重复");
            }
            PurchaseInboundItemVO planItem = planItemById.get(itemId);
            if (planItem == null) {
                throw new BusinessException("采购计划明细不存在");
            }
            if (requestItem.getBatches() == null || requestItem.getBatches().isEmpty()) {
                throw new BusinessException("实际入库批次不能为空");
            }

            int itemInboundQuantity = defaultInt(planItem.getInboundedBaseQuantity());
            BigDecimal itemInboundAmount = defaultAmount(planItem.getInboundedAmount());
            for (PurchaseInboundReceiptBatchRequest requestBatch : requestItem.getBatches()) {
                validateRequestBatch(requestBatch);
                int baseQuantity = calculateBaseQuantity(requestBatch.getQuantity(), planItem.getConversionRate());
                itemInboundQuantity = addBaseQuantity(itemInboundQuantity, baseQuantity);
                if (itemInboundQuantity > defaultInt(planItem.getPlannedBaseQuantity())) {
                    throw new BusinessException("实际入库数量不能超过计划剩余数量");
                }

                BigDecimal amount = planItem.getCostPrice()
                        .multiply(BigDecimal.valueOf(baseQuantity))
                        .setScale(6, RoundingMode.HALF_UP);
                itemInboundAmount = itemInboundAmount.add(amount).setScale(6, RoundingMode.HALF_UP);
                totalBaseQuantity = addBaseQuantity(totalBaseQuantity, baseQuantity);
                totalAmount = totalAmount.add(amount).setScale(6, RoundingMode.HALF_UP);
                batches.add(new PreparedReceiptBatch(toReceiptBatch(inboundId, planItem, requestBatch, baseQuantity, amount)));
            }
            itemTotals.put(itemId, new ItemInboundTotals(itemInboundQuantity, itemInboundAmount));
        }

        int newInboundTotalQuantity = addBaseQuantity(defaultInt(inbound.getInboundTotalQuantity()), totalBaseQuantity);
        BigDecimal newInboundTotalAmount = defaultAmount(inbound.getInboundTotalAmount())
                .add(totalAmount)
                .setScale(6, RoundingMode.HALF_UP);
        PurchaseInboundStatus newStatus = newInboundTotalQuantity >= defaultInt(inbound.getPlannedTotalQuantity())
                ? PurchaseInboundStatus.INBOUNDED
                : PurchaseInboundStatus.PARTIALLY_INBOUNDED;
        return new PreparedReceipt(
                batches,
                itemTotals,
                totalBaseQuantity,
                totalAmount,
                newInboundTotalQuantity,
                newInboundTotalAmount,
                newStatus
        );
    }

    private PurchaseInboundReceiptBatch toReceiptBatch(
            Long inboundId,
            PurchaseInboundItemVO planItem,
            PurchaseInboundReceiptBatchRequest requestBatch,
            int baseQuantity,
            BigDecimal amount
    ) {
        PurchaseInboundReceiptBatch batch = new PurchaseInboundReceiptBatch();
        batch.setPurchaseInboundId(inboundId);
        batch.setPurchaseInboundItemId(planItem.getId());
        batch.setSkuId(planItem.getSkuId());
        batch.setQuantity(requestBatch.getQuantity());
        batch.setBaseQuantity(baseQuantity);
        batch.setProductionDate(requestBatch.getProductionDate());
        batch.setShelfLifeDays(requestBatch.getShelfLifeDays());
        batch.setExpireDate(requestBatch.getProductionDate().plusDays(requestBatch.getShelfLifeDays()));
        batch.setPurchasePriceSnapshot(planItem.getPurchasePrice());
        batch.setCostPriceSnapshot(planItem.getCostPrice());
        batch.setAmount(amount);
        batch.setSupplierSkuCodeSnapshot(planItem.getSupplierSkuCodeSnapshot());
        batch.setSupplierSkuNameSnapshot(planItem.getSupplierSkuNameSnapshot());
        batch.setSupplierSpecSnapshot(planItem.getSupplierSpecSnapshot());
        return batch;
    }

    private void validateRequestBatch(PurchaseInboundReceiptBatchRequest requestBatch) {
        if (requestBatch == null) {
            throw new BusinessException("实际入库批次不能为空");
        }
        if (requestBatch.getQuantity() == null) {
            throw new BusinessException("实际入库数量不能为空");
        }
        if (requestBatch.getQuantity() <= 0) {
            throw new BusinessException("实际入库数量必须大于0");
        }
        if (requestBatch.getProductionDate() == null) {
            throw new BusinessException("生产日期不能为空");
        }
        if (requestBatch.getShelfLifeDays() == null) {
            throw new BusinessException("保质期天数不能为空");
        }
        if (requestBatch.getShelfLifeDays() <= 0) {
            throw new BusinessException("保质期天数必须大于0");
        }
    }

    private StockIncreaseCommand toStockIncreaseCommand(Long inboundId, Long receiptBatchId, PurchaseInboundReceiptBatch batch) {
        StockIncreaseCommand command = new StockIncreaseCommand();
        command.setSkuId(batch.getSkuId());
        command.setQuantity(batch.getBaseQuantity());
        command.setPurchasePrice(batch.getPurchasePriceSnapshot());
        command.setCostPrice(batch.getCostPriceSnapshot());
        command.setProductionDate(batch.getProductionDate());
        command.setExpiryDate(batch.getExpireDate());
        command.setPurchaseInboundId(inboundId);
        command.setPurchaseInboundItemId(batch.getPurchaseInboundItemId());
        command.setPurchaseInboundReceiptBatchId(receiptBatchId);
        command.setSourceType(SOURCE_TYPE_RECEIPT_BATCH);
        command.setSourceId(receiptBatchId);
        return command;
    }

    private PurchaseInboundStatus parseStatus(String status) {
        try {
            return PurchaseInboundStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException("采购单状态异常");
        }
    }

    private int calculateBaseQuantity(Integer quantity, Integer conversionRate) {
        try {
            return Math.multiplyExact(quantity, conversionRate);
        } catch (ArithmeticException ex) {
            throw new BusinessException("基础单位数量超出范围");
        }
    }

    private int addBaseQuantity(int current, int addition) {
        try {
            return Math.addExact(current, addition);
        } catch (ArithmeticException ex) {
            throw new BusinessException("基础单位数量超出范围");
        }
    }

    private String nextReceiptNo() {
        String prefix = "PIR" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String maxReceiptNo = receiptMapper.findMaxReceiptNo(prefix + "%");
        int sequence = 1;
        if (maxReceiptNo != null && maxReceiptNo.length() >= prefix.length() + 4) {
            try {
                sequence = Integer.parseInt(maxReceiptNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException exception) {
                throw new BusinessException("采购入库执行单号序号异常");
            }
        }
        return prefix + String.format("%04d", sequence);
    }

    private static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? zeroAmount() : value.setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroAmount() {
        return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    }

    private record PreparedReceipt(
            List<PreparedReceiptBatch> batches,
            Map<Long, ItemInboundTotals> itemTotals,
            int totalBaseQuantity,
            BigDecimal totalAmount,
            int newInboundTotalQuantity,
            BigDecimal newInboundTotalAmount,
            PurchaseInboundStatus newStatus
    ) {
    }

    private record PreparedReceiptBatch(PurchaseInboundReceiptBatch batch) {
    }

    private record ItemInboundTotals(int quantity, BigDecimal amount) {
    }
}
