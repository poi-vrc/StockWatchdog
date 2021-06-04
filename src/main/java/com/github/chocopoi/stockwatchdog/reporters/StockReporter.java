package com.github.chocopoi.stockwatchdog.reporters;

import com.github.chocopoi.stockwatchdog.ProductItem;
import com.github.chocopoi.stockwatchdog.websites.AbstractStockWebsite;

public interface StockReporter {

    void onNewProductDetected(AbstractStockWebsite website, ProductItem item);

    void onStockAvailable(AbstractStockWebsite website, ProductItem item);

}
