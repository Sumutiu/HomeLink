package com.sumutiu.homelink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sumutiu.homelink.util.HomeLinkMessages;

import java.io.*;

public class HomeLinkConfig {

    private static final File CONFIG_FOLDER = new File("config/HomeLink");
    private static final File CONFIG_FILE = new File(CONFIG_FOLDER, "HomeLink.json");

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

    public static void load() {
        try {
            if (!CONFIG_FOLDER.exists()) {
                if (CONFIG_FOLDER.mkdirs()) {
                    if (!CONFIG_FILE.exists()) {
                        save(); // write default config
                        return;
                    }
                } else {
                    HomeLinkMessages.Logger(2, HomeLinkMessages.CONFIG_FOLDER_CREATION_FAILED);
                }
            }

            try (Reader reader = new FileReader(CONFIG_FILE)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null){
                    config = loaded;
                    HomeLinkMessages.logAsciiBanner(HomeLinkMessages.MOD_ASCII_BANNER, "[HomeLink]: V" + HomeLinkMessages.getModVersion() + " - Teleport with style!");
                }
            }

        } catch (IOException e) {
            HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.CONFIG_LOAD_FAILED, e.getMessage()));
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            HomeLinkMessages.Logger(0, HomeLinkMessages.DEFAULT_CONFIG_LOADED);
        } catch (IOException e) {
            HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.CONFIG_SAVE_FAILED, e.getMessage()));
        }
    }

    public static int getMaxHomes() { return config.HomeLink_MaxHomes; }
    public static boolean getCancelOnMove() { return config.HomeLink_Cancel_OnMove; }
    public static int getHomeDelay() { return config.HomeLink_Home_Delay; }
    public static int getBackDelay() { return config.HomeLink_Back_Delay; }
    public static int getTeleportAcceptDelay() { return config.HomeLink_Teleport_Accept_Delay; }
    public static int getTeleportDelay() { return config.HomeLink_Teleport_Delay; }
}
