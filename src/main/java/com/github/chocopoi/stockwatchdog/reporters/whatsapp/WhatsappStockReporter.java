package com.github.chocopoi.stockwatchdog.reporters.whatsapp;

import com.github.chocopoi.stockwatchdog.ProductItem;
import com.github.chocopoi.stockwatchdog.reporters.StockReporter;
import com.github.chocopoi.stockwatchdog.websites.AbstractStockWebsite;
import com.google.gson.Gson;
import icu.jnet.whatsjava.ClientActionListener;
import icu.jnet.whatsjava.WAClient;
import icu.jnet.whatsjava.web.WebConversationMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class WhatsappStockReporter extends ClientActionListener implements StockReporter {

    private static final Logger logger = LogManager.getLogger(WhatsappStockReporter.class);

    private final String settingsFilePath;

    private final String authCredentialsPath;

    private final String whatsappQrCodeJpgTargetPath;

    private final Gson gson;

    private WhatsappSettings settings;

    private WAClient wa;

    public WhatsappStockReporter(String settingsFilePath, String authCredentialsPath, String whatsappQrCodeJpgTargetPath) {
        this.settingsFilePath = settingsFilePath;
        this.authCredentialsPath = authCredentialsPath;
        this.whatsappQrCodeJpgTargetPath = whatsappQrCodeJpgTargetPath;
        gson = new Gson();

        try {
            loadSettings();
        } catch (IOException e) {
            settings = new WhatsappSettings();
            logger.warn("Unable to load settings/create new settings file. Using a empty settings file now.");
        }

        initWhatsapp();
    }

    private void initWhatsapp() {
        wa = new WAClient(authCredentialsPath);
        wa.addClientActionListener(this);
        wa.openConnection();
    }

    private void loadSettings() throws IOException {
        logger.debug("loading whatsapp settings");
        File file = new File(settingsFilePath);

        if (!file.exists()) {
            logger.debug("settings JSON does not exist. perform one-time new save");
            settings = new WhatsappSettings();
            saveSettings();
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        settings = gson.fromJson(reader, WhatsappSettings.class);
        reader.close();
    }

    private void saveSettings() throws IOException {
        logger.debug("saving whatsapp settings");
        File file = new File(settingsFilePath);

        if (!file.exists()) {
            logger.debug("settings JSON does not exist. attempting to create a new one");
            file.createNewFile();
        }

        FileWriter writer = new FileWriter(file);
        gson.toJson(settings, writer);
        writer.close();
    }

    @Override
    public void onQRCodeScanRequired(BufferedImage img) {
        try {
            File file = new File(whatsappQrCodeJpgTargetPath);
            if (!file.exists()) {
                file.createNewFile();
            }
            ImageIO.write(img, "jpg", file);
            logger.info("Whatsapp QR code scan required. It is only valid for 20 seconds. Please scan the image file \"" + whatsappQrCodeJpgTargetPath + "\".");
        } catch (IOException e) {
            logger.error("Error writing whatsapp QR code image into \"" + whatsappQrCodeJpgTargetPath + "\"", e);
        }
    }

    @Override
    public void onReceiveLoginResponse(int httpCode) {
        if (httpCode == 200) {
            logger.info("Whatsapp logged in successfully.");
        } else {
            logger.error("Whatsapp login failed: " + httpCode);
        }
    }

    @Override
    public void onWebConversationMessage(WebConversationMessage msg) {
        if (msg.getFromMe()) {
            return;
        }

        if (!msg.getRemoteJid().equals(settings.ownerJid)) {
            logger.debug("message JID (" + msg.getRemoteJid() + ") is not owner JID (" + settings.ownerJid + "), skipping");
            return;
        }

        /*
        if (msg.getMessageTimestamp()) {

        }
        */

        if (msg.getText().equals("sw+register")) {
            try {
                if (!settings.targetJids.contains(msg.getRemoteJid())) {
                    settings.targetJids.add(msg.getRemoteJid());
                    saveSettings();
                    wa.sendMessage(msg.getRemoteJid(), "[SW] This chat has been successfully registered.");
                } else {
                    wa.sendMessage(msg.getRemoteJid(), "[SW] This chat has been registered before.");
                }
            } catch (IOException e) {
                logger.error("error saving whatsapp settings", e);
                wa.sendMessage(msg.getRemoteJid(), "[SW] Error saving whatsapp settings");
            }
        } else if (msg.getText().equals("sw+unregister")) {
            try {
                if (settings.targetJids.contains(msg.getRemoteJid())) {
                    settings.targetJids.remove(msg.getRemoteJid());
                    saveSettings();
                    wa.sendMessage(msg.getRemoteJid(), "[SW] This chat has been successfully unregistered.");
                } else {
                    wa.sendMessage(msg.getRemoteJid(), "[SW] This chat has not been registered.");
                }
            } catch (IOException e) {
                logger.error("error saving whatsapp settings", e);
                wa.sendMessage(msg.getRemoteJid(), "[SW] Error saving whatsapp settings");
            }
        }
    }

    private void broadcastMessage(String message) {
        for (String remoteJid : settings.targetJids) {
            wa.sendMessage(remoteJid, "[SW" + System.currentTimeMillis() + "]\n" + message);
        }
    }

    @Override
    public void onNewProductDetected(AbstractStockWebsite website, ProductItem item) {
        broadcastMessage(
                "\uD83D\uDD75 New product **\"" + item.productFullName + "\"** detected at " + website.getFullName() + " with status " + (item.inStock ? "\uD83D\uDFE2" : "\uD83D\uDD34") +
                        "\nPrice: " + item.currency + "$" + item.price +
                        "\nLink: " + item.url);
    }

    @Override
    public void onStockAvailable(AbstractStockWebsite website, ProductItem item) {
        broadcastMessage(
                "✔ Stock available at " + website.getFullName() + " for **\"" + item.productFullName + "\"**\n" +
                        "\nPrice: " + item.currency + "$" + item.price +
                        "\nLink: " + item.url);
    }
}
