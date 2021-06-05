package com.github.chocopoi.stockwatchdog;

import com.github.chocopoi.stockwatchdog.distributed.DistributedRequestServer;
import com.github.chocopoi.stockwatchdog.reporters.StockReporter;
import com.github.chocopoi.stockwatchdog.reporters.discord.DiscordStockReporter;
import com.github.chocopoi.stockwatchdog.reporters.whatsapp.WhatsappStockReporter;
import com.github.chocopoi.stockwatchdog.websites.AbstractStockWebsite;
import com.github.chocopoi.stockwatchdog.websites.AmazonStockWebsite;
import com.github.chocopoi.stockwatchdog.websites.NeweggStockWebsite;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        StockWatchdogConfig config = StockWatchdogConfig.load("config.json");
        StockDatabase db = StockDatabase.load("stock_database.json");

        DistributedRequestServer drs = new DistributedRequestServer(config);

        StockManager stockManager = new StockManager(new AbstractStockWebsite[]{
                new AmazonStockWebsite(drs),
                new NeweggStockWebsite(drs)
        }, new StockQuery[]{
                new StockQuery(
                        "rtx-3080-ti",
                        "RTX 3080 Ti",
                        "Watching stock for RTX 3080 Ti in multiple websites.",
                        "rtx 3080 ti")
        }, new StockReporter[]{
                //new DiscordStockReporter("discord_settings.json"),
                //new WhatsappStockReporter("whatsapp_settings.json", "whatsapp_auth.json", "whatsapp_qr_code.jpg")
        }, config, db);

        drs.start();
        stockManager.start();
    }

}
