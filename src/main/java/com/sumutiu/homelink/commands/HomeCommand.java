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

public class HomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("home")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(HomeStorage::suggestHomeNames)
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                System.out.println("[HomeLink]: This command can only be used by players.");
                                return 0;
                            }

                            String name = StringArgumentType.getString(ctx, "name");
                            HomeData home = HomeStorage.getHome(player, name);

                            if (home != null) {
                                MinecraftServer server = player.getServer();
                                if (server == null) {
                                    System.out.println("[HomeLink]: Server not available.");
                                    return 0;
                                }

                                Identifier worldId = Identifier.tryParse(home.world);
                                RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
                                ServerWorld targetWorld = server.getWorld(worldKey);

                                if (targetWorld == null) {
                                    player.sendMessage(HomeLinkMessages.prefix("World not found: " + home.world), false);
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
                                    player.sendMessage(HomeLinkMessages.prefix("Teleported to home '" + name + "'."), false);
                                });
                                return 1;

                            } else {
                                player.sendMessage(HomeLinkMessages.prefix("Home '" + name + "' not found."), false);
                                return 0;
                            }
                        })
                )
        );
    }
}
