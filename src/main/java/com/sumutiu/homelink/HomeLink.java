package com.sumutiu.homelink;

import com.sumutiu.homelink.commands.*;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class HomeLink implements ModInitializer {

	public static final Path CONFIG_FOLDER = Path.of("config", "HomeLink");
	public static final Path CONFIG_FILE = CONFIG_FOLDER.resolve("HomeLink.json");
	public static final File STORAGE_FOLDER = new File("mods/HomeLink");

	@Override
	public void onInitialize() {
		if (initPlugin()) {
			ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
				HomeLinkMessages.Logger(0, HomeLinkMessages.SHUTTING_DOWN_SCHEDULERS);
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

		if (!STORAGE_FOLDER.exists()) {
			if (STORAGE_FOLDER.mkdirs()) {
				HomeLinkMessages.Logger(0, HomeLinkMessages.MAIN_FOLDER_CREATED);
			} else {
				HomeLinkMessages.Logger(2, HomeLinkMessages.MAIN_FOLDER_CREATION_FAILED);
				return false;
			}
		}

		if (Files.notExists(CONFIG_FILE)) {
            return HomeLinkConfig.save();
		}
        return true;
    }
}