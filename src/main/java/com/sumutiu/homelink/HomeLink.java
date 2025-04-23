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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class HomeLink implements ModInitializer {
	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			HomeLinkMessages.Logger(0, HomeLinkMessages.SHUTTING_DOWN_SCHEDULERS);
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
			AboutCommand.register(dispatcher);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (handler != null && handler.getPlayer() != null) {
				HomeStorage.loadPlayerHomes(handler.getPlayer());
			} else {
				HomeLinkMessages.Logger(2, HomeLinkMessages.INVALID_CONNECTION_HANDLER);
			}
		});
	}
}