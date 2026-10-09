package com.sumutiu.homelink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
        // Serialize to a String first: Gson wraps write errors in an unchecked JsonIOException
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(config), StandardCharsets.UTF_8);
            Logger(0, String.format(CONFIG_SAVED, CONFIG_FILE));
            return true;
        } catch (IOException e) {
            Logger(2, String.format(CONFIG_SAVE_FAILED, e.getMessage()));
            return false;
        }
    }

    // Never fails: if the file can't be read, the defaults are used for this run
    public static void load() {
        boolean updated = false;
        try {
            String text = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
            ConfigData loaded = GSON.fromJson(text, ConfigData.class);
            if (loaded != null) {
                config = loaded;

                // Check for fields set to null
                if (config.HomeLink_MaxHomes == null) {
                    config.HomeLink_MaxHomes = config_Default.HomeLink_MaxHomes;
                    updated = true;
                }
                if (config.HomeLink_Home_Delay == null) {
                    config.HomeLink_Home_Delay = config_Default.HomeLink_Home_Delay;
                    updated = true;
                }
                if (config.HomeLink_Back_Delay == null) {
                    config.HomeLink_Back_Delay = config_Default.HomeLink_Back_Delay;
                    updated = true;
                }
                if (config.HomeLink_Cancel_OnMove == null) {
                    config.HomeLink_Cancel_OnMove = config_Default.HomeLink_Cancel_OnMove;
                    updated = true;
                }
                if (config.HomeLink_Teleport_Delay == null) {
                    config.HomeLink_Teleport_Delay = config_Default.HomeLink_Teleport_Delay;
                    updated = true;
                }
                if (config.HomeLink_Teleport_Accept_Delay == null) {
                    config.HomeLink_Teleport_Accept_Delay = config_Default.HomeLink_Teleport_Accept_Delay;
                    updated = true;
                }
                if (config.HomeLink_Invulnerability_Time == null) {
                    config.HomeLink_Invulnerability_Time = config_Default.HomeLink_Invulnerability_Time;
                    updated = true;
                }

                // Check for fields missing from the file (Gson keeps the default value for those,
                // so they are never null): write them to the file
                JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                for (String key : GSON.toJsonTree(config).getAsJsonObject().keySet()) {
                    if (!json.has(key)) {
                        updated = true;
                    }
                }

                Logger(0, CONFIG_LOADED);
            } else {
                Logger(1, CONFIG_LOAD_FAILED_MALFORMED);
                config = new ConfigData();
                updated = true;
            }
        } catch (IOException | JsonParseException e) {
            // Use the defaults for this run, but never overwrite a file the admin may be editing
            Logger(2, String.format(CONFIG_LOAD_FAILED, e.getMessage()));
            config = new ConfigData();
            return;
        }

        // Save updated file if defaults were added
        if (updated) save();

        validate();
    }

    // Out-of-range values are replaced in memory only (with a warning), the file is left as it is
    private static void validate() {
        config.HomeLink_MaxHomes = atLeast("HomeLink_MaxHomes", config.HomeLink_MaxHomes, 0);
        config.HomeLink_Home_Delay = atLeast("HomeLink_Home_Delay", config.HomeLink_Home_Delay, 0);
        config.HomeLink_Back_Delay = atLeast("HomeLink_Back_Delay", config.HomeLink_Back_Delay, 0);
        config.HomeLink_Teleport_Delay = atLeast("HomeLink_Teleport_Delay", config.HomeLink_Teleport_Delay, 0);
        config.HomeLink_Teleport_Accept_Delay = atLeast("HomeLink_Teleport_Accept_Delay", config.HomeLink_Teleport_Accept_Delay, 1);
        config.HomeLink_Invulnerability_Time = atLeast("HomeLink_Invulnerability_Time", config.HomeLink_Invulnerability_Time, 0);
    }

    private static int atLeast(String key, int value, int minimum) {
        if (value >= minimum) return value;
        Logger(1, String.format(CONFIG_INVALID_VALUE, key, value, minimum));
        return minimum;
    }

    public static int getMaxHomes() {
        return config.HomeLink_MaxHomes;
    }

    public static boolean getCancelOnMove() {
        return config.HomeLink_Cancel_OnMove;
    }

    public static int getHomeDelay() {
        return config.HomeLink_Home_Delay;
    }

    public static int getBackDelay() {
        return config.HomeLink_Back_Delay;
    }

    public static int getTeleportAcceptDelay() {
        return config.HomeLink_Teleport_Accept_Delay;
    }

    public static int getTeleportDelay() {
        return config.HomeLink_Teleport_Delay;
    }

    public static int getInvulnerabilityTime() {
        return config.HomeLink_Invulnerability_Time;
    }
}
