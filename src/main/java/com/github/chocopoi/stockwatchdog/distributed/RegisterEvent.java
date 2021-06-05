package com.github.chocopoi.stockwatchdog.distributed;

import java.io.Serializable;

public class RegisterEvent implements Serializable {

    public String action;

    public String name;

    public String token;

}
