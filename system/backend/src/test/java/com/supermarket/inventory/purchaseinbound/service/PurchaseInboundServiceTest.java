package com.supermarket.inventory.purchaseinbound.service;

import com.supermarket.inventory.auth.security.CurrentUser;
import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundItemRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundRequest;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInbound;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundItem;
import com.supermarket.inventory.purchaseinbound.mapper.PurchaseInboundMapper;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import com.supermarket.inventory.sku.entity.Sku;
import com.supermarket.inventory.sku.service.SkuUnitResolver;
import com.supermarket.inventory.stock.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseInboundServiceTest {

    @Mock
    private PurchaseInboundMapper purchaseInboundMapper;

    @Mock
    private SkuUnitResolver skuUnitResolver;

    @Mock
    private StockService stockService;

    private PurchaseInboundService purchaseInboundService;

    @BeforeEach
    void setUp() {
        purchaseInboundService = new PurchaseInboundService(purchaseInboundMapper, skuUnitResolver, stockService);
        CurrentUserContext.set(new CurrentUser(1L, "admin", List.of("ADMIN")));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void create_writesPurchaseInboundAndIncreasesStockWithPurchaseInboundLogType() {
        String todayPrefix = "PI" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(skuUnitResolver.resolve(20L, null))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "bottle", 1));
        when(purchaseInboundMapper.findMaxOrderNo(todayPrefix + "%")).thenReturn(null);
        when(purchaseInboundMapper.insertInbound(any(PurchaseInbound.class))).thenReturn(100L);
        when(purchaseInboundMapper.findById(100L)).thenReturn(Optional.of(vo(100L)));

        PurchaseInboundVO result = purchaseInboundService.create(request(List.of(item(20L, 5, null, "12.50")), "arrived"));

        assertThat(result.getId()).isEqualTo(100L);

        ArgumentCaptor<PurchaseInbound> inboundCaptor = ArgumentCaptor.forClass(PurchaseInbound.class);
        verify(purchaseInboundMapper).insertInbound(inboundCaptor.capture());
        PurchaseInbound inbound = inboundCaptor.getValue();
        assertThat(inbound.getOrderNo()).startsWith(todayPrefix);
        assertThat(inbound.getOrderNo()).endsWith("001");
        assertThat(inbound.getTotalQuantity()).isEqualTo(5);
        assertThat(inbound.getTotalAmount()).isEqualByComparingTo("62.50");
        assertThat(inbound.getStatus()).isEqualTo("COMPLETED");
        assertThat(inbound.getOperator()).isEqualTo("admin");
        assertThat(inbound.getRemark()).isEqualTo("arrived");

        ArgumentCaptor<List<PurchaseInboundItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInboundMapper).insertItems(itemsCaptor.capture());
        PurchaseInboundItem savedItem = itemsCaptor.getValue().get(0);
        assertThat(savedItem.getPurchaseInboundId()).isEqualTo(100L);
        assertThat(savedItem.getSkuId()).isEqualTo(20L);
        assertThat(savedItem.getQuantity()).isEqualTo(5);
        assertThat(savedItem.getUnit()).isEqualTo("bottle");
        assertThat(savedItem.getConversionRate()).isEqualTo(1);
        assertThat(savedItem.getBaseQuantity()).isEqualTo(5);
        assertThat(savedItem.getPurchasePrice()).isEqualByComparingTo("12.50");
        assertThat(savedItem.getCostPrice()).isEqualByComparingTo("12.5000");
        assertThat(savedItem.getAmount()).isEqualByComparingTo("62.50");

        InOrder inOrder = inOrder(purchaseInboundMapper, stockService);
        inOrder.verify(purchaseInboundMapper).insertInbound(any(PurchaseInbound.class));
        inOrder.verify(purchaseInboundMapper).insertItems(anyList());
        inOrder.verify(stockService).increase(20L, 5, "PURCHASE_INBOUND");
    }

    @Test
    void create_usesResolvedConversionRateAndNextOrderNoForMultipleItems() {
        String todayPrefix = "PI" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 24));
        when(skuUnitResolver.resolve(21L, "pack"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(21L), "pack", 5));
        when(purchaseInboundMapper.findMaxOrderNo(todayPrefix + "%")).thenReturn(todayPrefix + "009");
        when(purchaseInboundMapper.insertInbound(any(PurchaseInbound.class))).thenReturn(101L);
        when(purchaseInboundMapper.findById(101L)).thenReturn(Optional.of(vo(101L)));

        PurchaseInboundVO result = purchaseInboundService.create(request(List.of(
                item(20L, 2, "box", "48.00"),
                item(21L, 3, "pack", "10.00")
        ), null));

        assertThat(result.getId()).isEqualTo(101L);

        ArgumentCaptor<PurchaseInbound> inboundCaptor = ArgumentCaptor.forClass(PurchaseInbound.class);
        verify(purchaseInboundMapper).insertInbound(inboundCaptor.capture());
        PurchaseInbound inbound = inboundCaptor.getValue();
        assertThat(inbound.getOrderNo()).isEqualTo(todayPrefix + "010");
        assertThat(inbound.getTotalQuantity()).isEqualTo(63);
        assertThat(inbound.getTotalAmount()).isEqualByComparingTo("126.00");
        assertThat(inbound.getOperator()).isEqualTo("admin");

        ArgumentCaptor<List<PurchaseInboundItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInboundMapper).insertItems(itemsCaptor.capture());
        List<PurchaseInboundItem> items = itemsCaptor.getValue();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getPurchaseInboundId()).isEqualTo(101L);
        assertThat(items.get(0).getSkuId()).isEqualTo(20L);
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
        assertThat(items.get(0).getUnit()).isEqualTo("box");
        assertThat(items.get(0).getConversionRate()).isEqualTo(24);
        assertThat(items.get(0).getBaseQuantity()).isEqualTo(48);
        assertThat(items.get(0).getPurchasePrice()).isEqualByComparingTo("48.00");
        assertThat(items.get(0).getCostPrice()).isEqualByComparingTo("2.0000");
        assertThat(items.get(0).getAmount()).isEqualByComparingTo("96.00");
        assertThat(items.get(1).getPurchaseInboundId()).isEqualTo(101L);
        assertThat(items.get(1).getSkuId()).isEqualTo(21L);
        assertThat(items.get(1).getQuantity()).isEqualTo(3);
        assertThat(items.get(1).getUnit()).isEqualTo("pack");
        assertThat(items.get(1).getConversionRate()).isEqualTo(5);
        assertThat(items.get(1).getBaseQuantity()).isEqualTo(15);
        assertThat(items.get(1).getPurchasePrice()).isEqualByComparingTo("10.00");
        assertThat(items.get(1).getCostPrice()).isEqualByComparingTo("2.0000");
        assertThat(items.get(1).getAmount()).isEqualByComparingTo("30.00");

        verify(stockService).increase(20L, 48, "PURCHASE_INBOUND");
        verify(stockService).increase(21L, 15, "PURCHASE_INBOUND");
    }

    @Test
    void create_rejectsBaseQuantityOverflowBeforeWritingOrder() {
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 2));

        assertThatThrownBy(() -> purchaseInboundService.create(request(
                List.of(item(20L, Integer.MAX_VALUE, "box", "12.50")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("基础单位数量超出范围");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(purchaseInboundMapper, never()).insertItems(anyList());
        verify(stockService, never()).increase(any(), any(Integer.class), any());
    }

    @Test
    void create_rejectsPurchasePriceWithTooManyDecimalsBeforeWritingOrder() {
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 2));

        assertThatThrownBy(() -> purchaseInboundService.create(request(
                List.of(item(20L, 1, "box", "12.345")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购单价最多保留2位小数");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(purchaseInboundMapper, never()).insertItems(anyList());
        verify(stockService, never()).increase(any(), any(Integer.class), any());
    }

    @Test
    void create_rejectsPurchasePriceBeyondSchemaLimitBeforeWritingOrder() {
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 2));

        assertThatThrownBy(() -> purchaseInboundService.create(request(
                List.of(item(20L, 1, "box", "100000000.00")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购单价超出范围");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(purchaseInboundMapper, never()).insertItems(anyList());
        verify(stockService, never()).increase(any(), any(Integer.class), any());
    }

    @Test
    void create_rejectsLineAmountBeyondSchemaLimitBeforeWritingOrder() {
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 1));

        assertThatThrownBy(() -> purchaseInboundService.create(request(
                List.of(item(20L, 101, "box", "99999999.99")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购入库金额超出范围");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(purchaseInboundMapper, never()).insertItems(anyList());
        verify(stockService, never()).increase(any(), any(Integer.class), any());
    }

    @Test
    void create_rejectsTotalAmountBeyondSchemaLimitBeforeWritingOrder() {
        when(skuUnitResolver.resolve(20L, "box"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "box", 1));
        when(skuUnitResolver.resolve(21L, "pack"))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(21L), "pack", 1));

        assertThatThrownBy(() -> purchaseInboundService.create(request(List.of(
                item(20L, 100, "box", "50000000.00"),
                item(21L, 100, "pack", "50000000.00")
        ), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购入库金额超出范围");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(purchaseInboundMapper, never()).insertItems(anyList());
        verify(stockService, never()).increase(any(), any(Integer.class), any());
    }

    @Test
    void create_rejectsInvalidExistingOrderNoSuffixBeforeWritingOrder() {
        String todayPrefix = "PI" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(skuUnitResolver.resolve(20L, null))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "bottle", 1));
        when(purchaseInboundMapper.findMaxOrderNo(todayPrefix + "%")).thenReturn(todayPrefix + "ABC");

        assertThatThrownBy(() -> purchaseInboundService.create(request(
                List.of(item(20L, 1, null, "12.50")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购入库单号序号异常");

        verify(purchaseInboundMapper, never()).insertInbound(any(PurchaseInbound.class));
        verify(purchaseInboundMapper, never()).insertItems(anyList());
        verify(stockService, never()).increase(any(), any(Integer.class), any());
    }

    @Test
    void create_translatesDuplicateOrderNoToBusinessExceptionBeforeWritingItemsOrStock() {
        String todayPrefix = "PI" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        when(skuUnitResolver.resolve(20L, null))
                .thenReturn(new SkuUnitResolver.ResolvedUnit(sku(20L), "bottle", 1));
        when(purchaseInboundMapper.findMaxOrderNo(todayPrefix + "%")).thenReturn(null);
        when(purchaseInboundMapper.insertInbound(any(PurchaseInbound.class)))
                .thenThrow(new DuplicateKeyException("duplicate order_no"));

        assertThatThrownBy(() -> purchaseInboundService.create(request(
                List.of(item(20L, 1, null, "12.50")),
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购入库单号重复，请重试");

        verify(purchaseInboundMapper, never()).insertItems(anyList());
        verify(stockService, never()).increase(any(), any(Integer.class), any());
    }

    private PurchaseInboundRequest request(List<PurchaseInboundItemRequest> items, String remark) {
        PurchaseInboundRequest request = new PurchaseInboundRequest();
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
        return item;
    }

    private Sku sku(Long id) {
        Sku sku = new Sku();
        sku.setId(id);
        return sku;
    }

    private PurchaseInboundVO vo(Long id) {
        PurchaseInboundVO vo = new PurchaseInboundVO();
        vo.setId(id);
        return vo;
    }
}
