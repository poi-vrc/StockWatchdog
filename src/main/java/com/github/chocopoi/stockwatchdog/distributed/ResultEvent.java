package com.github.chocopoi.stockwatchdog.distributed;

public class ResultEvent {

    public int result;

    public String message;

    public ResultEvent(int result, String message) {
        this.result = result;
        this.message = message;
    }
}
