package com.github.chocopoi.stockwatchdog.websites;

import com.github.chocopoi.stockwatchdog.ProductItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

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
        String queryForUrl = exactQuery.replaceAll(" +", "+");

        String url = QUERY_URL + queryForUrl + SUFFIX;

        Map<String, ProductItem> map = new HashMap<String, ProductItem>();

        boolean hasNextBtn = true;
        while (hasNextBtn) {
            Document doc = Jsoup.connect(url).get();
            long timeNow = System.currentTimeMillis();
            lastRequestTimestamp = timeNow;

            //obtain next pagination
            url = doc.select("ul.a-pagination a-last a").attr("href");
            hasNextBtn = !url.isEmpty();

            ProductItem item;
            Elements els = doc.select("s-result-item");
            for (Element el : els) {
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

                Element priceEl = el.selectFirst("span.a-price a-offscreen");

                if (priceEl != null) {
                    String html = priceEl.html();
                    if (html.length() == 0) {
                        logger.warn("Price string is empty, skipping price parse");
                        item.currency = null;
                        item.price = -1;
                    } else {
                        html = html.substring(0);
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

                item.productFullName = el.select("div h2 a span").html();
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
