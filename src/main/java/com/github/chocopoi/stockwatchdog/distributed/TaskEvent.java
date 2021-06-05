package com.github.chocopoi.stockwatchdog.distributed;

import java.io.Serializable;

public class TaskEvent implements Serializable {

    public String taskId;

    public String url;

    public TaskEvent(String taskId, String url) {
        this.taskId = taskId;
        this.url = url;
    }

}
