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
	public static final Path STORAGE_FOLDER = Path.of("mods", "HomeLink");

	@Override
	public void onInitialize() {
		if (initPlugin()) {
			ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
				Logger(0, SHUTTING_DOWN_SCHEDULERS);
				TeleportScheduler.shutdown();
				TeleportRequestManager.shutdown();
			});
			TeleportScheduler.initialize();
			HomeStorage.initialize();

			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
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
		} else {
			Logger(2, MOD_INIT_FAILED);
		}
	}

	private static boolean initPlugin() {
		logAsciiBanner(MOD_ASCII_BANNER, "[HomeLink]: V" + getModVersion() + " - Teleport with style!");

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
				Logger(0, MAIN_FOLDER_CREATED);
			}
		} catch (IOException e) {
			Logger(2, MAIN_FOLDER_CREATION_FAILED);
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