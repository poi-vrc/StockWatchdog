package com.github.chocopoi.stockwatchdog.reporters.discord;

import java.io.Serializable;

public class DiscordSettings implements Serializable {

    public String token;

    public String targetUserId;

    public boolean isValid() {
        return token != null && !token.isEmpty() && targetUserId != null && !targetUserId.isEmpty();
    }

}
