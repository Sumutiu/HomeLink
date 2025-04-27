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

public class HomeLink implements ModInitializer {
	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			HomeLinkMessages.Logger(0, HomeLinkMessages.SHUTTING_DOWN_SCHEDULERS);
			TeleportScheduler.shutdown();
			TeleportRequestManager.shutdown();
		});
		HomeLinkConfig.load();
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
	}
}