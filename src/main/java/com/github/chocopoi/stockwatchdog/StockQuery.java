package com.github.chocopoi.stockwatchdog;

public class StockQuery {

    private final String stockQueryIdentifier;

    private final String name;

    private final String description;

    private final String queryString;

    public StockQuery(String stockQueryIdentifier, String name, String description, String queryString) {
        this.stockQueryIdentifier = stockQueryIdentifier;
        this.name = name;
        this.description = description;
        this.queryString = queryString;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getStockQueryIdentifier() {
        return stockQueryIdentifier;
    }
}
