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

import java.util.EnumSet;

public class BackCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpback")
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                        return 0;
                    }

                    BackData back = BackStorage.get(player);
                    if (back == null) {
                        HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.NO_BACK_LOCATION);
                        return 0;
                    }

                    MinecraftServer server = player.getServer();
                    if (server == null) {
                        HomeLinkMessages.Logger(2, HomeLinkMessages.SERVER_NOT_AVAILABLE);
                        return 0;
                    }

                    RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(back.world));
                    ServerWorld targetWorld = server.getWorld(worldKey);

                    if (targetWorld == null) {
                        HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.BACK_WORLD_NOT_FOUND, back.world));
                        return 0;
                    }

                    TeleportScheduler.schedule(player, null, HomeLinkConfig.getBackDelay(), () -> {
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
                        HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.BACK_TELEPORTED);
                    });

                    return 1;
                })
        );
    }
}
