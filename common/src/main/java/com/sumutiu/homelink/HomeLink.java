package com.sumutiu.homelink;

import com.mojang.brigadier.CommandDispatcher;
import com.sumutiu.homelink.commands.*;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sumutiu.homelink.util.HomeLinkMessages.*;

/**
 * The mod itself, shared by Fabric and NeoForge.
 * Each loader's entrypoint (HomeLinkFabric, HomeLinkNeoForge) calls init() once
 * and forwards its events to the on...() methods below.
 */
public class HomeLink {

    public static final String MOD_ID = "homelink";

    public static final Path CONFIG_FOLDER = Path.of("config", "HomeLink");
    public static final Path CONFIG_FILE = CONFIG_FOLDER.resolve("HomeLink.json");
    public static Path STORAGE_FOLDER;

    public static volatile boolean HomeLinkInitialized = false;

    // The running server (null when stopped)
    private static volatile MinecraftServer currentServer;

    private static String modVersion = "unknown";

    // Called by the loader entrypoint when the mod loads
    public static void init(String version) {
        modVersion = version;
    }

    public static String getModVersion() {
        return modVersion;
    }

    // -----------------------------
    // SERVER START (WORLD EXISTS)
    // -----------------------------
    public static void onServerStarted(MinecraftServer server) {

        long seed = server.getWorldGenSettings()
                .options()
                .seed();

        STORAGE_FOLDER = Path.of(
                "mods",
                "HomeLink_Seed_" + Long.toUnsignedString(seed)
        );

        if (initPlugin()) {
            currentServer = server;
            HomeLinkInitialized = true;
        } else {
            Logger(2, MOD_INIT_FAILED);
        }
    }

    // Called by the loader when the server registers its commands
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        SetHomeCommand.register(dispatcher);
        DelHomeCommand.register(dispatcher);
        HomeCommand.register(dispatcher);
        BackCommand.register(dispatcher);
        CancelCommand.register(dispatcher);
        TeleportToCommand.register(dispatcher);
        TeleportHereCommand.register(dispatcher);
        TeleportAcceptCommand.register(dispatcher);
        TeleportDenyCommand.register(dispatcher);
    }

    // Delayed teleports and request timeouts run here, on the server thread (end of every tick)
    public static void onServerTick(MinecraftServer server) {
        if (HomeLinkInitialized) {
            TeleportScheduler.tick(server);
            TeleportRequestManager.tick(server);
        }
    }

    // Teleport protection after arriving, and cancel a pending teleport on damage.
    // Returns false to block the damage.
    public static boolean allowDamage(ServerPlayer player, DamageSource source) {
        if (HomeLinkInitialized) {
            return TeleportScheduler.allowDamage(currentServer, player, source);
        }
        return true;
    }

    public static void onServerStopped() {
        if (HomeLinkInitialized) {
            Logger(0, SHUTTING_DOWN);
            HomeLinkInitialized = false;
            HomeStorage.saveAllAndClear();
            TeleportScheduler.clear();
            TeleportRequestManager.clear();
            BackStorage.clear();
            currentServer = null;
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!HomeLinkInitialized) {
            player.connection.disconnect(
                    Component.literal(MOD_INIT_NOT_READY)
            );
            return;
        }

        HomeStorage.loadPlayerHomes(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        if (!HomeLinkInitialized) return;

        TeleportScheduler.onDisconnect(player.getUUID());
        TeleportRequestManager.clearRequest(player.getUUID());
        HomeStorage.saveAndUnload(player);
    }

    private static boolean initPlugin() {
        logAsciiBanner(MOD_ASCII_BANNER, Mod_ID + ": V" + getModVersion() + " - Teleport with style!");

        try {
            if (Files.notExists(CONFIG_FOLDER)) {
                Files.createDirectories(CONFIG_FOLDER);
                Logger(0, MAIN_FOLDER_CREATED);
            }
        } catch (IOException e) {
            Logger(2, MAIN_FOLDER_CREATION_FAILED);
            return false;
        }

        try {
            if (Files.notExists(STORAGE_FOLDER)) {
                Files.createDirectories(STORAGE_FOLDER);
                Logger(0, STORAGE_FOLDER_CREATED);
            }
        } catch (IOException e) {
            Logger(2, STORAGE_FOLDER_CREATION_FAILED);
            return false;
        }

        if (Files.notExists(CONFIG_FILE)) {
            if (!HomeLinkConfig.save()) {
                return false;
            }
            Logger(0, DEFAULT_CONFIG_LOADED);
        }
        HomeLinkConfig.load();
        return true;
    }
}
