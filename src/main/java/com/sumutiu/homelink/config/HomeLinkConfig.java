package com.sumutiu.homelink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

import static com.sumutiu.homelink.HomeLink.CONFIG_FILE;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class HomeLinkConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public Integer HomeLink_MaxHomes = 5;
        public Integer HomeLink_Home_Delay = 5;
        public Integer HomeLink_Back_Delay = 5;
        public Boolean HomeLink_Cancel_OnMove = false;
        public Integer HomeLink_Teleport_Delay = 5;
        public Integer HomeLink_Teleport_Accept_Delay = 15;
        public Integer HomeLink_Invulnerability_Time = 3;
    }

    private static final ConfigData config_Default = new ConfigData();
    private static ConfigData config = new ConfigData();

    public static boolean save() {
        try (Writer writer = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(config, writer);
            Logger(0, DEFAULT_CONFIG_LOADED);
            return true;
        } catch (IOException e) {
            Logger(2, String.format(CONFIG_SAVE_FAILED, e.getMessage()));
            return false;
        }
    }

    public static boolean load() {
        boolean updated = false;
        try (Reader reader = new FileReader(CONFIG_FILE.toFile())) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                config = loaded;

                // Check for missing fields (null means they weren't present)
                if (config.HomeLink_MaxHomes == null) { config.HomeLink_MaxHomes = config_Default.HomeLink_MaxHomes; updated = true; }
                if (config.HomeLink_Home_Delay == null) { config.HomeLink_Home_Delay = config_Default.HomeLink_Home_Delay; updated = true; }
                if (config.HomeLink_Back_Delay == null) { config.HomeLink_Back_Delay = config_Default.HomeLink_Back_Delay; updated = true; }
                if (config.HomeLink_Cancel_OnMove == null) { config.HomeLink_Cancel_OnMove = config_Default.HomeLink_Cancel_OnMove; updated = true; }
                if (config.HomeLink_Teleport_Delay == null) { config.HomeLink_Teleport_Delay = config_Default.HomeLink_Teleport_Delay; updated = true; }
                if (config.HomeLink_Teleport_Accept_Delay == null) { config.HomeLink_Teleport_Accept_Delay = config_Default.HomeLink_Teleport_Accept_Delay; updated = true; }
                if (config.HomeLink_Invulnerability_Time == null) { config.HomeLink_Invulnerability_Time = config_Default.HomeLink_Invulnerability_Time; updated = true; }

                Logger(0, CONFIG_LOADED);
            } else {
                Logger(1, CONFIG_LOAD_FAILED_MALFORMED);
                config = new ConfigData();
                updated = true;
            }
        } catch (IOException e) {
            Logger(2, String.format(CONFIG_LOAD_FAILED, e.getMessage()));
            return false;
        }

        // Save updated file if defaults were added
        if (updated) save();

        return true;
    }

    public static int getMaxHomes() { return config.HomeLink_MaxHomes; }
    public static boolean getCancelOnMove() { return config.HomeLink_Cancel_OnMove; }
    public static int getHomeDelay() { return config.HomeLink_Home_Delay; }
    public static int getBackDelay() { return config.HomeLink_Back_Delay; }
    public static int getTeleportAcceptDelay() { return config.HomeLink_Teleport_Accept_Delay; }
    public static int getTeleportDelay() { return config.HomeLink_Teleport_Delay; }
    public static int getInvulnerabilityTime() { return config.HomeLink_Invulnerability_Time; }
}
