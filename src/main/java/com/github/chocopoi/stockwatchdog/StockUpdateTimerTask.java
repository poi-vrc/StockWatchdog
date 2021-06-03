package com.github.chocopoi.stockwatchdog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.TimerTask;

public class StockUpdateTimerTask extends TimerTask {

    private static final Logger logger = LogManager.getLogger(StockUpdateTimerTask.class);

    private final StockManager stockManager;

    public StockUpdateTimerTask(StockManager stockManager) {
        this.stockManager = stockManager;
    }

    @Override
    public void run() {
        logger.info("Start executing scheduled stock update task.");
        stockManager.updateAllAvailableProducts();
        try {
            stockManager.saveDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Executed scheduled stock update task.");
    }
}
