package com.github.chocopoi.stockwatchdog.reporters.discord;

import com.github.chocopoi.stockwatchdog.ProductItem;
import com.github.chocopoi.stockwatchdog.StockUpdateTimerTask;
import com.github.chocopoi.stockwatchdog.reporters.StockReporter;
import com.google.gson.Gson;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.io.*;

public class DiscordStockReporter implements StockReporter {

    private static final Logger logger = LogManager.getLogger(DiscordStockReporter.class);

    private final Gson gson;

    private JDA jda;

    private User user;

    private final String settingsFilePath;

    private DiscordSettings settings;

    private boolean ready;

    public DiscordStockReporter(String settingsFilePath) {
        gson = new Gson();
        this.settingsFilePath = settingsFilePath;
        ready = false;

        try {
            loadSettings();
        } catch (IOException e) {
            settings = new DiscordSettings();
            logger.warn("Unable to load settings/create new settings file. Using a empty settings file now.");
        }

        if (settings.isValid()) {
            initDiscord();
        } else {
            logger.error("discord settings is invalid. Please check your settings.");
        }
    }

    private void initDiscord() {
        if (!ready) {
            logger.info("attempting to initialize discord client");
            try {
                jda = JDABuilder.createDefault(settings.token).build();
                logger.info("discord client has initialized successfully");

                logger.info("retrieving target user: " + settings.targetUserId);
                user = jda.retrieveUserById(settings.targetUserId).complete();

                if (user == null) {
                    logger.error("target user cannot be found.");
                } else {
                    logger.info("retrieved target user successfully");
                }
                ready = true;
            } catch (LoginException e) {
                logger.error("error initializing discord client", e);
            }
        }
    }

    private void loadSettings() throws IOException {
        logger.debug("loading discord settings");
        File file = new File(settingsFilePath);

        if (!file.exists()) {
            logger.debug("settings JSON does not exist. perform one-time new save");
            settings = new DiscordSettings();
            saveSettings();
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        settings = gson.fromJson(reader, DiscordSettings.class);
        reader.close();
    }

    private void saveSettings() throws IOException {
        logger.debug("saving discord settings");
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
    public void onNewProductDetected(ProductItem item) {
        if (ready) {
            user.openPrivateChannel().queue((channel -> {
                channel.sendMessage("\uD83D\uDD75 New product ```" + item.productFullName + "``` detected with stock status " + (item.inStock ? "\uD83D\uDFE2" : "\uD83D\uDD34") + ": " + item.url).queue();
            }));
        } else {
            logger.debug("Unable to send any messages on new product detected because discord is not ready");
        }
    }

    @Override
    public void onStockAvailable(ProductItem item) {
        if (ready) {
            user.openPrivateChannel().queue((channel -> {
                channel.sendMessage("✔ Stock available for ```" + item.productFullName + "```: " + item.url).queue();
            }));
        } else {
            logger.debug("Unable to send any messages on stock available because discord is not ready");
        }
    }
}
