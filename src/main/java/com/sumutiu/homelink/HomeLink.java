package com.sumutiu.homelink;

import com.sumutiu.homelink.commands.*;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.HomeStorage;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class HomeLink implements ModInitializer {
	@Override
	public void onInitialize() {
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