package com.supermarket.inventory.purchaseinbound.service;

import com.supermarket.inventory.auth.security.CurrentUserContext;
import com.supermarket.inventory.common.exception.BusinessException;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundItemRequest;
import com.supermarket.inventory.purchaseinbound.dto.PurchaseInboundRequest;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInbound;
import com.supermarket.inventory.purchaseinbound.entity.PurchaseInboundItem;
import com.supermarket.inventory.purchaseinbound.mapper.PurchaseInboundMapper;
import com.supermarket.inventory.purchaseinbound.vo.PurchaseInboundVO;
import com.supermarket.inventory.sku.service.SkuUnitResolver;
import com.supermarket.inventory.stock.service.StockService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseInboundService {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STOCK_CHANGE_TYPE = "PURCHASE_INBOUND";
    private static final BigDecimal MAX_PURCHASE_PRICE = new BigDecimal("100000000.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000000.00");

    private final PurchaseInboundMapper purchaseInboundMapper;
    private final SkuUnitResolver skuUnitResolver;
    private final StockService stockService;

    public PurchaseInboundService(
            PurchaseInboundMapper purchaseInboundMapper,
            SkuUnitResolver skuUnitResolver,
            StockService stockService
    ) {
        this.purchaseInboundMapper = purchaseInboundMapper;
        this.skuUnitResolver = skuUnitResolver;
        this.stockService = stockService;
    }

    @Transactional
    public PurchaseInboundVO create(PurchaseInboundRequest request) {
        String operator = CurrentUserContext.get().getUsername();
        List<PurchaseInboundItem> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (PurchaseInboundItemRequest requestItem : request.getItems()) {
            SkuUnitResolver.ResolvedUnit resolvedUnit = skuUnitResolver.resolve(requestItem.getSkuId(), requestItem.getUnit());
            int baseQuantity = calculateBaseQuantity(requestItem.getQuantity(), resolvedUnit.conversionRate());
            BigDecimal purchasePrice = validatePurchasePrice(requestItem.getPurchasePrice());
            BigDecimal costPrice = purchasePrice.divide(
                    BigDecimal.valueOf(resolvedUnit.conversionRate()),
                    4,
                    RoundingMode.HALF_UP
            );
            BigDecimal amount = purchasePrice.multiply(BigDecimal.valueOf(requestItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            validateAmount(amount);

            PurchaseInboundItem item = new PurchaseInboundItem();
            item.setSkuId(resolvedUnit.sku().getId());
            item.setQuantity(requestItem.getQuantity());
            item.setUnit(resolvedUnit.unit());
            item.setConversionRate(resolvedUnit.conversionRate());
            item.setBaseQuantity(baseQuantity);
            item.setPurchasePrice(purchasePrice);
            item.setCostPrice(costPrice);
            item.setAmount(amount);
            items.add(item);

            totalQuantity = addBaseQuantity(totalQuantity, baseQuantity);
            totalAmount = totalAmount.add(amount);
            validateAmount(totalAmount);
        }

        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setOrderNo(nextOrderNo());
        inbound.setTotalQuantity(totalQuantity);
        inbound.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        inbound.setStatus(STATUS_COMPLETED);
        inbound.setOperator(operator);
        inbound.setRemark(request.getRemark());

        Long inboundId;
        try {
            inboundId = purchaseInboundMapper.insertInbound(inbound);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("采购入库单号重复，请重试");
        }
        for (PurchaseInboundItem item : items) {
            item.setPurchaseInboundId(inboundId);
        }
        purchaseInboundMapper.insertItems(items);
        for (PurchaseInboundItem item : items) {
            stockService.increase(item.getSkuId(), item.getBaseQuantity(), STOCK_CHANGE_TYPE);
        }
        return purchaseInboundMapper.findById(inboundId)
                .orElseThrow(() -> new BusinessException(404, "采购入库单不存在"));
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
}
