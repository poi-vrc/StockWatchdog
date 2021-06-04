package com.github.chocopoi.stockwatchdog;

import com.github.chocopoi.stockwatchdog.reporters.StockReporter;
import com.github.chocopoi.stockwatchdog.reporters.discord.DiscordStockReporter;
import com.github.chocopoi.stockwatchdog.reporters.whatsapp.WhatsappStockReporter;
import com.github.chocopoi.stockwatchdog.websites.AbstractStockWebsite;
import com.github.chocopoi.stockwatchdog.websites.AmazonStockWebsite;
import com.github.chocopoi.stockwatchdog.websites.NeweggStockWebsite;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        StockManager stockManager = new StockManager(new AbstractStockWebsite[]{
                new AmazonStockWebsite(),
                new NeweggStockWebsite()
        }, new StockQuery[]{
                new StockQuery(
                        "rtx-3080-ti",
                        "RTX 3080 Ti",
                        "Watching stock for RTX 3080 Ti in multiple websites.",
                        "rtx 3080 ti")
        }, new StockReporter[]{
                new DiscordStockReporter("discord_settings.json"),
                new WhatsappStockReporter("whatsapp_settings.json", "whatsapp_auth.json", "whatsapp_qr_code.jpg")
        }, "stock_database.json");

        stockManager.loadDatabase();
        stockManager.startTimer();
    }

}
