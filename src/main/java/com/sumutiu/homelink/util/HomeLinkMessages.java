package com.sumutiu.homelink.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeLinkMessages {

    // Core / General
    public static final String MOD_ASCII_BANNER = """
         _   _                      _     _       _   \s
        | | | |                    | |   (_)     | |  \s
        | |_| | ___  _ __ ___   ___| |    _ _ __ | | __
        |  _  |/ _ \\| '_ ` _ \\ / _ \\ |   | | '_ \\| |/ /
        | | | | (_) | | | | | |  __/ |___| | | | |   <\s
        \\_| |_/\\___/|_| |_| |_|\\___\\_____/|_| |_|_|\\_\\
        """;
    public static final String Mod_ID = "[HomeLink]";
    public static final String SCHEDULER_SERVICE_NAME = "[TeleportScheduler]";
    public static final String MANAGER_SERVICE_NAME = "[TeleportRequestManager]";
    public static final String SERVER_NOT_AVAILABLE = "Server not available.";
    public static final String INVALID_CONNECTION_HANDLER = "Invalid connection handler or player during join event.";
    public static final String PLAYER_ONLY_COMMAND = "This command can only be used by players.";

    // Configuration
    public static final String CONFIG_FOLDER_CREATION_FAILED = "Config folder cannot be created in the HomeLink folder.";
    public static final String CONFIG_LOAD_FAILED = "Failed to load HomeLink config: %s";
    public static final String CONFIG_SAVE_FAILED = "Failed to save HomeLink config: %s";
    public static final String DEFAULT_CONFIG_LOADED = "Default Config loaded. Please edit the default values in the HomeLink/Config folder.";

    // Folder / File Operations
    public static final String MAIN_FOLDER_CREATED = "Main mod folder has been created.";
    public static final String MAIN_FOLDER_CREATION_FAILED = "Failed to create the main mod folder.";
    public static final String HOME_LOAD_FAILED = "Failed to load homes for %s. Error: %s";
    public static final String HOME_SAVE_FAILED = "Failed to save homes for %s. Error: %s";

    // Teleportation - General
    public static final String TELEPORT_IN_PROGRESS = "You already have a teleport in progress.";
    public static final String TELEPORT_DELAY_MESSAGE = "Teleporting in %d seconds...";
    public static final String TELEPORT_CANCELLED_MOVEMENT = "Teleport cancelled due to movement.";

    // Teleportation - Scheduling
    public static final String TELEPORT_SCHEDULER_SHUTDOWN = "TeleportScheduler has been shut down.";
    public static final String TELEPORT_SCHEDULER_SHUTDOWN_FAILED = "Failed to shut down TeleportScheduler: %s";
    public static final String SHUTTING_DOWN_SCHEDULERS = "Shutting down HomeLink Schedulers...";
    public static final String TELEPORT_REQUEST_MANAGER_SHUTDOWN = "TeleportRequestManager scheduler has been shut down.";
    public static final String TELEPORT_REQUEST_MANAGER_SHUTDOWN_FAILED = "Failed to shut down TeleportRequestManager scheduler: %s";

    // Teleportation - Errors & Limits
    public static final String TELEPORT_SELF_DENIED = "You cannot teleport yourself to yourself.";
    public static final String PLAYER_NOT_FOUND = "Player has not been found: %s";
    public static final String PLAYER_HAS_PENDING_REQUEST = "That player already has a pending request.";
    public static final String NO_PENDING_REQUEST = "No pending request from: %s";

    // Teleportation - Requests (Send)
    public static final String TELEPORT_REQUEST_SENT_TO = "Teleport request sent to: %s";
    public static final String TELEPORT_REQUEST_PROMPT_HERE = "%s wants to teleport you to them. Type /teleportaccept %s to accept.";
    public static final String TELEPORT_REQUEST_PROMPT_TO = "%s wants to teleport to you. Type /teleportaccept %s within time to accept.";

    // Teleportation - Accepted
    public static final String TELEPORT_REQUEST_ACCEPTED = "You accepted the teleport request from: %s";
    public static final String TELEPORT_REQUEST_ACCEPTED_BY = "%s accepted your teleport request.";
    public static final String TELEPORTING_TO_IN_SECONDS = "Teleporting to %s in %d seconds...";
    public static final String TELEPORTING_YOU_TO_IN_SECONDS = "Teleporting you to %s in %d seconds...";
    public static final String TELEPORTED_TO_YOU = "%s teleported to you.";
    public static final String YOU_TELEPORTED_TO_PLAYER = "You teleported to: %s";
    public static final String PLAYER_WAS_TELEPORTED_TO_YOU = "%s was teleported to you.";
    public static final String LOG_TELEPORTED = "%s teleported to %s.";
    public static final String LOG_PLAYER_WAS_TELEPORTED = "%s was teleported to %s.";

    // Teleportation - Denied / Timed Out
    public static final String TELEPORT_REQUEST_DENIED_FROM = "Denied teleport request from: %s";
    public static final String TELEPORT_REQUEST_DENIED_TO = "Your teleport request to %s was denied.";
    public static final String TELEPORT_REQUEST_TO_TIMEOUT = "Teleport request to %s timed out.";
    public static final String TELEPORT_REQUEST_FROM_TIMEOUT = "Teleport request from %s timed out.";
    public static final String LOG_TELEPORT_TIMEOUT = "Teleport request from %s to %s timed out.";
    public static final String LOG_TELEPORT_REQUEST_DENIED = "Teleport request from %s to %s has been denied.";

    // Back Command
    public static final String NO_BACK_LOCATION = "No previous location to return to.";
    public static final String BACK_WORLD_NOT_FOUND = "Back location world not found: %s";
    public static final String BACK_TELEPORTED = "Teleported back to your previous location.";

    // Homes
    public static final String HOME_NONE_SET = "You have no homes set.";
    public static final String HOME_DELETED = "Home has been deleted: %s";
    public static final String HOME_NOT_FOUND = "Home has not been found: %s";
    public static final String HOME_WORLD_NOT_FOUND = "Home world has not been found: %s";
    public static final String HOME_TELEPORTED_NAMED = "Teleported to home: %s";
    public static final String HOME_SET_NAMED = "Home set at your current location: %s";
    public static final String HOME_LIMIT_REACHED = "You have reached the maximum number of homes (%d). Delete one to set another.";


    private static final Logger LOGGER = LoggerFactory.getLogger(Mod_ID);

    public static void PrivateMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(Mod_ID + ": ")
                .styled(style -> style.withColor(Formatting.GREEN))
                .append(Text.literal(message).styled(s -> s.withColor(Formatting.WHITE))), false);
    }

    public static void Logger(int type, String message) {
        switch (type) {
            case 0 -> LOGGER.info(message);
            case 1 -> LOGGER.warn(message);
            case 2 -> LOGGER.error(message);
            default -> LOGGER.info(message); // Fallback
        }
    }

    public static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("homelink")
                .map(ModContainer::getMetadata)
                .map(meta -> meta.getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static void logAsciiBanner(String banner, String footer) {
        LOGGER.info(""); // Empty line before
        for (String line : banner.stripTrailing().split("\n")) {
            LOGGER.info(line);
        }
        LOGGER.info(""); // Empty line before
        LOGGER.info(footer);
        LOGGER.info(""); // Empty line after
    }

    public static void sendModInfoToPlayer(ServerPlayerEntity player) {
        player.sendMessage(Text.literal(""));
        player.sendMessage(Text.literal("======= [ HomeLink ] =======").styled(s -> s.withColor(Formatting.AQUA)), false);
        player.sendMessage(Text.literal("V" + getModVersion() + " - Teleport with style!").styled(s -> s.withColor(Formatting.GRAY)), false);
        player.sendMessage(Text.literal(""));
    }
}