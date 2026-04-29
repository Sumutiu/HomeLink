package com.sumutiu.homelink;

import com.sumutiu.homelink.commands.*;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class HomeLink implements ModInitializer {

	public static final Path CONFIG_FOLDER = Path.of("config", "HomeLink");
	public static final Path CONFIG_FILE = CONFIG_FOLDER.resolve("HomeLink.json");
	public static Path STORAGE_FOLDER;

	public static volatile boolean HomeLinkInitialized = false;

	@Override
	public void onInitialize() {

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
				// safe to init now because world exists
				TeleportScheduler.initialize();
				HomeStorage.initialize();

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

		ServerLifecycleEvents.SERVER_STOPPED.register(_ -> {
			if (HomeLinkInitialized) {
				Logger(0, SHUTTING_DOWN_SCHEDULERS);
				TeleportScheduler.shutdown();
				TeleportRequestManager.shutdown();
				HomeLinkInitialized = false;
			}
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
		}
		return HomeLinkConfig.load();
	}
}