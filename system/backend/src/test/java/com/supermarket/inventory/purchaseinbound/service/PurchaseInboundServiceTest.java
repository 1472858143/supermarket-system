package com.supermarket.inventory.purchaseinbound.service;

import com.supermarket.inventory.auth.security.CurrentUser;
import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.common.response.PageResult;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundDecisionRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundItemRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundRequest;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInbound;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundApprovalLog;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundItem;
import com.supermarket.inventory.purchaseinbound.mapper.PurchaseInboundMapper;
import com.supermarket.inventory.purchaseinbound.mapper.PurchaseInboundReceiptMapper;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundApprovalLogVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundItemVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundReceiptVO;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.service.SkuUnitResolver;
import com.supermarket.inventory.stock.dto.StockIncreaseCommand;
import com.supermarket.inventory.stock.service.StockService;
import com.supermarket.inventory.supplier.entity.Supplier;
import com.supermarket.inventory.supplier.entity.SupplierSku;
import com.supermarket.inventory.supplier.mapper.SupplierMapper;
import com.supermarket.inventory.supplier.service.SupplierSkuService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseInboundServiceTest {

    @Mock
    private PurchaseInboundMapper purchaseInboundMapper;
    @Mock
    private PurchaseInboundReceiptMapper receiptMapper;

    @Mock
    private SkuUnitResolver skuUnitResolver;

    @Mock
    private StockService stockService;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SupplierSkuService supplierSkuService;

    private PurchaseInboundService purchaseInboundService;

    @BeforeEach
    void setUp() {
        purchaseInboundService = new PurchaseInboundService(
                purchaseInboundMapper,
                receiptMapper,
                skuUnitResolver,
                stockService,
                supplierMapper,
                supplierSkuService
        );
        CurrentUserContext.set(new CurrentUser(1L, "admin", List.of("ADMIN")));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void list_normalizesPaginationAndReturnsPageResult() {
        PurchaseInboundVO order = vo(100L);
        when(purchaseInboundMapper.findPage("cola", 0, 100)).thenReturn(List.of(order));
        when(purchaseInboundMapper.count("cola")).thenReturn(12L);

        PageResult<PurchaseInboundVO> result = purchaseInboundService.list("cola", 0, 120);

        assertThat(result.getItems()).containsExactly(order);
        assertThat(result.getTotal()).isEqualTo(12L);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(100);
    }

    @Test
    void getById_returnsInboundWithItems() {
        PurchaseInboundVO order = vo(100L);
        PurchaseInboundItemVO item = itemVO(10L, 48, 0);
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundId(100L)).thenReturn(List.of(item));

        PurchaseInboundVO result = purchaseInboundService.getById(100L);

        assertThat(result).isSameAs(order);
        assertThat(result.getItems()).containsExactly(item);
    }

    @Test
    void getById_returnsApprovalLogsAndReceiptsForWorkflowDetail() {
        PurchaseInboundVO order = vo(100L);
        PurchaseInboundItemVO item = itemVO(10L, 48, 0);
        PurchaseInboundApprovalLogVO approvalLog = new PurchaseInboundApprovalLogVO();
        approvalLog.setAction("APPROVE");
        approvalLog.setFromStatus("SUBMITTED");
        approvalLog.setToStatus("APPROVED");
        PurchaseInboundReceiptVO receipt = new PurchaseInboundReceiptVO();
        receipt.setId(200L);
        receipt.setPurchaseInboundId(100L);
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundId(100L)).thenReturn(List.of(item));
        when(purchaseInboundMapper.findApprovalLogsByInboundId(100L)).thenReturn(List.of(approvalLog));
        when(receiptMapper.findReceiptsByInboundId(100L)).thenReturn(List.of(receipt));

        PurchaseInboundVO result = purchaseInboundService.getById(100L);

        assertThat(result.getItems()).containsExactly(item);
        assertThat(result.getApprovalLogs()).containsExactly(approvalLog);
        assertThat(result.getReceipts()).containsExactly(receipt);
    }

    @Test
    void getById_throwsBusinessExceptionWhenInboundMissing() {
        when(purchaseInboundMapper.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseInboundService.getById(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购入库单不存在");
    }

    @Test
    void createDraft_savesPlanWithoutIncreasingStock() {
        String todayPrefix = "PI" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        mockEnabledSupplier(7L);
        mockEnabledBinding(7L, 20L, 1);
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 24));
        when(purchaseInboundMapper.findMaxOrderNo(todayPrefix + "%")).thenReturn(null);
        when(purchaseInboundMapper.insertInbound(any(PurchaseInbound.class))).thenReturn(100L);
        when(purchaseInboundMapper.insertItem(any(PurchaseInboundItem.class))).thenReturn(10L);
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        PurchaseInboundVO result = purchaseInboundService.createDraft(request(List.of(item(20L, 2, "box", "48.00")), "draft"));

        assertThat(result.getId()).isEqualTo(100L);
        ArgumentCaptor<PurchaseInbound> inboundCaptor = ArgumentCaptor.forClass(PurchaseInbound.class);
        verify(purchaseInboundMapper).insertInbound(inboundCaptor.capture());
        assertThat(inboundCaptor.getValue().getStatus()).isEqualTo("DRAFT");
        assertThat(inboundCaptor.getValue().getCreatorUserId()).isEqualTo(1L);
        assertThat(inboundCaptor.getValue().getCreatorUsername()).isEqualTo("admin");
        assertThat(inboundCaptor.getValue().getPlannedTotalQuantity()).isEqualTo(48);
        assertThat(inboundCaptor.getValue().getInboundTotalQuantity()).isEqualTo(0);
        assertThat(inboundCaptor.getValue().getPlannedTotalAmount()).isEqualByComparingTo("96.00");
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void createDraft_capturesSupplierSkuSnapshotsOnItems() {
        mockEnabledSupplier(7L);
        SupplierSku binding = binding(7L, 20L, 1);
        binding.setId(300L);
        binding.setSupplierSkuCode("SUP-20");
        binding.setSupplierSkuName("供应商可乐");
        binding.setSupplierSpec("24瓶/箱");
        when(supplierSkuService.requireEnabledBinding(7L, 20L)).thenReturn(binding);
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 24));
        when(purchaseInboundMapper.insertInbound(any(PurchaseInbound.class))).thenReturn(100L);
        when(purchaseInboundMapper.insertItem(any(PurchaseInboundItem.class))).thenReturn(10L);
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.createDraft(request(List.of(item(20L, 2, "box", "48.00")), "draft"));

        ArgumentCaptor<PurchaseInboundItem> itemCaptor = ArgumentCaptor.forClass(PurchaseInboundItem.class);
        verify(purchaseInboundMapper).insertItem(itemCaptor.capture());
        PurchaseInboundItem savedItem = itemCaptor.getValue();
        assertThat(savedItem.getSupplierSkuId()).isEqualTo(300L);
        assertThat(savedItem.getSupplierSkuCodeSnapshot()).isEqualTo("SUP-20");
        assertThat(savedItem.getSupplierSkuNameSnapshot()).isEqualTo("供应商可乐");
        assertThat(savedItem.getSupplierSpecSnapshot()).isEqualTo("24瓶/箱");
        assertThat(savedItem.getPlannedQuantity()).isEqualTo(2);
        assertThat(savedItem.getPlannedBaseQuantity()).isEqualTo(48);
        assertThat(savedItem.getInboundedBaseQuantity()).isEqualTo(0);
        assertThat(savedItem.getInboundedAmount()).isEqualByComparingTo("0.000000");
        assertThat(savedItem.getPlannedAmount()).isEqualByComparingTo("96.00");
    }

    @Test
    void createDraft_rejectsNegativePurchasePriceAtServiceLayerBeforeWritingOrder() {
        mockEnabledSupplier(7L);
        mockEnabledBinding(7L, 20L, 1);
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 24));

        assertThatThrownBy(() -> purchaseInboundService.createDraft(request(List.of(item(20L, 1, "box", "-0.01")), "draft")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购单价不能小于0");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(purchaseInboundMapper, never()).insertItem(any(PurchaseInboundItem.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void createDraft_calculatesCostPriceWithEightDecimalPlaces() {
        mockEnabledSupplier(7L);
        mockEnabledBinding(7L, 20L, 1);
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 3));
        when(purchaseInboundMapper.insertInbound(any(PurchaseInbound.class))).thenReturn(100L);
        when(purchaseInboundMapper.insertItem(any(PurchaseInboundItem.class))).thenReturn(10L);
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.createDraft(request(List.of(item(20L, 1, "box", "10.00")), "draft"));

        ArgumentCaptor<PurchaseInboundItem> itemCaptor = ArgumentCaptor.forClass(PurchaseInboundItem.class);
        verify(purchaseInboundMapper).insertItem(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getCostPrice()).isEqualByComparingTo("3.33333333");
    }

    @Test
    void submit_writesApprovalLogAndDoesNotIncreaseStock() {
        PurchaseInboundVO order = vo(100L);
        order.setStatus("DRAFT");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundId(100L)).thenReturn(List.of(itemVO(10L, 48, 0)));
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.submit(100L);

        verify(purchaseInboundMapper).updateStatusForSubmit(100L, "SUBMITTED", 1L, "admin");
        verify(purchaseInboundMapper).insertApprovalLog(argThat(log ->
                "SUBMIT".equals(log.getAction()) &&
                        "DRAFT".equals(log.getFromStatus()) &&
                        "SUBMITTED".equals(log.getToStatus())));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void submit_rejectsEmptyItemList() {
        PurchaseInboundVO order = vo(100L);
        order.setStatus("DRAFT");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundId(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> purchaseInboundService.submit(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购计划明细不能为空");

        verify(purchaseInboundMapper, never()).updateStatusForSubmit(any(), any(), any(), any());
        verify(purchaseInboundMapper, never()).insertApprovalLog(any(PurchaseInboundApprovalLog.class));
    }

    @Test
    void approve_writesApprovalLogAndDoesNotIncreaseStock() {
        PurchaseInboundVO order = vo(100L);
        order.setStatus("SUBMITTED");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.approve(100L);

        verify(purchaseInboundMapper).updateStatusForApprove(100L, "APPROVED", 1L, "admin");
        verify(purchaseInboundMapper).insertApprovalLog(argThat(log ->
                "APPROVE".equals(log.getAction()) &&
                        "SUBMITTED".equals(log.getFromStatus()) &&
                        "APPROVED".equals(log.getToStatus())));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void returnForModification_writesReturnLogAndKeepsIdentity() {
        PurchaseInboundDecisionRequest request = decision("价格需确认");
        PurchaseInboundVO order = vo(100L);
        order.setStatus("SUBMITTED");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.returnForModification(100L, request);

        verify(purchaseInboundMapper).updateStatus(100L, "RETURNED");
        verify(purchaseInboundMapper).insertApprovalLog(argThat(log ->
                "RETURN".equals(log.getAction()) &&
                        "SUBMITTED".equals(log.getFromStatus()) &&
                        "RETURNED".equals(log.getToStatus()) &&
                        "价格需确认".equals(log.getReason())));
    }

    @Test
    void updatePlan_allowsDraftPlanContentChange() {
        PurchaseInboundVO order = vo(100L);
        order.setStatus("DRAFT");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        mockEnabledSupplier(7L);
        mockEnabledBinding(7L, 20L, 1);
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 24));
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.updatePlan(100L, request(List.of(item(20L, 3, "box", "48.00")), "changed"));

        ArgumentCaptor<PurchaseInbound> inboundCaptor = ArgumentCaptor.forClass(PurchaseInbound.class);
        verify(purchaseInboundMapper).updatePlan(inboundCaptor.capture());
        assertThat(inboundCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(inboundCaptor.getValue().getSupplierId()).isEqualTo(7L);
        assertThat(inboundCaptor.getValue().getPlannedTotalQuantity()).isEqualTo(72);
        verify(purchaseInboundMapper).deleteItemsByInboundId(100L);
        verify(purchaseInboundMapper).insertItem(any(PurchaseInboundItem.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void updatePlan_allowsReturnedPlanToChangeSupplierAndRevalidatesBindings() {
        PurchaseInboundVO order = vo(100L);
        order.setStatus("RETURNED");
        PurchaseInboundRequest request = request(List.of(item(20L, 3, "box", "48.00")), "changed");
        request.setSupplierId(8L);
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        mockEnabledSupplier(8L);
        mockEnabledBinding(8L, 20L, 2);
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 24));
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.updatePlan(100L, request);

        verify(supplierMapper).findById(8L);
        verify(supplierSkuService).requireEnabledBinding(8L, 20L);
        verify(purchaseInboundMapper).updatePlan(argThat(inbound ->
                inbound.getId().equals(100L) && inbound.getSupplierId().equals(8L)));
    }

    @Test
    void updatePlan_rejectsStatusesAfterApprovalOrTermination() {
        for (String status : List.of("APPROVED", "PARTIALLY_INBOUNDED", "INBOUNDED", "CLOSED", "CANCELLED")) {
            PurchaseInboundVO order = vo(100L);
            order.setStatus(status);
            when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> purchaseInboundService.updatePlan(
                    100L,
                    request(List.of(item(20L, 1, null, "12.50")), null)
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("当前采购单状态不允许修改计划");
        }
    }

    @Test
    void cancel_rejectsOrderWithAnyReceipt() {
        PurchaseInboundDecisionRequest request = decision("不采购");
        PurchaseInboundVO order = vo(100L);
        order.setStatus("APPROVED");
        order.setInboundTotalQuantity(1);
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> purchaseInboundService.cancel(100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已有实际入库结果的采购单不能取消");
    }

    @Test
    void cancel_succeedsWhenInboundTotalIsZeroAndWritesCancelAudit() {
        PurchaseInboundDecisionRequest request = decision("不采购");
        PurchaseInboundVO order = vo(100L);
        order.setStatus("APPROVED");
        order.setInboundTotalQuantity(0);
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.cancel(100L, request);

        verify(purchaseInboundMapper).updateStatusForCancel(100L, "CANCELLED", 1L, "admin", "不采购");
        verify(purchaseInboundMapper).insertApprovalLog(argThat(log ->
                "CANCEL".equals(log.getAction()) &&
                        "APPROVED".equals(log.getFromStatus()) &&
                        "CANCELLED".equals(log.getToStatus()) &&
                        "不采购".equals(log.getReason())));
    }

    @Test
    void close_requiresPartiallyInboundedOrder() {
        PurchaseInboundDecisionRequest request = decision("剩余不再收货");
        PurchaseInboundVO order = vo(100L);
        order.setStatus("PARTIALLY_INBOUNDED");
        order.setInboundTotalQuantity(24);
        order.setPlannedTotalQuantity(48);
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        purchaseInboundService.close(100L, request);

        verify(purchaseInboundMapper).updateStatusForClose(100L, "CLOSED", 1L, "admin", "剩余不再收货");
        verify(purchaseInboundMapper).insertApprovalLog(argThat(log -> "CLOSE".equals(log.getAction())));
    }

    @Test
    void close_rejectsWithoutAnyReceiptOrAlreadyFullyReceived() {
        PurchaseInboundVO noReceipt = vo(100L);
        noReceipt.setStatus("PARTIALLY_INBOUNDED");
        noReceipt.setInboundTotalQuantity(0);
        noReceipt.setPlannedTotalQuantity(48);
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(noReceipt));

        assertThatThrownBy(() -> purchaseInboundService.close(100L, decision("剩余不收")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有已部分入库且未满计划的采购单可以关闭");

        PurchaseInboundVO fullyReceived = vo(100L);
        fullyReceived.setStatus("PARTIALLY_INBOUNDED");
        fullyReceived.setInboundTotalQuantity(48);
        fullyReceived.setPlannedTotalQuantity(48);
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(fullyReceived));

        assertThatThrownBy(() -> purchaseInboundService.close(100L, decision("剩余不收")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有已部分入库且未满计划的采购单可以关闭");
    }

    @Test
    void createDraft_translatesDuplicateOrderNoToBusinessExceptionBeforeWritingItems() {
        String todayPrefix = "PI" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        mockEnabledSupplier(7L);
        mockEnabledBinding(7L, 20L, 1);
        when(skuUnitResolver.resolve(20L, null))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "bottle", 1));
        when(purchaseInboundMapper.findMaxOrderNo(todayPrefix + "%")).thenReturn(null);
        when(purchaseInboundMapper.insertInbound(any(PurchaseInbound.class)))
                .thenThrow(new DuplicateKeyException("duplicate order_no"));

        assertThatThrownBy(() -> purchaseInboundService.createDraft(request(
                List.of(item(20L, 1, null, "12.50")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购入库单号重复，请重试");

        verify(purchaseInboundMapper, never()).insertItem(any(PurchaseInboundItem.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void createDraft_rejectsSupplierAndBindingProblemsBeforeWritingOrder() {
        PurchaseInboundRequest missingSupplier = request(List.of(item(20L, 1, null, "12.50")), null);
        missingSupplier.setSupplierId(null);

        assertThatThrownBy(() -> purchaseInboundService.createDraft(missingSupplier))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商不能为空");

        when(supplierMapper.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> purchaseInboundService.createDraft(request(List.of(item(20L, 1, null, "12.50")), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商不存在");

        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 0)));
        assertThatThrownBy(() -> purchaseInboundService.createDraft(request(List.of(item(20L, 1, null, "12.50")), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("供应商已停用");

        when(supplierMapper.findById(7L)).thenReturn(Optional.of(supplier(7L, 1)));
        when(supplierSkuService.requireEnabledBinding(7L, 20L))
                .thenThrow(new BusinessException("该SKU未绑定当前供应商"));
        assertThatThrownBy(() -> purchaseInboundService.createDraft(request(List.of(item(20L, 1, null, "12.50")), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该SKU未绑定当前供应商");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void createDraft_rejectsQuantityAndAmountProblemsBeforeWritingOrder() {
        mockEnabledSupplier(7L);
        mockEnabledBinding(7L, 20L, 6);
        assertThatThrownBy(() -> purchaseInboundService.createDraft(request(List.of(item(20L, 5, null, "12.50")), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购数量不能低于供应商最小采购量");

        mockEnabledBinding(7L, 21L, 1);
        when(skuUnitResolver.resolve(21L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(21L), "box", 2));
        assertThatThrownBy(() -> purchaseInboundService.createDraft(request(
                List.of(item(21L, Integer.MAX_VALUE, "box", "12.50")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("基础单位数量超出范围");

        mockEnabledBinding(7L, 22L, 1);
        when(skuUnitResolver.resolve(22L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(22L), "box", 2));
        assertThatThrownBy(() -> purchaseInboundService.createDraft(request(
                List.of(item(22L, 1, "box", "12.345")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购单价最多保留2位小数");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    private PurchaseInboundRequest request(List<PurchaseInboundItemRequest> items, String remark) {
        PurchaseInboundRequest request = new PurchaseInboundRequest();
        request.setSupplierId(7L);
        request.setItems(items);
        request.setRemark(remark);
        return request;
    }

    private PurchaseInboundItemRequest item(Long skuId, Integer quantity, String unit, String purchasePrice) {
        PurchaseInboundItemRequest item = new PurchaseInboundItemRequest();
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setPurchasePrice(new BigDecimal(purchasePrice));
        item.setProductionDate(LocalDate.of(2026, 6, 1));
        item.setShelfLifeDays(180);
        return item;
    }

    private PurchaseInboundDecisionRequest decision(String reason) {
        PurchaseInboundDecisionRequest request = new PurchaseInboundDecisionRequest();
        request.setReason(reason);
        return request;
    }

    private PurchaseInboundItemVO itemVO(Long id, Integer plannedBaseQuantity, Integer inboundedBaseQuantity) {
        PurchaseInboundItemVO vo = new PurchaseInboundItemVO();
        vo.setId(id);
        vo.setPlannedBaseQuantity(plannedBaseQuantity);
        vo.setInboundedBaseQuantity(inboundedBaseQuantity);
        return vo;
    }

    private Sku sku(Long id) {
        Sku sku = new Sku();
        sku.setId(id);
        return sku;
    }

    private Supplier supplier(Long id, Integer status) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setStatus(status);
        return supplier;
    }

    private SupplierSku binding(Long supplierId, Long skuId, Integer minPurchaseQuantity) {
        SupplierSku binding = new SupplierSku();
        binding.setSupplierId(supplierId);
        binding.setSkuId(skuId);
        binding.setMinPurchaseQuantity(minPurchaseQuantity);
        return binding;
    }

    private void mockEnabledSupplier(Long supplierId) {
        when(supplierMapper.findById(supplierId)).thenReturn(Optional.of(supplier(supplierId, 1)));
    }

    private void mockEnabledBinding(Long supplierId, Long skuId, Integer minPurchaseQuantity) {
        when(supplierSkuService.requireEnabledBinding(supplierId, skuId))
                .thenReturn(binding(supplierId, skuId, minPurchaseQuantity));
    }

    private PurchaseInboundVO vo(Long id) {
        PurchaseInboundVO vo = new PurchaseInboundVO();
        vo.setId(id);
        return vo;
    }
}
