package com.github.chocopoi.stockwatchdog.distributed;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.github.chocopoi.stockwatchdog.StockWatchdogConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedRequestServer {

    private static final Logger logger = LogManager.getLogger(DistributedRequestServer.class);

    private final StockWatchdogConfig config;

    private SocketIOServer server;

    private Map<String, DistributedNode> nodes;

    private Map<String, TaskResultEvent> taskResults;

    private String[] taskDistributionNodes;

    public DistributedRequestServer(StockWatchdogConfig config) {
        nodes = new HashMap<String, DistributedNode>();
        taskResults = new ConcurrentHashMap<String, TaskResultEvent>();
        this.config = config;

        Configuration socketIoConfig = new Configuration();
        socketIoConfig.setHostname(config.drsHostName);
        socketIoConfig.setPort(config.drsPort);
        socketIoConfig.setMaxFramePayloadLength(1024 * 1024);
        socketIoConfig.setMaxHttpContentLength(1024 * 1024);

        server = new SocketIOServer(socketIoConfig);
        server.addEventListener("register", RegisterEvent.class, new RegisterEventListener());
        server.addEventListener("task", TaskResultEvent.class, new TaskResultEventListener());

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                String uuid = socketIOClient.getSessionId().toString();
                if (nodes.containsKey(uuid)) {
                    nodes.remove(uuid);
                }
            }
        });
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public DistributedNode getFreeNode() {
        long timeNow = System.currentTimeMillis();
        Iterator<String> it = nodes.keySet().iterator();
        while (it.hasNext()) {
            DistributedNode dc = nodes.get(it.next());
            if ((dc.getLastUsedTimestamp() + config.drsNodeCoolDown) - timeNow <= 0) {
                dc.setLastUsedTimestamp(System.currentTimeMillis());
                return dc;
            }
        }
        return null;
    }

    public Map<String, TaskResultEvent> getTaskResults() {
        return taskResults;
    }

    private class TaskResultEventListener implements DataListener<TaskResultEvent> {

        @Override
        public void onData(SocketIOClient socketIOClient, TaskResultEvent taskResultEvent, AckRequest ackRequest) throws Exception {
            if (taskResultEvent.token == null || taskResultEvent.taskId == null) {
                return;
            }

            if (!taskResultEvent.token.equals(config.drsAuthToken)) {
                logger.warn("a request attempted to report task result with wrong auth token");
                socketIOClient.sendEvent("task", new ResultEvent(-1, "invalid auth token"));
                return;
            }

            if (!taskResults.containsKey(taskResultEvent.taskId)) {
                logger.warn("a request attempted to report task result of a non-existing task");
                socketIOClient.sendEvent("task", new ResultEvent(-2, "task does not exist"));
                return;
            }

            if (taskResults.get(taskResultEvent.taskId).result != Integer.MAX_VALUE) {
                logger.warn("a request attempted to report task result of an already reported task");
                socketIOClient.sendEvent("task", new ResultEvent(-3, "task already reported"));
                return;
            }

            logger.debug("task result got reported: " + taskResultEvent.taskId);
            taskResults.put(taskResultEvent.taskId, taskResultEvent);
        }

    }

    private class RegisterEventListener implements DataListener<RegisterEvent> {
        @Override
        public void onData(SocketIOClient socketIOClient, RegisterEvent s, AckRequest ackRequest) throws Exception {
            if (s.token == null) {
                return;
            }

            if (!s.token.equals(config.drsAuthToken)) {
                logger.warn("a request attempted to register with wrong auth token");
                socketIOClient.sendEvent("register", new ResultEvent(-1, "invalid auth token"));
                return;
            }

            UUID uuid = socketIOClient.getSessionId();

            if (s.action.equals("register")) {
                if (nodes.containsKey(uuid.toString())) {
                    logger.warn("cannot register duplicated uuid to DRS: " + uuid.toString());
                    socketIOClient.sendEvent("register", new ResultEvent(-2, "duplicated uuid"));
                    return;
                }

                nodes.put(uuid.toString(), new DistributedNode(socketIOClient, s.name));
                socketIOClient.sendEvent("register", new ResultEvent(0, "register successful"));
                logger.debug("registered " + uuid.toString());
            } else if (s.action.equals("unregister")){
                if (!nodes.containsKey(uuid.toString())) {
                    socketIOClient.sendEvent("register", new ResultEvent(-3, "no such uuid"));
                    return;
                }

                nodes.remove(uuid.toString());
                socketIOClient.sendEvent("register", new ResultEvent(0, "unregister successful"));
                logger.debug("unregistered " + uuid.toString());
            } else {
                socketIOClient.sendEvent("register", new ResultEvent(-4, "invalid request"));
                logger.debug("invalid request from " + uuid.toString());
            }
        }
    }

}
