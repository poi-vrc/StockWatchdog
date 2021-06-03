package com.github.chocopoi.stockwatchdog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class ProductItemGroup implements Serializable {

    public String productItemGroupIdentifier;

    public String productItemGroupName;

    public List<String> productItemIdentifiers;

    public void addIfCanBeGrouped(ProductItem productItem) {
        if (productItemIdentifiers == null) {
            productItemIdentifiers = new ArrayList<String>();
        }

        if (!productItemIdentifiers.contains(productItem.productItemIdentifier) && isProductItemThisGroup(productItem)) {
            productItemIdentifiers.add(productItem.productItemIdentifier);
        }
    }

    public abstract boolean isProductItemThisGroup(ProductItem productItem);

}
