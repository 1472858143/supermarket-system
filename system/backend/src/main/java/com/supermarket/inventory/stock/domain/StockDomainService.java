package com.supermarket.inventory.stock.domain;

import com.supermarket.inventory.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class StockDomainService {

    /**
     * 库存增加只校验变化量，具体记录来源由入库等业务服务负责。
     */
    public int increase(int currentQuantity, int changeQuantity) {
        validatePositiveQuantity(changeQuantity);
        return currentQuantity + changeQuantity;
    }

    /**
     * 出库扣减必须在领域层统一校验，避免不同业务入口各自实现库存不足判断。
     */
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

    /**
     * 上下限规则集中在库存领域层，保证页面和接口复用同一套约束。
     */
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
