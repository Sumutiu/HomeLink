package com.sumutiu.homelink.neoforge;

import com.sumutiu.homelink.HomeLink;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge entrypoint (see neoforge.mods.toml): passes the NeoForge events to the common code.
 * Only NeoForge creates it, by finding @Mod, so IntelliJ thinks it is never used.
 */
@SuppressWarnings("unused")
@Mod(HomeLink.MOD_ID)
public class HomeLinkNeoForge {

    public HomeLinkNeoForge(ModContainer container) {
        HomeLink.init(container.getModInfo().getVersion().toString());

        IEventBus events = NeoForge.EVENT_BUS;

        // All events are registered once here. (Registering them on server start would add
        // them again every time a singleplayer world is opened.)

        events.addListener(ServerStartedEvent.class, event -> HomeLink.onServerStarted(event.getServer()));

        events.addListener(RegisterCommandsEvent.class, event -> HomeLink.registerCommands(event.getDispatcher()));

        events.addListener(ServerTickEvent.Post.class, event -> HomeLink.onServerTick(event.getServer()));

        // Lowest priority: runs after the other mods, so damage they already blocked (a protection mod)
        // doesn't cancel a pending teleport
        events.addListener(EventPriority.LOWEST, LivingIncomingDamageEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && !HomeLink.allowDamage(player, event.getSource())) {
                event.setCanceled(true);
            }
        });

        events.addListener(ServerStoppedEvent.class, _ -> HomeLink.onServerStopped());

        events.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                HomeLink.onPlayerJoin(player);
            }
        });

        events.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                HomeLink.onPlayerLeave(player);
            }
        });
    }
}
