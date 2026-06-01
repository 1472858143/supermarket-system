package com.supermarket.inventory.stockbatch.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StockBatchExpirationScheduler {

    private final StockBatchService stockBatchService;

    public StockBatchExpirationScheduler(StockBatchService stockBatchService) {
        this.stockBatchService = stockBatchService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void markExpiredBatchesDaily() {
        stockBatchService.markExpiredBatches(LocalDate.now());
    }
}
