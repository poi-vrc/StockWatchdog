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
import java.util.*;

public class NeweggStockWebsite extends AbstractStockWebsite{

    private static final Logger logger = LogManager.getLogger(NeweggStockWebsite.class);

    private static final String IDENTIFIER = "newegg";

    private static final String QUERY_URL = "https://www.newegg.com/global/hk-en/p/pl?d=";

    private static final String PAGE_PREFIX = "&page=";

    private static final String CHECK_ONLINE_URL = "https://www.newegg.com/global/hk-en/";

    private long lastRequestTimestamp;

    public NeweggStockWebsite() {
        super(IDENTIFIER);
        lastRequestTimestamp = -1;
    }

    @Override
    public boolean isWebsiteOnline() {
        if (System.currentTimeMillis() - lastRequestTimestamp > 300000) {
            try {
                URLConnection conn = new URL(CHECK_ONLINE_URL).openConnection();
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

        int pageNumber = 1;
        int maxPages = 1;

        Map<String, ProductItem> map = new HashMap<String, ProductItem>();

        //loop all pages
        while (pageNumber <= maxPages) {
            String url = QUERY_URL + queryForUrl + PAGE_PREFIX + pageNumber;
            Document doc = Jsoup.connect(url).get();

            Calendar cal = Calendar.getInstance();
            lastRequestTimestamp = cal.getTimeInMillis();

            //Extract maximum page from pagination
            String paginationStr = doc.select("div.list-tool-pagination span.list-tool-pagination-text strong").first().html();
            String[] paginationStrSplit = paginationStr.split("<!-- -->");
            if (paginationStrSplit.length == 3) {
                try {
                    maxPages = Integer.parseInt(paginationStrSplit[paginationStrSplit.length - 1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    logger.warn("Unable to parse maximum page number from pagination of \"" + url + "\", scanning for single page only.");
                }
            } else {
                logger.warn("Unable to identify pagination for \"" + url + "\", scanning for single page only.");
            }
            logger.debug("Got maximum page number of " + maxPages + " from " + url);

            //Extract all products from this page
            Elements items = doc.select("div.item-cell div.item-container");

            Element el;
            ProductItem item;
            for (int i = 0; i < items.size(); i++) {
                el = items.get(i);

                String fullName = el.select("a.item-title").html();

                //only include those with exact wordings
                if (!fullName.toLowerCase().contains(exactQuery.toLowerCase())){
                    continue;
                }

                item = new ProductItem();

                item.stockWebsiteIdentifier = getIdentifier();
                item.productFullName = fullName;
                item.url = el.select("a.item-title").attr("href");
                item.imageUrl = el.select("a.item-img img").attr("src");

                String itemStockDomId = el.select("div.item-stock").attr("id");
                String itemId;
                if (itemStockDomId.isEmpty()) {
                    logger.warn("Error extracting item ID from item-stock of \"" + item.url + "\", falling back to use the hashCode of URL as identifier");
                    itemId = "hc" + Integer.toUnsignedLong(item.url.hashCode());
                } else {
                    itemId = itemStockDomId.replace("stock_", "");
                }
                item.productItemIdentifier = getIdentifier() + "_" + itemId;

                try {
                    String currencyText = el.select("span.price-current-label").html();
                    String priceText = el.select("li.price-current strong").html();
                    String priceTextSup = el.select("li.price-current sup").html();

                    priceText = priceText.replaceAll(",", "");

                    item.currency = currencyText;
                    item.price = Float.parseFloat(priceText + priceTextSup);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Unable to parse price and currency from " + item.url);
                }

                String promoHtml = el.select("p.item-promo").html();
                if (!promoHtml.contains("OUT OF STOCK")) {
                    item.inStock = true;
                }

                item.updatedTimestamp = cal.getTimeInMillis();

                map.put(item.productItemIdentifier, item);
            }
            pageNumber++;
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean checkProductStock(ProductItem productItem) throws IOException{
        Document doc = Jsoup.connect(productItem.url).get();
        String productFlags = doc.select("div.product-flag").html();
        return !productFlags.contains("OUT OF STOCK");
    }
}
