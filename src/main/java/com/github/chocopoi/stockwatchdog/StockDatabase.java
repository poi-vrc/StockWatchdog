package com.github.chocopoi.stockwatchdog;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.List;
import java.util.Map;

public class StockDatabase implements Serializable {

    private static final Logger logger = LogManager.getLogger(StockDatabase.class);

    public Map<String, QueryStringProducts> products;

    public List<String> productIdIgnores;

    private String pathToDatabaseJson = null;

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

    public static StockDatabase load(String pathToDatabaseJson) throws IOException {
        File file = new File(pathToDatabaseJson);

        if (!file.exists()) {
            StockDatabase db = new StockDatabase();
            db.pathToDatabaseJson = pathToDatabaseJson;
            db.save();
            return db;
        }

        Gson gson = new Gson();
        FileInputStream fis = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        StockDatabase stockDatabase = gson.fromJson(reader, StockDatabase.class);
        stockDatabase.pathToDatabaseJson = pathToDatabaseJson;
        reader.close();
        return stockDatabase;
    }

    public void save() throws IOException {
        File file = new File(pathToDatabaseJson);

        if (!file.exists()) {
            file.createNewFile();
        }

        Gson gson = new Gson();
        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(fos);
        gson.toJson(this, writer);
        writer.close();
    }
}
