package com.supermarket.inventory.stock.domain;

import com.supermarket.inventory.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class StockDomainService {

    public int increase(int currentQuantity, int changeQuantity) {
        validatePositiveQuantity(changeQuantity);
        return currentQuantity + changeQuantity;
    }

    public int decrease(int currentQuantity, int changeQuantity) {
        validatePositiveQuantity(changeQuantity);
        if (currentQuantity < changeQuantity) {
            throw new BusinessException("库存不足，无法出库");
        }
        return currentQuantity - changeQuantity;
    }

    public int adjustTo(int actualQuantity) {
        if (actualQuantity < 0) {
            throw new BusinessException("实际库存不能小于0");
        }
        return actualQuantity;
    }

    public void validateLimit(int minStock, int maxStock) {
        if (minStock < 0) {
            throw new BusinessException("库存下限不能小于0");
        }
        if (maxStock < minStock) {
            throw new BusinessException("库存上限不能小于库存下限");
        }
    }

    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("库存变化数量必须大于0");
        }
    }
}
