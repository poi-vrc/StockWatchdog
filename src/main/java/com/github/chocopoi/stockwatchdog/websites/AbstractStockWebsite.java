package com.github.chocopoi.stockwatchdog.websites;

import com.github.chocopoi.stockwatchdog.ProductItem;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractStockWebsite {

    private final String identifier;

    private final String fullName;

    public AbstractStockWebsite(String identifier, String fullName) {
        this.identifier = identifier;
        this.fullName = fullName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getFullName() {
        return fullName;
    }

    public abstract boolean isWebsiteOnline();

    public abstract Map<String, ProductItem> getAvailableProducts(String exactQuery) throws IOException;

    public abstract boolean checkProductStock(ProductItem productItem) throws IOException;

}
