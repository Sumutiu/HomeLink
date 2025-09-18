package com.sumutiu.homelink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sumutiu.homelink.util.HomeLinkMessages;

import java.io.*;

import static com.sumutiu.homelink.HomeLink.*;

public class HomeLinkConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public int HomeLink_MaxHomes = 5;
        public int HomeLink_Home_Delay = 5;
        public int HomeLink_Back_Delay = 5;
        public boolean HomeLink_Cancel_OnMove = false;
        public int HomeLink_Teleport_Delay = 5;
        public int HomeLink_Teleport_Accept_Delay = 15;
    }

    private static ConfigData config = new ConfigData();

    public static boolean save() {
        try (Writer writer = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(config, writer);
            HomeLinkMessages.Logger(0, HomeLinkMessages.DEFAULT_CONFIG_LOADED);
            try (Reader reader = new FileReader(CONFIG_FILE.toFile())) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null){
                    config = loaded;
                }
            } catch (IOException e) {
                HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.CONFIG_LOAD_FAILED, e.getMessage()));
                return false;
            }
            return true;
        } catch (IOException e) {
            HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.CONFIG_SAVE_FAILED, e.getMessage()));
            return false;
        }
    }

    public static int getMaxHomes() { return config.HomeLink_MaxHomes; }
    public static boolean getCancelOnMove() { return config.HomeLink_Cancel_OnMove; }
    public static int getHomeDelay() { return config.HomeLink_Home_Delay; }
    public static int getBackDelay() { return config.HomeLink_Back_Delay; }
    public static int getTeleportAcceptDelay() { return config.HomeLink_Teleport_Accept_Delay; }
    public static int getTeleportDelay() { return config.HomeLink_Teleport_Delay; }
}
