package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.HomeData;
import com.sumutiu.homelink.storage.HomeStorage;
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

import static com.sumutiu.homelink.HomeLink.HomeLinkInitialized;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class HomeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
            .executes(ctx -> {

                CommandSourceStack source = ctx.getSource();
                if (!(source.getEntity() instanceof ServerPlayer player)) {
                    Logger(1, PLAYER_ONLY_COMMAND);
                    return 0;
                }

                if (HomeLinkInitialized) {
                    Map<String, HomeData> playerHomes = HomeStorage.getAllHomes(player);

                    if (playerHomes == null || playerHomes.isEmpty()) {
                        PrivateMessage(player, HOME_NONE_SET);
                        return 0;
                    }

                    var iterator = playerHomes.entrySet().iterator();

                    if (!iterator.hasNext()) {
                        PrivateMessage(player, HOME_NONE_SET);
                        return 0;
                    }

                    Map.Entry<String, HomeData> first =
                            playerHomes.entrySet().iterator().next();

                    return teleportToHome(player, first.getKey(), first.getValue(), source.getServer());
                } else {
                    PrivateMessage(player, MOD_INIT_NOT_READY);
                    return 0;
                }
            })
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(HomeStorage::suggestHomeNames)
                .executes(ctx -> {

                    CommandSourceStack source = ctx.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        Logger(1, PLAYER_ONLY_COMMAND);
                        return 0;
                    }

                    if (HomeLinkInitialized) {
                        String name = StringArgumentType.getString(ctx, "name");
                        HomeData home = HomeStorage.getHome(player, name);

                        if (home == null) {
                            PrivateMessage(player, String.format(HOME_NOT_FOUND, name));
                            return 0;
                        }

                        return teleportToHome(player, name, home, source.getServer());
                    } else {
                        PrivateMessage(player, MOD_INIT_NOT_READY);
                        return 0;
                    }

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
            Logger(2, SERVER_NOT_AVAILABLE);
            return 0;
        }

        Identifier location = Identifier.tryParse(home.world);

        if (location == null) {
            PrivateMessage(player, "Invalid world id: " + home.world);
            return 0;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, location);

        ServerLevel targetWorld = server.getLevel(dimensionKey);

        if (targetWorld == null) {
            PrivateMessage(player, String.format(HOME_WORLD_NOT_FOUND, home.world));
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

            PrivateMessage(player, String.format(HOME_TELEPORTED_NAMED, name));
        });

        return 1;
    }
}