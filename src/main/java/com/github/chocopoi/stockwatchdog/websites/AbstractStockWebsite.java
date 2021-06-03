package com.github.chocopoi.stockwatchdog.websites;

import com.github.chocopoi.stockwatchdog.ProductItem;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractStockWebsite {

    private final String identifier;

    public AbstractStockWebsite(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public abstract boolean isWebsiteOnline();

    public abstract Map<String, ProductItem> getAvailableProducts(String exactQuery) throws IOException;

    public abstract boolean checkProductStock(ProductItem productItem) throws IOException;

}
