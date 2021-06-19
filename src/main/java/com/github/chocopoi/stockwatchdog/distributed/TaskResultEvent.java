package com.github.chocopoi.stockwatchdog.distributed;

import java.io.Serializable;

public class TaskResultEvent implements Serializable {

    public String token;

    public String taskId;

    public int result;

    public String data;

}
