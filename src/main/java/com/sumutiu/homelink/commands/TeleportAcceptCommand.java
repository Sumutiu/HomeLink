package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.TeleportRequest;
import com.sumutiu.homelink.teleport.TeleportRequestManager.RequestType;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import static com.sumutiu.homelink.HomeLink.LOGGER;

import java.util.EnumSet;

public class TeleportAcceptCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("teleportaccept")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            if (!(context.getSource().getEntity() instanceof ServerPlayerEntity target)) {
                                return builder.buildFuture();
                            }

                            return TeleportRequestManager.suggestPendingRequestNames(target, builder);
                        })
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity target)) {
                                LOGGER.warn("[HomeLink]: This command can only be used by players.");
                                return 0;
                            }

                            String requesterName = StringArgumentType.getString(ctx, "name");

                            MinecraftServer server = target.getServer();
                            if (server == null) {
                                LOGGER.error("[HomeLink]: Server not available.");
                                return 0;
                            }

                            ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterName);

                            if (requester == null) {
                                target.sendMessage(HomeLinkMessages.prefix("Player '" + requesterName + "' not found."), false);
                                return 0;
                            }

                            TeleportRequest request = TeleportRequestManager.getRequest(target);
                            if (request == null || !request.requesterId().equals(requester.getUuid())) {
                                target.sendMessage(HomeLinkMessages.prefix("No pending request from '" + requesterName + "'."), false);
                                return 0;
                            }

                            TeleportRequestManager.clearRequest(target);
                            int delay = HomeLinkConfig.getTeleportDelay();

                            if (request.type() == RequestType.TO) {
                                target.sendMessage(HomeLinkMessages.prefix("You accepted " + requester.getName().getString() + "'s teleport request."), false);
                                requester.sendMessage(HomeLinkMessages.prefix("Teleporting to " + target.getName().getString() + " in " + delay + " seconds..."), false);
                                TeleportScheduler.schedule(requester, delay, () -> {
                                    requester.teleport(
                                            (ServerWorld) target.getWorld(),
                                            target.getX() + 0.5,
                                            target.getY(),
                                            target.getZ() + 0.5,
                                            EnumSet.noneOf(PositionFlag.class),
                                            requester.getYaw(),
                                            requester.getPitch(),
                                            false
                                    );
                                    requester.sendMessage(HomeLinkMessages.prefix("You teleported to " + target.getName().getString() + "."), false);
                                    target.sendMessage(HomeLinkMessages.prefix(requester.getName().getString() + " teleported to you."), false);
                                });
                            } else {
                                requester.sendMessage(HomeLinkMessages.prefix(target.getName().getString() + " accepted your teleport request."), false);
                                target.sendMessage(HomeLinkMessages.prefix("Teleporting you to " + requester.getName().getString() + " in " + delay + " seconds..."), false);
                                TeleportScheduler.schedule(target, delay, () -> {
                                    target.teleport(
                                            (ServerWorld) requester.getWorld(),
                                            requester.getX() + 0.5,
                                            requester.getY(),
                                            requester.getZ() + 0.5,
                                            EnumSet.noneOf(PositionFlag.class),
                                            target.getYaw(),
                                            target.getPitch(),
                                            false
                                    );
                                    requester.sendMessage(HomeLinkMessages.prefix(target.getName().getString() + " was teleported to you."), false);
                                    target.sendMessage(HomeLinkMessages.prefix("You teleported to " + requester.getName().getString() + "."), false);
                                });
                            }


                            return 1;
                        })
                )
        );
    }
}
