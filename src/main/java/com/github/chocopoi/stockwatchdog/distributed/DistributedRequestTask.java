package com.github.chocopoi.stockwatchdog.distributed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.UUID;
import java.util.concurrent.Callable;

public class DistributedRequestTask implements Callable<Document> {

    private static final int MAXIMUM_FIND_NODE_RETIRES = 6;

    private static final int REQUEST_TIMEOUT = 150000;

    private static final Logger logger = LogManager.getLogger(DistributedRequestTask.class);

    private final DistributedRequestServer drs;

    private final String url;

    public DistributedRequestTask(DistributedRequestServer drs, String url) {
        this.drs = drs;
        this.url = url;
    }

    @Override
    public Document call() throws Exception {
        //wait until a free node is found
        DistributedNode node;
        int retries = 0;
        while ((node = drs.getFreeNode()) == null) {
            if (retries >= MAXIMUM_FIND_NODE_RETIRES) {
                logger.warn("aborting find free node (retries: " + (retries) + "/" + MAXIMUM_FIND_NODE_RETIRES + ")");
                return null;
            }
            retries++;
            logger.warn("no free distribution node available, sleeping for 5000ms before retrying (retries: " + (retries) + "/" + MAXIMUM_FIND_NODE_RETIRES + ")");
            Thread.sleep(5000);
        }

        //request task
        UUID taskUuid = UUID.randomUUID();
        String uuidStr = taskUuid.toString();
        logger.debug("requesting node " + node.getUuid().toString() + " to run task " + url + " with ID " + uuidStr);

        TaskResultEvent dummyInProgressEvent = new TaskResultEvent();
        dummyInProgressEvent.result = Integer.MAX_VALUE;

        drs.getTaskResults().put(uuidStr, dummyInProgressEvent);
        node.requestHtml(taskUuid.toString(), url);

        //wait for task result
        logger.debug("awaiting task result to return from node");
        TaskResultEvent taskResult = null;
        long startTime = System.currentTimeMillis();
        while (true) {
            taskResult = drs.getTaskResults().get(uuidStr);
            if (taskResult.result != Integer.MAX_VALUE) {
                break;
            }

            if (System.currentTimeMillis() - startTime >= REQUEST_TIMEOUT) {
                drs.getTaskResults().remove(uuidStr);
                logger.error("request timeout (" + REQUEST_TIMEOUT + "ms) from node " + node.getUuid().toString() + " to run task " + url + " with ID " + uuidStr);
                return null;
            }
            Thread.sleep(1000);
        }
        logger.debug("task result received from node " + node.getUuid().toString() + " to run task ID " + uuidStr);

        drs.getTaskResults().remove(uuidStr);
        if (taskResult.result == 0) {
            return Jsoup.parse(taskResult.data);
        } else {
            logger.error("node returned non-zero result code: " + taskResult.result + " data: " + taskResult.data);
            return null;
        }
    }

}
