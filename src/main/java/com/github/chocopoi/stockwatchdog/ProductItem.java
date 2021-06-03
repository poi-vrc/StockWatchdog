package com.github.chocopoi.stockwatchdog;

import java.io.Serializable;

public class ProductItem implements Serializable {

    public String productItemIdentifier;

    public String stockWebsiteIdentifier;

    public String productFullName;

    public String url;

    public String imageUrl;

    public String currency;

    public float price;

    public boolean inStock;

    public long updatedTimestamp;

    public long firstDetectedTimestamp;

    public long lastInStockTimestamp;

    public long getFirstDetectedTimestamp() {
        return firstDetectedTimestamp;
    }

    public void setFirstDetectedTimestamp(long firstDetectedTimestamp) {
        this.firstDetectedTimestamp = firstDetectedTimestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProductItemIdentifier() {
        return productItemIdentifier;
    }

    public void setProductItemIdentifier(String productItemIdentifier) {
        this.productItemIdentifier = productItemIdentifier;
    }

    public String getStockWebsiteIdentifier() {
        return stockWebsiteIdentifier;
    }

    public void setStockWebsiteIdentifier(String stockWebsiteIdentifier) {
        this.stockWebsiteIdentifier = stockWebsiteIdentifier;
    }

    public long getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public void setUpdatedTimestamp(long updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
    }

    public long getLastInStockTimestamp() {
        return lastInStockTimestamp;
    }

    public void setLastInStockTimestamp(long lastInStockTimestamp) {
        this.lastInStockTimestamp = lastInStockTimestamp;
    }

    public String getProductFullName() {
        return productFullName;
    }

    public void setProductFullName(String productFullName) {
        this.productFullName = productFullName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }
}
