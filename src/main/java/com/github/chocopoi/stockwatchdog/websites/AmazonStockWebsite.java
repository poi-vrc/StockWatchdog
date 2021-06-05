package com.github.chocopoi.stockwatchdog.websites;

import com.github.chocopoi.stockwatchdog.ProductItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class AmazonStockWebsite extends AbstractStockWebsite {

    private static final Logger logger = LogManager.getLogger(AmazonStockWebsite.class);

    private static final String IDENTIFIER = "amazon";

    private static final String FULL_NAME = "Amazon";

    private static final String DOMAIN_URL = "https://www.amazon.com";

    private static final String QUERY_URL = "https://www.amazon.com/s?k=";

    private static final String SUFFIX = "&page=1&language=en_US";

    private long lastRequestTimestamp;

    public AmazonStockWebsite() {
        super(IDENTIFIER, FULL_NAME);
        lastRequestTimestamp = -1;
    }

    @Override
    public boolean isWebsiteOnline() {
        if (System.currentTimeMillis() - lastRequestTimestamp > 300000) {
            try {
                URLConnection conn = new URL(DOMAIN_URL).openConnection();
                conn.connect();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public Map<String, ProductItem> getAvailableProducts(String exactQuery) throws IOException {
        logger.debug("begin crawling amazon available products for \"" + exactQuery + "\"");
        String queryForUrl = exactQuery.replaceAll(" +", "+");

        String url = QUERY_URL + queryForUrl + SUFFIX;

        Map<String, ProductItem> map = new HashMap<String, ProductItem>();

        boolean hasNextBtn = true;
        while (hasNextBtn) {
            logger.debug("crawling at " + url);
            HttpURLConnection conn = prepareUrlConnection(url, DOMAIN_URL);
            Document doc = Jsoup.parse(prepareInputStream(conn), "UTF-8", url);
            long timeNow = System.currentTimeMillis();
            lastRequestTimestamp = timeNow;

            writeToFile("debug", "amazon_debug_" + timeNow + ".html", doc.html());

            //obtain next pagination
            String paginationUrl = doc.select("ul.a-pagination li.a-last a").attr("href");
            if (!paginationUrl.isEmpty()) {
                url = DOMAIN_URL + paginationUrl;
                hasNextBtn = true;
            } else {
                hasNextBtn = false;
            }
            logger.debug("next pagination: \"" + url + "\" hasNextBtn: " + hasNextBtn);

            ProductItem item;
            Elements els = doc.select("div[data-asin]");
            logger.debug("looping " + els.size() + " found s-result-items");
            for (Element el : els) {
                String fullName = el.select("div h2 a span").html();

                //only include those with exact wordings
                if (!fullName.toLowerCase().contains(exactQuery.toLowerCase())) {
                    continue;
                }

                String id = el.attr("data-asin");

                if (id.isEmpty()) {
                    logger.error("result does not have a \"data-asin\" attribute, skipping");
                    continue;
                }

                item = new ProductItem();

                item.productItemIdentifier = getIdentifier() + "_" + id;
                item.stockWebsiteIdentifier = getIdentifier();
                item.url = DOMAIN_URL + el.selectFirst("a").attr("href");
                item.imageUrl = el.select("img").attr("src");

                Element priceEl = el.selectFirst("span.a-price span.a-offscreen");

                if (priceEl != null) {
                    String html = priceEl.html();
                    if (html.length() < 2) {
                        logger.warn("Price string is empty, skipping price parse");
                        item.currency = null;
                        item.price = -1;
                    } else {
                        html = html.substring(1).replaceAll(",", "");
                        item.currency = "USD"; //amazon defaults to USD
                        try {
                            item.price = Float.parseFloat(html);
                        } catch (NumberFormatException e) {
                            logger.error("error parsing price float \"" + html + "\" of " + item.url, e);
                        }
                    }
                    item.inStock = true;
                } else {
                    //item.currency = null;
                    //item.price = -1;
                    item.inStock = false;
                }

                item.productFullName = fullName;
                item.updatedTimestamp = timeNow;

                map.put(item.productItemIdentifier, item);
            }
        }
        return map;
    }

    @Override
    public boolean checkProductStock(ProductItem productItem) throws IOException {
        return false;
    }
}
