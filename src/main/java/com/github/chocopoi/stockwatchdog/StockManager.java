package com.github.chocopoi.stockwatchdog;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
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

    private StockWatchdogConfig config;

    private final Gson gson;

    private final Timer timer;

    private final StockUpdateTimerTask timerTask;

    private boolean started;

    private SocketIOServer server;

    public StockManager(AbstractStockWebsite[] websites, StockQuery[] queries, StockReporter[] reporters, StockWatchdogConfig config, StockDatabase stockDatabase) {
        this.websites = websites;
        this.queries = queries;
        this.reporters = reporters;
        this.config = config;
        this.stockDatabase = stockDatabase;

        timer = new Timer();
        timerTask = new StockUpdateTimerTask(this);

        gson = new GsonBuilder().disableHtmlEscaping().create();

        started = false;
    }

    public AbstractStockWebsite getStockWebsiteByIdentifier(String identifier) {
        for (int i = 0; i < websites.length; i++) {
            if (websites[i].getIdentifier().equals(identifier)) {
                return websites[i];
            }
        }
        return null;
    }

    public StockDatabase getStockDatabase() {
        return stockDatabase;
    }

    public StockWatchdogConfig getConfig() {
        return config;
    }

    public void start() {
        if (started) {
            return;
        }

        Configuration socketIoConfig = new Configuration();
        socketIoConfig.setHostname(config.rtsHostName);
        socketIoConfig.setPort(config.rtsPort);
        server = new SocketIOServer(socketIoConfig);

        timer.schedule(timerTask, 0, config.crawlFrequency);

        started = true;
    }

    public void stop() {
        if (!started) {
            return;
        }
        timer.cancel();
        server.stop();

        started = false;
    }

    private void reportNewDetectedProduct(ProductItem item) {
        AbstractStockWebsite website = getStockWebsiteByIdentifier(item.stockWebsiteIdentifier);
        for (int i = 0; i < reporters.length; i++) {
            reporters[i].onNewProductDetected(website, item);
        }
    }

    private void reportProductStockAvailable(ProductItem item) {
        AbstractStockWebsite website = getStockWebsiteByIdentifier(item.stockWebsiteIdentifier);
        for (int i = 0; i < reporters.length; i++) {
            reporters[i].onStockAvailable(website, item);
        }
    }

    public void updateAllAvailableProducts() {
        for (int i = 0; i < queries.length; i++) {
            logger.debug("begin updating all available products of \"" + queries[i].getQueryString() + "\" from websites");
            for (int j = 0; j < websites.length; j++) {
                Map<String, ProductItem> newItems = null;

                try {
                    newItems = websites[j].getAvailableProducts(queries[i].getQueryString());
                } catch (Exception e) {
                    logger.error("Error getting available products from \"" + websites[j].getIdentifier() + "\" website: ", e);
                    continue;
                }

                updateProducts(queries[i], newItems);
            }
        }
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
