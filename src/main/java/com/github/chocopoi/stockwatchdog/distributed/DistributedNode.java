package com.github.chocopoi.stockwatchdog.distributed;

import com.corundumstudio.socketio.SocketIOClient;

import java.util.UUID;
import java.util.concurrent.*;

public class DistributedNode {

    private final SocketIOClient client;

    private final String name;

    private long lastUsedTimestamp = -1;

    public DistributedNode(SocketIOClient client, String name) {
        this.client = client;
        this.name = name;
    }

    public UUID getUuid() {
        return client.getSessionId();
    }

    public SocketIOClient getClient() {
        return client;
    }

    public void requestHtml(String taskId, String url) throws ExecutionException, InterruptedException, TimeoutException {
        client.sendEvent("task", new TaskEvent(taskId, url));
    }

    public String getName() {
        return name;
    }

    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long lastUsedTimestamp) {
        this.lastUsedTimestamp = lastUsedTimestamp;
    }
}
