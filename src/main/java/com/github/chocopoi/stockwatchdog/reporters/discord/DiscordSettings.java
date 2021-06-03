package com.github.chocopoi.stockwatchdog.reporters.discord;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordSettings implements Serializable {

    public String token;

    public String ownerUserId;

    public String customPlayingStatus = "people's wallet";

    public List<String> targetChannelIds = new ArrayList<String>();

    public boolean isValid() {
        return token != null && !token.isEmpty() && ownerUserId != null && !ownerUserId.isEmpty();
    }

}
