package com.sumutiu.homelink;

import com.sumutiu.homelink.commands.*;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeLink implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("HomeLink");

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LOGGER.info("[HomeLink]: Shutting down HomeLink Schedulers...");
			TeleportScheduler.shutdown();
			TeleportRequestManager.shutdown();
		});

		HomeLinkConfig.load();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			SetHomeCommand.register(dispatcher);
			DelHomeCommand.register(dispatcher);
			HomeCommand.register(dispatcher);
			BackCommand.register(dispatcher);
			TeleportToCommand.register(dispatcher);
			TeleportHereCommand.register(dispatcher);
			TeleportAcceptCommand.register(dispatcher);
			TeleportDenyCommand.register(dispatcher);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> HomeStorage.loadPlayerHomes(handler.getPlayer()));
	}
}