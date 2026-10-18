package com.sumutiu.homelink.fabric;

import com.sumutiu.homelink.HomeLink;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric entrypoint (see fabric.mod.json): passes the Fabric API events to the common code.
 * Only Fabric creates it, from the name in fabric.mod.json, so IntelliJ thinks it is never used.
 */
@SuppressWarnings("unused")
public class HomeLinkFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        HomeLink.init(FabricLoader.getInstance()
                .getModContainer(HomeLink.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown"));

        // All events are registered once here. (Registering them on server start would add
        // them again every time a singleplayer world is opened.)

        ServerLifecycleEvents.SERVER_STARTED.register(HomeLink::onServerStarted);

        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> HomeLink.registerCommands(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(HomeLink::onServerTick);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, _) -> {
            if (entity instanceof ServerPlayer player) {
                return HomeLink.allowDamage(player, source);
            }
            return true;
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> HomeLink.onServerStopped());

        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> HomeLink.onPlayerJoin(handler.getPlayer()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> HomeLink.onPlayerLeave(handler.getPlayer()));
    }
}
