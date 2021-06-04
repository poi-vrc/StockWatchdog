package com.github.chocopoi.stockwatchdog.reporters.whatsapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WhatsappSettings implements Serializable {

    public List<String> targetJids = new ArrayList<String>();

    public String ownerJid;

}
