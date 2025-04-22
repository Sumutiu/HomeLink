package com.sumutiu.homelink.commands;

import com.sumutiu.homelink.config.HomeLinkConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.storage.HomeData;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.Map;

public class HomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("home")
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                        return 0;
                    }

                    Map<String, HomeData> playerHomes = HomeStorage.getAllHomes(player);
                    if (playerHomes == null || playerHomes.isEmpty()) {
                        HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.HOME_NONE_SET);
                        return 0;
                    }

                    Map.Entry<String, HomeData> first = playerHomes.entrySet().iterator().next();
                    return teleportToHome(player, first.getKey(), first.getValue());
                })
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(HomeStorage::suggestHomeNames)
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                return 0;
                            }

                            String name = StringArgumentType.getString(ctx, "name");
                            HomeData home = HomeStorage.getHome(player, name);

                            if (home == null) {
                                HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.HOME_NOT_FOUND, name));
                                return 0;
                            }

                            return teleportToHome(player, name, home);
                        })
                )
        );
    }

    private static int teleportToHome(ServerPlayerEntity player, String name, HomeData home) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            HomeLinkMessages.Logger(2, HomeLinkMessages.SERVER_NOT_AVAILABLE);
            return 0;
        }

        Identifier worldId = Identifier.tryParse(home.world);
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
        ServerWorld targetWorld = server.getWorld(worldKey);

        if (targetWorld == null) {
            HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.HOME_WORLD_NOT_FOUND, home.world));
            return 0;
        }

        TeleportScheduler.schedule(player, HomeLinkConfig.getHomeDelay(), () -> {
            player.teleport(
                    targetWorld,
                    home.position.getX() + 0.5,
                    home.position.getY(),
                    home.position.getZ() + 0.5,
                    EnumSet.noneOf(PositionFlag.class),
                    home.yaw,
                    home.pitch,
                    false
            );
            HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.HOME_TELEPORTED_NAMED, name));
        });

        return 1;
    }
}
