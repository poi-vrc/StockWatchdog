package com.github.chocopoi.stockwatchdog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class StockDatabase implements Serializable {

    private static final Logger logger = LogManager.getLogger(StockDatabase.class);

    public Map<String, QueryStringProducts> products;

    public List<String> productIdIgnores;

    public Map<String, QueryStringProducts> getProducts() {
        return products;
    }

    public void setProducts(Map<String, QueryStringProducts> products) {
        this.products = products;
    }

    public List<String> getProductIdIgnores() {
        return productIdIgnores;
    }

    public void setProductIdIgnores(List<String> productIdIgnores) {
        this.productIdIgnores = productIdIgnores;
    }
}
