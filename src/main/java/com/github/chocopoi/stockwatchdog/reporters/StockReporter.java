package com.github.chocopoi.stockwatchdog.reporters;

import com.github.chocopoi.stockwatchdog.ProductItem;

public interface StockReporter {

    void onNewProductDetected(ProductItem item);

    void onStockAvailable(ProductItem item);

}
