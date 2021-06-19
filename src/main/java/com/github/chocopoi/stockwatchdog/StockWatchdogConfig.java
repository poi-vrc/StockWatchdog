package com.github.chocopoi.stockwatchdog;

import com.google.gson.Gson;

import java.io.*;
import java.security.SecureRandom;
import java.util.UUID;

public class StockWatchdogConfig implements Serializable {

    public String httpProxy = "";

    public String rtsHostName = "localhost";

    public int rtsPort = 7943;

    public boolean useDistributedRequest = false;

    public String drsHostName = "localhost";

    public int drsPort = 6382;

    public String drsAuthToken = randomString(128);

    public String drsEncKey = null;

    public String drsEncSalt = null;

    public long drsNodeCoolDown = 120000; //2-min

    public long crawlFrequency = 180000; //3-min

    private String pathToConfigJson = null;

    public static StockWatchdogConfig load(String pathToConfigJson) throws IOException {
        File file = new File(pathToConfigJson);

        if (!file.exists()) {
            StockWatchdogConfig config = new StockWatchdogConfig();
            config.pathToConfigJson = pathToConfigJson;
            config.save();
            return config;
        }

        Gson gson = new Gson();
        FileInputStream fis = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        StockWatchdogConfig config = gson.fromJson(reader, StockWatchdogConfig.class);
        reader.close();
        config.pathToConfigJson = pathToConfigJson;
        return config;
    }

    public void save() throws IOException {
        File file = new File(pathToConfigJson);

        if (!file.exists()) {
            file.createNewFile();
        }

        Gson gson = new Gson();
        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(fos);
        gson.toJson(this, writer);
        writer.close();
    }

    //https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static SecureRandom rnd = new SecureRandom();

    private String randomString(int len){
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

}
