package com.supermarket.inventory.purchaseinbound.service;

import com.supermarket.inventory.auth.security.CurrentUser;
import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.common.util.PageUtils;
import com.supermarket.inventory.purchaseinbound.domain.PurchaseInboundAction;
import com.supermarket.inventory.purchaseinbound.domain.PurchaseInboundStatus;
import com.supermarket.inventory.purchaseinbound.domain.PurchaseInboundWorkflow;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundDecisionRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundItemRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundRequest;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInbound;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundApprovalLog;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundItem;
import com.supermarket.inventory.purchaseinbound.mapper.PurchaseInboundMapper;
import com.supermarket.inventory.purchaseinbound.mapper.PurchaseInboundReceiptMapper;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundItemVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundReceiptBatchVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundReceiptVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import com.supermarket.inventory.sku.service.SkuUnitResolver;
import com.supermarket.inventory.stock.service.StockService;
import com.supermarket.inventory.supplier.entity.Supplier;
import com.supermarket.inventory.supplier.entity.SupplierSku;
import com.supermarket.inventory.supplier.mapper.SupplierMapper;
import com.supermarket.inventory.supplier.service.SupplierSkuService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseInboundService {

    private static final BigDecimal MAX_PURCHASE_PRICE = new BigDecimal("100000000.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000000.00");

    private final PurchaseInboundMapper purchaseInboundMapper;
    private final PurchaseInboundReceiptMapper receiptMapper;
    private final SkuUnitResolver skuUnitResolver;
    @SuppressWarnings("unused")
    private final StockService stockService;
    private final SupplierMapper supplierMapper;
    private final SupplierSkuService supplierSkuService;

    public PurchaseInboundService(
            PurchaseInboundMapper purchaseInboundMapper,
            PurchaseInboundReceiptMapper receiptMapper,
            SkuUnitResolver skuUnitResolver,
            StockService stockService,
            SupplierMapper supplierMapper,
            SupplierSkuService supplierSkuService
    ) {
        this.purchaseInboundMapper = purchaseInboundMapper;
        this.receiptMapper = receiptMapper;
        this.skuUnitResolver = skuUnitResolver;
        this.stockService = stockService;
        this.supplierMapper = supplierMapper;
        this.supplierSkuService = supplierSkuService;
    }

    public PageResult<PurchaseInboundVO> list(String keyword, Integer page, Integer pageSize) {
        int normalizedPage = PageUtils.normalizePage(page);
        int normalizedPageSize = PageUtils.normalizePageSize(pageSize);
        return new PageResult<>(
                purchaseInboundMapper.findPage(keyword, PageUtils.offset(normalizedPage, normalizedPageSize), normalizedPageSize),
                purchaseInboundMapper.count(keyword),
                normalizedPage,
                normalizedPageSize
        );
    }

    public PurchaseInboundVO getById(Long id) {
        PurchaseInboundVO vo = purchaseInboundMapper.findById(id)
                .orElseThrow(() -> new BusinessException(404, "采购入库单不存在"));
        vo.setItems(purchaseInboundMapper.findItemsByInboundId(id));
        vo.setApprovalLogs(purchaseInboundMapper.findApprovalLogsByInboundId(id));
        vo.setReceipts(findReceiptsWithBatches(id));
        return vo;
    }

    private List<PurchaseInboundReceiptVO> findReceiptsWithBatches(Long inboundId) {
        List<PurchaseInboundReceiptVO> receipts = receiptMapper.findReceiptsByInboundId(inboundId);
        if (receipts == null || receipts.isEmpty()) {
            return List.of();
        }
        List<Long> receiptIds = receipts.stream().map(PurchaseInboundReceiptVO::getId).toList();
        List<PurchaseInboundReceiptBatchVO> batches = receiptMapper.findReceiptBatchesByReceiptIds(receiptIds);
        if (batches == null || batches.isEmpty()) {
            receipts.forEach(receipt -> receipt.setBatches(List.of()));
            return receipts;
        }
        Map<Long, List<PurchaseInboundReceiptBatchVO>> batchesByReceiptId = batches.stream()
                .collect(Collectors.groupingBy(PurchaseInboundReceiptBatchVO::getReceiptId));
        receipts.forEach(receipt -> receipt.setBatches(
                batchesByReceiptId.getOrDefault(receipt.getId(), List.of())
        ));
        return receipts;
    }

    @Transactional
    public PurchaseInboundVO create(PurchaseInboundRequest request) {
        return createDraft(request);
    }

    @Transactional
    public PurchaseInboundVO createDraft(PurchaseInboundRequest request) {
        PreparedPlan preparedPlan = preparePlan(request);
        CurrentUser currentUser = CurrentUserContext.get();

        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setOrderNo(nextOrderNo());
        inbound.setSupplierId(preparedPlan.supplierId());
        inbound.setPlannedTotalQuantity(preparedPlan.totalQuantity());
        inbound.setPlannedTotalAmount(preparedPlan.totalAmount());
        inbound.setInboundTotalQuantity(0);
        inbound.setInboundTotalAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        inbound.setStatus(PurchaseInboundStatus.DRAFT.name());
        inbound.setCreatorUserId(currentUser.getUserId());
        inbound.setCreatorUsername(currentUser.getUsername());
        inbound.setOperator(currentUser.getUsername());
        inbound.setRemark(request.getRemark());

        Long inboundId;
        try {
            inboundId = purchaseInboundMapper.insertInbound(inbound);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("采购入库单号重复，请重试");
        }
        insertItems(inboundId, preparedPlan.items());
        return getById(inboundId);
    }

    @Transactional
    public PurchaseInboundVO updatePlan(Long id, PurchaseInboundRequest request) {
        PurchaseInboundVO current = requireForUpdate(id);
        PurchaseInboundStatus currentStatus = parseStatus(current.getStatus());
        if (!PurchaseInboundWorkflow.isPlanEditable(currentStatus)) {
            throw new BusinessException("当前采购单状态不允许修改计划");
        }

        PreparedPlan preparedPlan = preparePlan(request);
        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setId(id);
        inbound.setSupplierId(preparedPlan.supplierId());
        inbound.setPlannedTotalQuantity(preparedPlan.totalQuantity());
        inbound.setPlannedTotalAmount(preparedPlan.totalAmount());
        inbound.setRemark(request.getRemark());

        purchaseInboundMapper.updatePlan(inbound);
        purchaseInboundMapper.deleteItemsByInboundId(id);
        insertItems(id, preparedPlan.items());
        return getById(id);
    }

    @Transactional
    public PurchaseInboundVO submit(Long id) {
        PurchaseInboundVO current = requireForUpdate(id);
        PurchaseInboundStatus from = parseStatus(current.getStatus());
        PurchaseInboundStatus to = PurchaseInboundWorkflow.next(from, PurchaseInboundAction.SUBMIT);
        List<PurchaseInboundItemVO> items = purchaseInboundMapper.findItemsByInboundId(id);
        if (items == null || items.isEmpty()) {
            throw new BusinessException("采购计划明细不能为空");
        }

        CurrentUser currentUser = CurrentUserContext.get();
        purchaseInboundMapper.updateStatusForSubmit(id, to.name(), currentUser.getUserId(), currentUser.getUsername());
        writeApprovalLog(id, PurchaseInboundAction.SUBMIT, from, to, null);
        return getById(id);
    }

    @Transactional
    public PurchaseInboundVO approve(Long id) {
        PurchaseInboundVO current = requireForUpdate(id);
        PurchaseInboundStatus from = parseStatus(current.getStatus());
        PurchaseInboundStatus to = PurchaseInboundWorkflow.next(from, PurchaseInboundAction.APPROVE);
        CurrentUser currentUser = CurrentUserContext.get();

        purchaseInboundMapper.updateStatusForApprove(id, to.name(), currentUser.getUserId(), currentUser.getUsername());
        writeApprovalLog(id, PurchaseInboundAction.APPROVE, from, to, null);
        return getById(id);
    }

    @Transactional
    public PurchaseInboundVO returnForModification(Long id, PurchaseInboundDecisionRequest request) {
        PurchaseInboundVO current = requireForUpdate(id);
        PurchaseInboundStatus from = parseStatus(current.getStatus());
        PurchaseInboundStatus to = PurchaseInboundWorkflow.next(from, PurchaseInboundAction.RETURN);

        purchaseInboundMapper.updateStatus(id, to.name());
        writeApprovalLog(id, PurchaseInboundAction.RETURN, from, to, request);
        return getById(id);
    }

    @Transactional
    public PurchaseInboundVO cancel(Long id, PurchaseInboundDecisionRequest request) {
        PurchaseInboundVO current = requireForUpdate(id);
        if (defaultInt(current.getInboundTotalQuantity()) > 0) {
            throw new BusinessException("已有实际入库结果的采购单不能取消");
        }

        PurchaseInboundStatus from = parseStatus(current.getStatus());
        PurchaseInboundStatus to = PurchaseInboundWorkflow.next(from, PurchaseInboundAction.CANCEL);
        CurrentUser currentUser = CurrentUserContext.get();
        String reason = request == null ? null : request.getReason();

        purchaseInboundMapper.updateStatusForCancel(id, to.name(), currentUser.getUserId(), currentUser.getUsername(), reason);
        writeApprovalLog(id, PurchaseInboundAction.CANCEL, from, to, request);
        return getById(id);
    }

    @Transactional
    public PurchaseInboundVO close(Long id, PurchaseInboundDecisionRequest request) {
        PurchaseInboundVO current = requireForUpdate(id);
        PurchaseInboundStatus from = parseStatus(current.getStatus());
        PurchaseInboundStatus to = PurchaseInboundWorkflow.next(from, PurchaseInboundAction.CLOSE);
        int inboundTotalQuantity = defaultInt(current.getInboundTotalQuantity());
        int plannedTotalQuantity = defaultInt(current.getPlannedTotalQuantity());
        if (inboundTotalQuantity <= 0 || inboundTotalQuantity >= plannedTotalQuantity) {
            throw new BusinessException("只有已部分入库且未满计划的采购单可以关闭");
        }

        CurrentUser currentUser = CurrentUserContext.get();
        String reason = request == null ? null : request.getReason();
        purchaseInboundMapper.updateStatusForClose(id, to.name(), currentUser.getUserId(), currentUser.getUsername(), reason);
        writeApprovalLog(id, PurchaseInboundAction.CLOSE, from, to, request);
        return getById(id);
    }

    private PreparedPlan preparePlan(PurchaseInboundRequest request) {
        if (request == null) {
            throw new BusinessException("采购计划不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("采购计划明细不能为空");
        }

        Supplier supplier = requireEnabledSupplier(request.getSupplierId());
        Long supplierId = supplier.getId();
        List<PurchaseInboundItem> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (PurchaseInboundItemRequest requestItem : request.getItems()) {
            SupplierSku binding = supplierSkuService.requireEnabledBinding(supplierId, requestItem.getSkuId());
            validateMinPurchaseQuantity(requestItem, binding);
            SkuUnitResolver.ResolvedUnit resolvedUnit = skuUnitResolver.resolve(requestItem.getSkuId(), requestItem.getUnit());
            int baseQuantity = calculateBaseQuantity(requestItem.getQuantity(), resolvedUnit.conversionRate());
            BigDecimal purchasePrice = validatePurchasePrice(requestItem.getPurchasePrice());
            BigDecimal costPrice = purchasePrice.divide(
                    BigDecimal.valueOf(resolvedUnit.conversionRate()),
                    8,
                    RoundingMode.HALF_UP
            );
            BigDecimal amount = purchasePrice.multiply(BigDecimal.valueOf(requestItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            validateAmount(amount);

            PurchaseInboundItem item = new PurchaseInboundItem();
            item.setSkuId(resolvedUnit.sku().getId());
            item.setSupplierSkuId(binding.getId());
            item.setSupplierSkuCodeSnapshot(binding.getSupplierSkuCode());
            item.setSupplierSkuNameSnapshot(binding.getSupplierSkuName());
            item.setSupplierSpecSnapshot(binding.getSupplierSpec());
            item.setPlannedQuantity(requestItem.getQuantity());
            item.setUnit(resolvedUnit.unit());
            item.setConversionRate(resolvedUnit.conversionRate());
            item.setPlannedBaseQuantity(baseQuantity);
            item.setPlannedAmount(amount);
            item.setInboundedBaseQuantity(0);
            item.setInboundedAmount(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            item.setPurchasePrice(purchasePrice);
            item.setCostPrice(costPrice);
            items.add(item);

            totalQuantity = addBaseQuantity(totalQuantity, baseQuantity);
            totalAmount = totalAmount.add(amount);
            validateAmount(totalAmount);
        }

        return new PreparedPlan(supplierId, totalQuantity, totalAmount.setScale(2, RoundingMode.HALF_UP), items);
    }

    private void insertItems(Long inboundId, List<PurchaseInboundItem> items) {
        for (PurchaseInboundItem item : items) {
            item.setPurchaseInboundId(inboundId);
            purchaseInboundMapper.insertItem(item);
        }
    }

    private PurchaseInboundVO requireForUpdate(Long id) {
        return purchaseInboundMapper.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(404, "采购入库单不存在"));
    }

    private Supplier requireEnabledSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new BusinessException("供应商不能为空");
        }
        Supplier supplier = supplierMapper.findById(supplierId)
                .orElseThrow(() -> new BusinessException("供应商不存在"));
        if (!Integer.valueOf(1).equals(supplier.getStatus())) {
            throw new BusinessException("供应商已停用");
        }
        return supplier;
    }

    private void validateMinPurchaseQuantity(PurchaseInboundItemRequest requestItem, SupplierSku binding) {
        int minPurchaseQuantity = binding.getMinPurchaseQuantity() == null ? 1 : binding.getMinPurchaseQuantity();
        if (requestItem.getQuantity() < minPurchaseQuantity) {
            throw new BusinessException("采购数量不能低于供应商最小采购量");
        }
    }

    private void writeApprovalLog(
            Long inboundId,
            PurchaseInboundAction action,
            PurchaseInboundStatus from,
            PurchaseInboundStatus to,
            PurchaseInboundDecisionRequest request
    ) {
        CurrentUser currentUser = CurrentUserContext.get();
        PurchaseInboundApprovalLog log = new PurchaseInboundApprovalLog();
        log.setPurchaseInboundId(inboundId);
        log.setAction(action.name());
        log.setFromStatus(from.name());
        log.setToStatus(to.name());
        log.setOperatorUserId(currentUser.getUserId());
        log.setOperatorUsername(currentUser.getUsername());
        if (request != null) {
            log.setReason(request.getReason());
            log.setRemark(request.getRemark());
        }
        purchaseInboundMapper.insertApprovalLog(log);
    }

    private PurchaseInboundStatus parseStatus(String status) {
        try {
            return PurchaseInboundStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException("采购单状态异常");
        }
    }

    private int calculateBaseQuantity(int quantity, int conversionRate) {
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

    private BigDecimal validatePurchasePrice(BigDecimal purchasePrice) {
        if (purchasePrice == null) {
            throw new BusinessException("采购单价不能为空");
        }
        if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("采购单价不能小于0");
        }
        if (purchasePrice.scale() > 2) {
            throw new BusinessException("采购单价最多保留2位小数");
        }
        BigDecimal normalizedPrice = purchasePrice.setScale(2, RoundingMode.HALF_UP);
        if (normalizedPrice.compareTo(MAX_PURCHASE_PRICE) >= 0) {
            throw new BusinessException("采购单价超出范围");
        }
        return normalizedPrice;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(MAX_AMOUNT) >= 0) {
            throw new BusinessException("采购入库金额超出范围");
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String nextOrderNo() {
        String prefix = "PI" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String maxOrderNo = purchaseInboundMapper.findMaxOrderNo(prefix + "%");
        int sequence = 1;
        if (maxOrderNo != null && maxOrderNo.length() >= prefix.length() + 3) {
            try {
                sequence = Integer.parseInt(maxOrderNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException exception) {
                throw new BusinessException("采购入库单号序号异常");
            }
        }
        return prefix + String.format("%03d", sequence);
    }

    private record PreparedPlan(Long supplierId, int totalQuantity, BigDecimal totalAmount, List<PurchaseInboundItem> items) {
    }
}
