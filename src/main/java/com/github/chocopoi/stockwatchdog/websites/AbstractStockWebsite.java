package com.github.chocopoi.stockwatchdog.websites;

import com.github.chocopoi.stockwatchdog.ProductItem;
import com.github.chocopoi.stockwatchdog.distributed.DistributedNode;
import com.github.chocopoi.stockwatchdog.distributed.DistributedRequestServer;
import com.github.chocopoi.stockwatchdog.distributed.DistributedRequestTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

public abstract class AbstractStockWebsite {

    private static final Logger logger = LogManager.getLogger(AbstractStockWebsite.class);

    private final String identifier;

    private final String fullName;

    private final DistributedRequestServer drs;

    public AbstractStockWebsite(String identifier, String fullName, DistributedRequestServer drs) {
        this.identifier = identifier;
        this.fullName = fullName;
        this.drs = drs;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getFullName() {
        return fullName;
    }

    public static void writeToFile(String path, String fileName, String content) {
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(path + "/" + fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            writer.println(content);
            writer.close();
        } catch (IOException e) {
            logger.error("error writing to file", e);
        }
    }

    public final HttpURLConnection prepareUrlConnection(String urlStr, String targetOriginReferer) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //Fake environmental headers
        conn.setRequestProperty("Connection", "Keep-alive");
        conn.setRequestProperty("Cache-Control", "max-age=0");
        conn.setRequestProperty("Upgrade-Insecure-Requests", "0");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36");
        conn.setRequestProperty("Origin", targetOriginReferer);
        conn.setRequestProperty("Referer", targetOriginReferer);
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        conn.setRequestProperty("DNT", "1");
        conn.setRequestProperty("Accept-Encoding", "gzip"); //Accept gzip encoding
        conn.setRequestProperty("Accept-Language", "en-US;en;q=0.8,en;q=0.6");

        return conn;
    }

    public final InputStream prepareInputStream(URLConnection conn) throws IOException {
        String encoding = conn.getHeaderField("Content-Encoding");
        InputStream in;
        if (encoding != null && encoding.equals("gzip")) {
            in = new GZIPInputStream(conn.getInputStream());
        } else {
            in = conn.getInputStream();
        }
        return in;
    }

    public final Document getJsoupDocument(String url, String targetOriginReferer) throws Exception {
        if (drs != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            Future<Document> result = executor.submit(new DistributedRequestTask(drs, url));
            return result.get(30000, TimeUnit.MILLISECONDS);
        } else {
            HttpURLConnection conn = prepareUrlConnection(url, targetOriginReferer);
            return Jsoup.parse(prepareInputStream(conn), "UTF-8", url);
        }
    }

    public abstract boolean isWebsiteOnline();

    public abstract Map<String, ProductItem> getAvailableProducts(String exactQuery) throws Exception;

    public abstract boolean checkProductStock(ProductItem productItem) throws Exception;

}
