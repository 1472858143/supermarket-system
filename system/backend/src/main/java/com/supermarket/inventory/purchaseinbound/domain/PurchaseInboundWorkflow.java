package com.supermarket.inventory.purchaseinbound.domain;

import com.supermarket.inventory.common.exception.BusinessException;

import java.util.EnumMap;
import java.util.Map;

public final class PurchaseInboundWorkflow {

    private static final Map<PurchaseInboundStatus, Map<PurchaseInboundAction, PurchaseInboundStatus>> TRANSITIONS =
            new EnumMap<>(PurchaseInboundStatus.class);

    static {
        allow(PurchaseInboundStatus.DRAFT, PurchaseInboundAction.SUBMIT, PurchaseInboundStatus.SUBMITTED);
        allow(PurchaseInboundStatus.SUBMITTED, PurchaseInboundAction.APPROVE, PurchaseInboundStatus.APPROVED);
        allow(PurchaseInboundStatus.SUBMITTED, PurchaseInboundAction.RETURN, PurchaseInboundStatus.RETURNED);
        allow(PurchaseInboundStatus.RETURNED, PurchaseInboundAction.SUBMIT, PurchaseInboundStatus.SUBMITTED);

        allow(PurchaseInboundStatus.DRAFT, PurchaseInboundAction.CANCEL, PurchaseInboundStatus.CANCELLED);
        allow(PurchaseInboundStatus.SUBMITTED, PurchaseInboundAction.CANCEL, PurchaseInboundStatus.CANCELLED);
        allow(PurchaseInboundStatus.RETURNED, PurchaseInboundAction.CANCEL, PurchaseInboundStatus.CANCELLED);
        allow(PurchaseInboundStatus.APPROVED, PurchaseInboundAction.CANCEL, PurchaseInboundStatus.CANCELLED);

        allow(PurchaseInboundStatus.APPROVED, PurchaseInboundAction.RECEIPT_PARTIAL, PurchaseInboundStatus.PARTIALLY_INBOUNDED);
        allow(PurchaseInboundStatus.APPROVED, PurchaseInboundAction.RECEIPT_COMPLETE, PurchaseInboundStatus.INBOUNDED);
        allow(PurchaseInboundStatus.PARTIALLY_INBOUNDED, PurchaseInboundAction.RECEIPT_PARTIAL, PurchaseInboundStatus.PARTIALLY_INBOUNDED);
        allow(PurchaseInboundStatus.PARTIALLY_INBOUNDED, PurchaseInboundAction.RECEIPT_COMPLETE, PurchaseInboundStatus.INBOUNDED);
        allow(PurchaseInboundStatus.PARTIALLY_INBOUNDED, PurchaseInboundAction.CLOSE, PurchaseInboundStatus.CLOSED);
    }

    private PurchaseInboundWorkflow() {
    }

    public static PurchaseInboundStatus next(PurchaseInboundStatus current, PurchaseInboundAction action) {
        if (current == null || action == null) {
            throw new BusinessException("采购单状态操作不能为空");
        }
        PurchaseInboundStatus next = TRANSITIONS.getOrDefault(current, Map.of()).get(action);
        if (next == null) {
            throw new BusinessException("当前采购单状态不允许执行该操作");
        }
        return next;
    }

    public static boolean isPlanEditable(PurchaseInboundStatus status) {
        return status == PurchaseInboundStatus.DRAFT || status == PurchaseInboundStatus.RETURNED;
    }

    public static boolean isReceivable(PurchaseInboundStatus status) {
        return status == PurchaseInboundStatus.APPROVED || status == PurchaseInboundStatus.PARTIALLY_INBOUNDED;
    }

    private static void allow(PurchaseInboundStatus from, PurchaseInboundAction action, PurchaseInboundStatus to) {
        TRANSITIONS.computeIfAbsent(from, ignored -> new EnumMap<>(PurchaseInboundAction.class)).put(action, to);
    }
}
