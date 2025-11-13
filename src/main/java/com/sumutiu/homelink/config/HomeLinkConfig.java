package com.sumutiu.homelink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

import static com.sumutiu.homelink.HomeLink.CONFIG_FILE;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class HomeLinkConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public int HomeLink_MaxHomes = 5;
        public int HomeLink_Home_Delay = 5;
        public int HomeLink_Back_Delay = 5;
        public boolean HomeLink_Cancel_OnMove = false;
        public int HomeLink_Teleport_Delay = 5;
        public int HomeLink_Teleport_Accept_Delay = 15;
        public int HomeLink_Invulnerability_Time = 3;
    }

    private static ConfigData config = new ConfigData();

    public static boolean save() {
        try (Writer writer = new FileWriter(CONFIG_FILE.toFile())) {
            GSON.toJson(new ConfigData(), writer);
            // This message is not ideal, but we are not allowed to add new messages.
            // It is logged when a new default configuration is created.
            Logger(0, DEFAULT_CONFIG_LOADED);
            return true;
        } catch (IOException e) {
            Logger(2, String.format(CONFIG_SAVE_FAILED, e.getMessage()));
            return false;
        }
    }

    public static boolean load() {
        try (Reader reader = new FileReader(CONFIG_FILE.toFile())) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                config = loaded;
                Logger(0, CONFIG_LOADED);
            } else {
                // The config file is present but malformed. Log a warning and load default settings.
                // The mod will not be prevented from starting, to avoid start-up failures for a misconfigured mod.
                Logger(1, CONFIG_LOAD_FAILED_MALFORMED);
                config = new ConfigData();
            }
            return true;
        } catch (IOException e) {
            Logger(2, String.format(CONFIG_LOAD_FAILED, e.getMessage()));
            return false;
        }
    }

    public static int getMaxHomes() { return config.HomeLink_MaxHomes; }
    public static boolean getCancelOnMove() { return config.HomeLink_Cancel_OnMove; }
    public static int getHomeDelay() { return config.HomeLink_Home_Delay; }
    public static int getBackDelay() { return config.HomeLink_Back_Delay; }
    public static int getTeleportAcceptDelay() { return config.HomeLink_Teleport_Accept_Delay; }
    public static int getTeleportDelay() { return config.HomeLink_Teleport_Delay; }
    public static int getInvulnerabilityTime() { return config.HomeLink_Invulnerability_Time; }
}
