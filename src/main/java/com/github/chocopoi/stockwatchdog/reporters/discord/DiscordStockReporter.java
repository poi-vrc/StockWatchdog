package com.github.chocopoi.stockwatchdog.reporters.discord;

import com.github.chocopoi.stockwatchdog.ProductItem;
import com.github.chocopoi.stockwatchdog.reporters.StockReporter;
import com.google.gson.Gson;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class DiscordStockReporter extends ListenerAdapter implements StockReporter {

    private static final Logger logger = LogManager.getLogger(DiscordStockReporter.class);

    private static final GatewayIntent[] INTENTS = new GatewayIntent[]{GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS};

    private final Gson gson;

    private JDA jda;

    private User ownerUser;

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
                jda = JDABuilder
                        .createLight(settings.token, Arrays.asList(INTENTS))
                        .addEventListeners(this)
                        .setActivity(Activity.playing(settings.customPlayingStatus))
                        .build();

                logger.info("awaiting discord to be ready...");
                jda.awaitReady();
                logger.info("discord client has initialized successfully");

                logger.info("retrieving owner user: " + settings.ownerUserId);

                ready = true;
            } catch (LoginException | InterruptedException e) {
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

    private void testChannelMessageSend(String channelId) {
        MessageChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(new MessageEmbed(
                    "http://www.example.com/",
                    "Test Message",
                    "**Testing** if I can send message to this channel.",
                    EmbedType.RICH,
                    null,
                    0,
                    null, null, null, null, null, null, null
            )).queue();
        } else {
            logger.warn("the channel with ID \"" + channelId + "\" returns a null TextChannel.");
        }
    }

    private void broadcastEmbed(MessageEmbed embed) {
        for (String channelId : settings.targetChannelIds) {
            TextChannel channel = (TextChannel) jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(embed).queue();
            } else {
                logger.warn("the channel with ID \"" + channelId + "\" returns a null TextChannel.");
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();
        String authorId = msg.getAuthor().getId();

        if (authorId.equals(jda.getSelfUser().getId())) {
            return;
        }

        if (!authorId.equals(settings.ownerUserId)) {
            logger.debug("message author (" + msg.getAuthor().getId() + ") is not owner (" + settings.ownerUserId + "), skipping");
            return;
        }

        if (msg.getContentRaw().equals("sw+register")) {
            MessageChannel channel = event.getChannel();
            try {
                if (!settings.targetChannelIds.contains(channel.getId())) {
                    settings.targetChannelIds.add(channel.getId());
                    saveSettings();
                    channel.sendMessage("This channel will be used to announce stock updates.").queue();
                    testChannelMessageSend(channel.getId());
                } else {
                    channel.sendMessage("This channel has already been registered to announce stock updates before.").queue();
                }
            } catch (IOException e) {
                channel.sendMessage("Error saving settings for registering this channel ID.").queue();
                logger.error("error saving discord settings", e);
            }
        } else if (msg.getContentRaw().equals("sw+unregister")) {
            MessageChannel channel = event.getChannel();
            try {
                if (settings.targetChannelIds.contains(channel.getId())) {
                    settings.targetChannelIds.remove(channel.getId());
                    saveSettings();
                    channel.sendMessage("This channel will no longer be used to announce stock updates.").queue();
                } else {
                    channel.sendMessage("This channel has not been registered to announce stock updates before.").queue();
                }
            } catch (IOException e) {
                channel.sendMessage("Error saving settings for unregistering this channel ID.").queue();
                logger.error("error saving discord settings", e);
            }
        }
    }

    @Override
    public void onNewProductDetected(ProductItem item) {
        if (ready) {
            broadcastEmbed(new MessageEmbed(
                    item.url,
                    "\uD83D\uDD75 New product detected with stock " + (item.inStock ? "\uD83D\uDFE2" : "\uD83D\uDD34"),
                    "**\"" + item.productFullName + "\"** detected with stock status " + (item.inStock ? "\uD83D\uDFE2" : "\uD83D\uDD34"),
                    EmbedType.LINK,
                    null,
                    0,
                    new MessageEmbed.Thumbnail(item.imageUrl, null, 300, 225),
                    null, null, null, null, null, null
            ));
        } else {
            logger.debug("Unable to send any messages on new product detected because discord is not ready");
        }
    }

    @Override
    public void onStockAvailable(ProductItem item) {
        if (ready) {
            broadcastEmbed(new MessageEmbed(
                    item.url,
                    "✔ Stock Available",
                    "**\"" + item.productFullName + "\"** is now in stock.",
                    EmbedType.LINK,
                    null,
                    0,
                    new MessageEmbed.Thumbnail(item.imageUrl, null, 300, 225),
                    null, null, null, null, null, null
            ));
        } else {
            logger.debug("Unable to send any messages on stock available because discord is not ready");
        }
    }
}
