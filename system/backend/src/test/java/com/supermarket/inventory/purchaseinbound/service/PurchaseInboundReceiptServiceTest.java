package com.supermarket.inventory.purchaseinbound.service;

import com.supermarket.inventory.auth.security.CurrentUser;
import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseInboundReceiptServiceTest {

    @Mock
    private PurchaseInboundMapper purchaseInboundMapper;
    @Mock
    private PurchaseInboundReceiptMapper receiptMapper;
    @Mock
    private StockService stockService;

    private PurchaseInboundReceiptService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseInboundReceiptService(purchaseInboundMapper, receiptMapper, stockService);
        CurrentUserContext.set(new CurrentUser(1L, "admin", List.of("ADMIN")));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void receivePartial_writesReceiptBatchesStockAndMainRollupWithoutApprovalLog() {
        PurchaseInboundVO order = order("APPROVED", 48, 0, "0.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 48, 0, "0.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));
        when(receiptMapper.findMaxReceiptNo(any(String.class))).thenReturn(null);
        when(receiptMapper.insertReceipt(any(PurchaseInboundReceipt.class))).thenReturn(200L);
        when(receiptMapper.insertReceiptBatch(any(PurchaseInboundReceiptBatch.class))).thenReturn(300L);

        service.receive(100L, request(10L, 1, LocalDate.of(2026, 6, 1), 180));

        ArgumentCaptor<PurchaseInboundReceipt> receiptCaptor = ArgumentCaptor.forClass(PurchaseInboundReceipt.class);
        verify(receiptMapper).insertReceipt(receiptCaptor.capture());
        assertThat(receiptCaptor.getValue().getReceiptNo())
                .isEqualTo("PIR" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "0001");

        ArgumentCaptor<PurchaseInboundReceiptBatch> batchCaptor = ArgumentCaptor.forClass(PurchaseInboundReceiptBatch.class);
        verify(receiptMapper).insertReceiptBatch(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getPurchasePriceSnapshot()).isEqualByComparingTo("48.000000");
        assertThat(batchCaptor.getValue().getCostPriceSnapshot()).isEqualByComparingTo("2.00000000");
        assertThat(batchCaptor.getValue().getBaseQuantity()).isEqualTo(24);
        assertThat(batchCaptor.getValue().getAmount()).isEqualByComparingTo("48.000000");

        verify(stockService).increase(any(StockIncreaseCommand.class));
        verify(purchaseInboundMapper).updateItemInboundTotals(10L, 24, new BigDecimal("48.000000"));
        verify(purchaseInboundMapper).updateInboundTotals(100L, 24, new BigDecimal("48.000000"));
        verify(purchaseInboundMapper).updateStatus(100L, "PARTIALLY_INBOUNDED");
        verify(purchaseInboundMapper, never()).insertApprovalLog(any());
    }

    @Test
    void receiveRejectsQuantityOverPlan() {
        PurchaseInboundVO order = order("PARTIALLY_INBOUNDED", 48, 24, "48.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 48, 24, "48.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));

        assertThatThrownBy(() -> service.receive(100L, request(10L, 2, LocalDate.of(2026, 6, 1), 180)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("实际入库数量不能超过计划剩余数量");

        verify(receiptMapper, never()).insertReceipt(any(PurchaseInboundReceipt.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void receiveRejectsNonReceivableStatuses() {
        for (String status : List.of("DRAFT", "SUBMITTED", "RETURNED", "CANCELLED", "CLOSED", "INBOUNDED")) {
            when(purchaseInboundMapper.findByIdForUpdate(100L))
                    .thenReturn(Optional.of(order(status, 48, 0, "0.000000")));

            assertThatThrownBy(() -> service.receive(100L, request(10L, 1, LocalDate.of(2026, 6, 1), 180)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("当前采购单状态不允许执行入库");
        }

        verify(purchaseInboundMapper, never()).findItemsByInboundIdForUpdate(100L);
        verify(receiptMapper, never()).insertReceipt(any(PurchaseInboundReceipt.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void receiveRejectsUnknownItemId() {
        PurchaseInboundVO order = order("APPROVED", 48, 0, "0.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 48, 0, "0.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));

        assertThatThrownBy(() -> service.receive(100L, request(99L, 1, LocalDate.of(2026, 6, 1), 180)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购计划明细不存在");

        verify(receiptMapper, never()).insertReceipt(any(PurchaseInboundReceipt.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void receiveRejectsDuplicateItemIds() {
        PurchaseInboundVO order = order("APPROVED", 48, 0, "0.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 48, 0, "0.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));

        PurchaseInboundReceiptRequest request = new PurchaseInboundReceiptRequest();
        request.setItems(List.of(
                receiptItem(10L, 1, LocalDate.of(2026, 6, 1), 180),
                receiptItem(10L, 1, LocalDate.of(2026, 6, 2), 180)
        ));

        assertThatThrownBy(() -> service.receive(100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("实际入库明细不能重复");

        verify(receiptMapper, never()).insertReceipt(any(PurchaseInboundReceipt.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void receiveComplete_updatesInboundedStatusAndStockCommandWithReceiptBatchSourceWithoutApprovalLog() {
        PurchaseInboundVO order = order("PARTIALLY_INBOUNDED", 48, 24, "48.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 48, 24, "48.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));
        when(receiptMapper.findMaxReceiptNo(any(String.class))).thenReturn(null);
        when(receiptMapper.insertReceipt(any(PurchaseInboundReceipt.class))).thenReturn(200L);
        when(receiptMapper.insertReceiptBatch(any(PurchaseInboundReceiptBatch.class))).thenReturn(300L);

        service.receive(100L, request(10L, 1, LocalDate.of(2026, 6, 1), 180));

        ArgumentCaptor<StockIncreaseCommand> commandCaptor = ArgumentCaptor.forClass(StockIncreaseCommand.class);
        verify(stockService).increase(commandCaptor.capture());
        StockIncreaseCommand command = commandCaptor.getValue();
        assertThat(command.getSkuId()).isEqualTo(20L);
        assertThat(command.getQuantity()).isEqualTo(24);
        assertThat(command.getPurchasePrice()).isEqualByComparingTo("48.000000");
        assertThat(command.getCostPrice()).isEqualByComparingTo("2.00000000");
        assertThat(command.getProductionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(command.getExpiryDate()).isEqualTo(LocalDate.of(2026, 11, 28));
        assertThat(command.getPurchaseInboundId()).isEqualTo(100L);
        assertThat(command.getPurchaseInboundItemId()).isEqualTo(10L);
        assertThat(command.getPurchaseInboundReceiptBatchId()).isEqualTo(300L);
        assertThat(command.getSourceType()).isEqualTo("PURCHASE_INBOUND_RECEIPT_BATCH");
        assertThat(command.getSourceId()).isEqualTo(300L);

        verify(purchaseInboundMapper).updateItemInboundTotals(10L, 48, new BigDecimal("96.000000"));
        verify(purchaseInboundMapper).updateInboundTotals(100L, 48, new BigDecimal("96.000000"));
        verify(purchaseInboundMapper).updateStatus(100L, "INBOUNDED");
        verify(purchaseInboundMapper, never()).insertApprovalLog(any());
    }

    @Test
    void receiveRejectsNullItemWithoutNpe() {
        PurchaseInboundVO order = order("APPROVED", 48, 0, "0.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 48, 0, "0.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));
        PurchaseInboundReceiptRequest request = new PurchaseInboundReceiptRequest();
        request.setItems(Arrays.asList((PurchaseInboundReceiptItemRequest) null));

        assertThatThrownBy(() -> service.receive(100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("实际入库明细不能为空");

        verify(receiptMapper, never()).insertReceipt(any(PurchaseInboundReceipt.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void receiveRejectsNullBatchWithoutNpe() {
        PurchaseInboundVO order = order("APPROVED", 48, 0, "0.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 48, 0, "0.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));
        PurchaseInboundReceiptItemRequest requestItem = new PurchaseInboundReceiptItemRequest();
        requestItem.setPurchaseInboundItemId(10L);
        requestItem.setBatches(Arrays.asList((PurchaseInboundReceiptBatchRequest) null));
        PurchaseInboundReceiptRequest request = new PurchaseInboundReceiptRequest();
        request.setItems(List.of(requestItem));

        assertThatThrownBy(() -> service.receive(100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("实际入库批次不能为空");

        verify(receiptMapper, never()).insertReceipt(any(PurchaseInboundReceipt.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void receiveRejectsNullBatchFieldsWithoutNpe() {
        PurchaseInboundVO order = order("APPROVED", 48, 0, "0.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 48, 0, "0.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));
        PurchaseInboundReceiptBatchRequest batch = new PurchaseInboundReceiptBatchRequest();
        batch.setQuantity(null);
        batch.setProductionDate(LocalDate.of(2026, 6, 1));
        batch.setShelfLifeDays(180);
        PurchaseInboundReceiptItemRequest requestItem = new PurchaseInboundReceiptItemRequest();
        requestItem.setPurchaseInboundItemId(10L);
        requestItem.setBatches(List.of(batch));
        PurchaseInboundReceiptRequest request = new PurchaseInboundReceiptRequest();
        request.setItems(List.of(requestItem));

        assertThatThrownBy(() -> service.receive(100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("实际入库数量不能为空");

        verify(receiptMapper, never()).insertReceipt(any(PurchaseInboundReceipt.class));
        verify(stockService, never()).increase(any(StockIncreaseCommand.class));
    }

    @Test
    void receiveSameItemMultipleBatches_accumulatesQuantityAndAmount() {
        PurchaseInboundVO order = order("APPROVED", 72, 0, "0.000000");
        PurchaseInboundItemVO item = item(10L, 20L, 72, 0, "0.000000");
        when(purchaseInboundMapper.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(purchaseInboundMapper.findItemsByInboundIdForUpdate(100L)).thenReturn(List.of(item));
        when(receiptMapper.findMaxReceiptNo(any(String.class))).thenReturn(null);
        when(receiptMapper.insertReceipt(any(PurchaseInboundReceipt.class))).thenReturn(200L);
        when(receiptMapper.insertReceiptBatch(any(PurchaseInboundReceiptBatch.class))).thenReturn(300L, 301L);

        PurchaseInboundReceiptItemRequest requestItem = new PurchaseInboundReceiptItemRequest();
        requestItem.setPurchaseInboundItemId(10L);
        requestItem.setBatches(List.of(
                receiptBatch(1, LocalDate.of(2026, 6, 1), 180),
                receiptBatch(2, LocalDate.of(2026, 6, 2), 180)
        ));
        PurchaseInboundReceiptRequest request = new PurchaseInboundReceiptRequest();
        request.setItems(List.of(requestItem));
        request.setRemark("arrival");

        service.receive(100L, request);

        verify(purchaseInboundMapper).updateItemInboundTotals(10L, 72, new BigDecimal("144.000000"));
        verify(purchaseInboundMapper).updateInboundTotals(100L, 72, new BigDecimal("144.000000"));
        verify(purchaseInboundMapper).updateStatus(100L, "INBOUNDED");
        verify(stockService, org.mockito.Mockito.times(2)).increase(any(StockIncreaseCommand.class));
        verify(receiptMapper, org.mockito.Mockito.times(2)).insertReceiptBatch(any(PurchaseInboundReceiptBatch.class));
    }

    private PurchaseInboundReceiptRequest request(Long itemId, int quantity, LocalDate productionDate, int shelfLifeDays) {
        PurchaseInboundReceiptRequest request = new PurchaseInboundReceiptRequest();
        request.setItems(List.of(receiptItem(itemId, quantity, productionDate, shelfLifeDays)));
        request.setRemark("arrival");
        return request;
    }

    private PurchaseInboundReceiptBatchRequest receiptBatch(int quantity, LocalDate productionDate, int shelfLifeDays) {
        PurchaseInboundReceiptBatchRequest batch = new PurchaseInboundReceiptBatchRequest();
        batch.setQuantity(quantity);
        batch.setProductionDate(productionDate);
        batch.setShelfLifeDays(shelfLifeDays);
        return batch;
    }

    private PurchaseInboundReceiptItemRequest receiptItem(
            Long itemId,
            int quantity,
            LocalDate productionDate,
            int shelfLifeDays
    ) {
        PurchaseInboundReceiptBatchRequest batch = receiptBatch(quantity, productionDate, shelfLifeDays);
        PurchaseInboundReceiptItemRequest item = new PurchaseInboundReceiptItemRequest();
        item.setPurchaseInboundItemId(itemId);
        item.setBatches(List.of(batch));
        return item;
    }

    private PurchaseInboundVO order(String status, int planned, int inbounded, String inboundAmount) {
        PurchaseInboundVO vo = new PurchaseInboundVO();
        vo.setId(100L);
        vo.setStatus(status);
        vo.setPlannedTotalQuantity(planned);
        vo.setInboundTotalQuantity(inbounded);
        vo.setInboundTotalAmount(new BigDecimal(inboundAmount));
        return vo;
    }

    private PurchaseInboundItemVO item(Long id, Long skuId, int planned, int inbounded, String inboundAmount) {
        PurchaseInboundItemVO vo = new PurchaseInboundItemVO();
        vo.setId(id);
        vo.setSkuId(skuId);
        vo.setPlannedBaseQuantity(planned);
        vo.setInboundedBaseQuantity(inbounded);
        vo.setInboundedAmount(new BigDecimal(inboundAmount));
        vo.setQuantity(2);
        vo.setUnit("box");
        vo.setConversionRate(24);
        vo.setPurchasePrice(new BigDecimal("48.000000"));
        vo.setCostPrice(new BigDecimal("2.00000000"));
        vo.setSupplierSkuCodeSnapshot("SUP-COLA");
        vo.setSupplierSkuNameSnapshot("Supplier Cola");
        vo.setSupplierSpecSnapshot("box");
        return vo;
    }
}
