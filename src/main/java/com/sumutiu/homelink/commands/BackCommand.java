package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import com.sumutiu.homelink.storage.BackStorage.BackData;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import static com.sumutiu.homelink.HomeLink.LOGGER;

import java.util.EnumSet;

public class BackCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("back")
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                        LOGGER.warn("[HomeLink]: This command can only be used by players.");
                        return 0;
                    }

                    BackData back = BackStorage.get(player);
                    if (back == null) {
                        player.sendMessage(HomeLinkMessages.prefix("No previous location to return to."), false);
                        return 0;
                    }

                    MinecraftServer server = player.getServer();
                    if (server == null) {
                        LOGGER.error("[HomeLink]: Server not available.");
                        return 0;
                    }

                    RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(back.world));
                    ServerWorld targetWorld = server.getWorld(worldKey);

                    if (targetWorld == null) {
                        player.sendMessage(HomeLinkMessages.prefix("Back location world not found: " + back.world), false);
                        return 0;
                    }

                    TeleportScheduler.schedule(player, HomeLinkConfig.getBackDelay(), () -> {
                        player.teleport(
                                targetWorld,
                                back.pos.getX() + 0.5,
                                back.pos.getY() + 0.5,
                                back.pos.getZ() + 0.5,
                                EnumSet.noneOf(PositionFlag.class),
                                back.yaw,
                                back.pitch,
                                false
                        );
                        player.sendMessage(HomeLinkMessages.prefix("Teleported back to your previous location."), false);
                    });

                    return 1;
                })
        );
    }
}
