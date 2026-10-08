package com.sumutiu.homelink;

import com.sumutiu.homelink.commands.*;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class HomeLink implements ModInitializer {

    public static final Path CONFIG_FOLDER = Path.of("config", "HomeLink");
    public static final Path CONFIG_FILE = CONFIG_FOLDER.resolve("HomeLink.json");
    public static Path STORAGE_FOLDER;

    public static volatile boolean HomeLinkInitialized = false;

    // The running server (null when stopped)
    private static volatile MinecraftServer currentServer;

    @Override
    public void onInitialize() {

        // All events are registered once here. (Registering them on server start would add
        // them again every time a singleplayer world is opened.)

        // -----------------------------
        // SERVER START (WORLD EXISTS)
        // -----------------------------
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {

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
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> {
            SetHomeCommand.register(dispatcher);
            DelHomeCommand.register(dispatcher);
            HomeCommand.register(dispatcher);
            BackCommand.register(dispatcher);
            CancelCommand.register(dispatcher);
            TeleportToCommand.register(dispatcher);
            TeleportHereCommand.register(dispatcher);
            TeleportAcceptCommand.register(dispatcher);
            TeleportDenyCommand.register(dispatcher);
        });

        // Delayed teleports and request timeouts run here, on the server thread
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (HomeLinkInitialized) {
                TeleportScheduler.tick(server);
                TeleportRequestManager.tick(server);
            }
        });

        // Teleport protection after arriving, and cancel a pending teleport on damage
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, _) -> {
            if (HomeLinkInitialized && entity instanceof ServerPlayer player) {
                return TeleportScheduler.allowDamage(currentServer, player, source);
            }
            return true;
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> {
            if (HomeLinkInitialized) {
                Logger(0, SHUTTING_DOWN);
                HomeLinkInitialized = false;
                HomeStorage.saveAllAndClear();
                TeleportScheduler.clear();
                TeleportRequestManager.clear();
                BackStorage.clear();
                currentServer = null;
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> {
            ServerPlayer player = handler.getPlayer();

            if (!HomeLinkInitialized) {
                player.connection.disconnect(
                        Component.literal(MOD_INIT_NOT_READY)
                );
                return;
            }

            HomeStorage.loadPlayerHomes(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> {
            if (!HomeLinkInitialized) return;

            ServerPlayer player = handler.getPlayer();
            TeleportScheduler.onDisconnect(player.getUUID());
            TeleportRequestManager.clearRequest(player.getUUID());
            HomeStorage.saveAndUnload(player);
        });
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
