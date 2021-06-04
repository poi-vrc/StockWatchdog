package com.github.chocopoi.stockwatchdog.reporters.whatsapp;

import com.github.chocopoi.stockwatchdog.ProductItem;
import com.github.chocopoi.stockwatchdog.reporters.StockReporter;
import icu.jnet.whatsjava.ClientActionListener;
import icu.jnet.whatsjava.WAClient;
import icu.jnet.whatsjava.web.WebChat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class WhatsappStockReporter extends ClientActionListener implements StockReporter {

    private static final Logger logger = LogManager.getLogger(WhatsappStockReporter.class);

    private final String authCredentialsPath;

    private final String whatsappQrCodeJpgTargetPath;

    private WAClient wa;

    public WhatsappStockReporter(String authCredentialsPath, String whatsappQrCodeJpgTargetPath) {
        this.authCredentialsPath = authCredentialsPath;
        this.whatsappQrCodeJpgTargetPath = whatsappQrCodeJpgTargetPath;
        initWhatsapp();
    }

    private void initWhatsapp() {
        wa = new WAClient(authCredentialsPath);
        wa.addClientActionListener(this);
        wa.openConnection();
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
    public void onWebChat(WebChat[] chats) {

    }

    @Override
    public void onNewProductDetected(ProductItem item) {

    }

    @Override
    public void onStockAvailable(ProductItem item) {

    }
}
