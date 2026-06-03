package com.supermarket.inventory.purchaseinbound.domain;

import com.supermarket.inventory.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseInboundWorkflowTest {

    @Test
    void next_allowsConfirmedPlanTransitions() {
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.DRAFT, PurchaseInboundAction.SUBMIT))
                .isEqualTo(PurchaseInboundStatus.SUBMITTED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.SUBMITTED, PurchaseInboundAction.APPROVE))
                .isEqualTo(PurchaseInboundStatus.APPROVED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.SUBMITTED, PurchaseInboundAction.RETURN))
                .isEqualTo(PurchaseInboundStatus.RETURNED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.RETURNED, PurchaseInboundAction.SUBMIT))
                .isEqualTo(PurchaseInboundStatus.SUBMITTED);
    }

    @Test
    void next_allowsReceiptProgressTransitionsWithoutApprovalAction() {
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.APPROVED, PurchaseInboundAction.RECEIPT_PARTIAL))
                .isEqualTo(PurchaseInboundStatus.PARTIALLY_INBOUNDED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.APPROVED, PurchaseInboundAction.RECEIPT_COMPLETE))
                .isEqualTo(PurchaseInboundStatus.INBOUNDED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.PARTIALLY_INBOUNDED, PurchaseInboundAction.RECEIPT_PARTIAL))
                .isEqualTo(PurchaseInboundStatus.PARTIALLY_INBOUNDED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.PARTIALLY_INBOUNDED, PurchaseInboundAction.RECEIPT_COMPLETE))
                .isEqualTo(PurchaseInboundStatus.INBOUNDED);
    }

    @Test
    void next_allowsCancelAndCloseBoundaries() {
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.DRAFT, PurchaseInboundAction.CANCEL))
                .isEqualTo(PurchaseInboundStatus.CANCELLED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.SUBMITTED, PurchaseInboundAction.CANCEL))
                .isEqualTo(PurchaseInboundStatus.CANCELLED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.RETURNED, PurchaseInboundAction.CANCEL))
                .isEqualTo(PurchaseInboundStatus.CANCELLED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.APPROVED, PurchaseInboundAction.CANCEL))
                .isEqualTo(PurchaseInboundStatus.CANCELLED);
        assertThat(PurchaseInboundWorkflow.next(PurchaseInboundStatus.PARTIALLY_INBOUNDED, PurchaseInboundAction.CLOSE))
                .isEqualTo(PurchaseInboundStatus.CLOSED);
    }

    @Test
    void next_rejectsInvalidTransitions() {
        assertThatThrownBy(() -> PurchaseInboundWorkflow.next(PurchaseInboundStatus.DRAFT, PurchaseInboundAction.APPROVE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前采购单状态不允许执行该操作");
        assertThatThrownBy(() -> PurchaseInboundWorkflow.next(PurchaseInboundStatus.INBOUNDED, PurchaseInboundAction.CANCEL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前采购单状态不允许执行该操作");
        assertThatThrownBy(() -> PurchaseInboundWorkflow.next(PurchaseInboundStatus.APPROVED, PurchaseInboundAction.RETURN))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前采购单状态不允许执行该操作");
    }

    @Test
    void next_rejectsNullArguments() {
        assertThatThrownBy(() -> PurchaseInboundWorkflow.next(null, PurchaseInboundAction.SUBMIT))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购单状态操作不能为空");
        assertThatThrownBy(() -> PurchaseInboundWorkflow.next(PurchaseInboundStatus.DRAFT, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("采购单状态操作不能为空");
    }

    @Test
    void isPlanEditable_onlyAllowsDraftAndReturned() {
        for (PurchaseInboundStatus status : PurchaseInboundStatus.values()) {
            assertThat(PurchaseInboundWorkflow.isPlanEditable(status))
                    .as("status %s", status)
                    .isEqualTo(status == PurchaseInboundStatus.DRAFT || status == PurchaseInboundStatus.RETURNED);
        }
    }

    @Test
    void isReceivable_onlyAllowsApprovedAndPartiallyInbounded() {
        for (PurchaseInboundStatus status : PurchaseInboundStatus.values()) {
            assertThat(PurchaseInboundWorkflow.isReceivable(status))
                    .as("status %s", status)
                    .isEqualTo(status == PurchaseInboundStatus.APPROVED
                            || status == PurchaseInboundStatus.PARTIALLY_INBOUNDED);
        }
    }
}
