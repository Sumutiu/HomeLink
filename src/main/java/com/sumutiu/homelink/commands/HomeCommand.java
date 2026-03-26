package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.HomeData;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.Map;

public class HomeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                        return 0;
                    }

                    Map<String, HomeData> playerHomes = HomeStorage.getAllHomes(player);

                    if (playerHomes == null || playerHomes.isEmpty()) {
                        HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.HOME_NONE_SET);
                        return 0;
                    }

                    var iterator = playerHomes.entrySet().iterator();

                    if (!iterator.hasNext()) {
                        HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.HOME_NONE_SET);
                        return 0;
                    }

                    Map.Entry<String, HomeData> first =
                            playerHomes.entrySet().iterator().next();

                    return teleportToHome(player, first.getKey(), first.getValue(), source.getServer());
                })
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HomeStorage::suggestHomeNames)
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();

                            if (!(source.getEntity() instanceof ServerPlayer player)) {
                                HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                return 0;
                            }

                            String name = StringArgumentType.getString(ctx, "name");
                            HomeData home = HomeStorage.getHome(player, name);

                            if (home == null) {
                                HomeLinkMessages.PrivateMessage(
                                        player,
                                        String.format(HomeLinkMessages.HOME_NOT_FOUND, name)
                                );
                                return 0;
                            }

                            return teleportToHome(player, name, home, source.getServer());
                        })
                )
        );
    }

    private static int teleportToHome(
            ServerPlayer player,
            String name,
            HomeData home,
            MinecraftServer server
    ) {
        if (server == null) {
            HomeLinkMessages.Logger(2, HomeLinkMessages.SERVER_NOT_AVAILABLE);
            return 0;
        }

        Identifier location = Identifier.tryParse(home.world);

        if (location == null) {
            HomeLinkMessages.PrivateMessage(player, "Invalid world id: " + home.world);
            return 0;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(
                Registries.DIMENSION,
                location
        );

        ServerLevel targetWorld = server.getLevel(dimensionKey);

        if (targetWorld == null) {
            HomeLinkMessages.PrivateMessage(
                    player,
                    String.format(HomeLinkMessages.HOME_WORLD_NOT_FOUND, home.world)
            );
            return 0;
        }

        TeleportScheduler.schedule(player, null, HomeLinkConfig.getHomeDelay(), () -> {
            player.teleportTo(
                    targetWorld,
                    home.position.getX() + 0.5,
                    home.position.getY(),
                    home.position.getZ() + 0.5,
                    EnumSet.noneOf(net.minecraft.world.entity.Relative.class),
                    home.yaw,
                    home.pitch,
                    false
            );

            HomeLinkMessages.PrivateMessage(
                    player,
                    String.format(HomeLinkMessages.HOME_TELEPORTED_NAMED, name)
            );
        });

        return 1;
    }
}