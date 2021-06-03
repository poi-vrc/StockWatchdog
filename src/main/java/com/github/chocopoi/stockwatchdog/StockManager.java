package com.github.chocopoi.stockwatchdog;

import com.github.chocopoi.stockwatchdog.reporters.StockReporter;
import com.github.chocopoi.stockwatchdog.websites.AbstractStockWebsite;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

public class StockManager {

    private static final Logger logger = LogManager.getLogger(StockManager.class);

    private final AbstractStockWebsite[] websites;

    private final StockQuery[] queries;

    private final StockReporter[] reporters;

    private StockDatabase stockDatabase;

    private final Gson gson;

    private final String pathToDatabaseJson;

    private final Timer timer;

    private final StockUpdateTimerTask timerTask;

    public StockManager(AbstractStockWebsite[] websites, StockQuery[] queries, StockReporter[] reporters, String pathToDatabaseJson) {
        this.websites = websites;
        this.pathToDatabaseJson = pathToDatabaseJson;
        this.queries = queries;
        this.reporters = reporters;
        timer = new Timer();
        timerTask = new StockUpdateTimerTask(this);
        stockDatabase = new StockDatabase();
        gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    public void startTimer() {
        timer.schedule(timerTask, 0, 1 * 60 * 1000);
    }

    public void stopTimer() {
        timer.cancel();
    }

    private void reportNewDetectedProduct(ProductItem item) {
        for (int i = 0; i < reporters.length; i++) {
            reporters[i].onNewProductDetected(item);
        }
    }

    private void reportProductStockAvailable(ProductItem item) {
        for (int i = 0; i < reporters.length; i++) {
            reporters[i].onStockAvailable(item);
        }
    }

    public void updateAllAvailableProducts() {
        for (int i = 0; i < queries.length; i++) {
            logger.debug("begin updating all available products of \"" + queries[i].getQueryString() + "\" from websites");
            for (int j = 0; j < websites.length; j++) {
                Map<String, ProductItem> newItems = null;

                try {
                    newItems = websites[j].getAvailableProducts(queries[i].getQueryString());
                } catch (IOException e) {
                    logger.error("Error getting available products from \"" + websites[j].getIdentifier() + "\" website: ", e);
                    continue;
                }

                updateProducts(queries[i], newItems);
            }
        }
    }

    public void loadDatabase() throws IOException {
        File file = new File(pathToDatabaseJson);

        if (!file.exists()) {
            return;
        }

        FileInputStream fis = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        stockDatabase = gson.fromJson(reader, StockDatabase.class);
        reader.close();
    }

    public void saveDatabase() throws IOException {
        File file = new File(pathToDatabaseJson);

        if (!file.exists()) {
            file.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(fos);
        gson.toJson(stockDatabase, writer);
        writer.close();
    }

    public void updateProducts(final StockQuery query, final Map<String, ProductItem> newItems) {
        if (stockDatabase == null) {
            stockDatabase = new StockDatabase();
        }

        if (stockDatabase.products == null) {
            logger.debug("products is null. creating a new hashmap");
            stockDatabase.products = new HashMap<String, QueryStringProducts>();
        }

        String stockQueryId = query.getStockQueryIdentifier();

        if (!stockDatabase.products.containsKey(stockQueryId)) {
            stockDatabase.products.put(stockQueryId, new QueryStringProducts());
        }
        QueryStringProducts queryStringProducts = stockDatabase.products.get(stockQueryId);

        Iterator<String> it = newItems.keySet().iterator();

        ProductItem oldItem;
        ProductItem newItem;
        String key;
        logger.debug("iterating all keys of new items received");
        while (it.hasNext()) {
            key = it.next();
            oldItem = null;
            newItem = newItems.get(key);

            if (queryStringProducts.containsKey(key)) {
                oldItem = queryStringProducts.get(key);
                newItem.firstDetectedTimestamp = oldItem.firstDetectedTimestamp;
                newItem.lastInStockTimestamp = oldItem.lastInStockTimestamp;
            } else {
                newItem.firstDetectedTimestamp = newItem.updatedTimestamp;
                newItem.lastInStockTimestamp = -1;
                reportNewDetectedProduct(newItem);
                logger.info("New product detected for \"" + newItem.productFullName + "\":" + newItem.url);
            }

            if (newItem.isInStock()) {
                newItem.lastInStockTimestamp = newItem.updatedTimestamp;
            }

            if ((oldItem != null && newItem.isInStock() && !oldItem.isInStock()) ||
                    (oldItem == null && newItem.isInStock())
            ) {
                reportProductStockAvailable(newItem);
                logger.info("Stock available for \"" + newItem.productFullName + "\":" + newItem.url);
            }

            queryStringProducts.put(key, newItem);
        }
    }

}
