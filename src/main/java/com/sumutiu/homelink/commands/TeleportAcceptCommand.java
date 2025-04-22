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
                                HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                return 0;
                            }

                            String requesterName = StringArgumentType.getString(ctx, "name");

                            MinecraftServer server = target.getServer();
                            if (server == null) {
                                HomeLinkMessages.Logger(2, HomeLinkMessages.SERVER_NOT_AVAILABLE);
                                return 0;
                            }

                            ServerPlayerEntity requester = server.getPlayerManager().getPlayer(requesterName);

                            if (requester == null) {
                                HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.PLAYER_NOT_FOUND, requesterName));
                                return 0;
                            }

                            TeleportRequest request = TeleportRequestManager.getRequest(target);
                            if (request == null || !request.requesterId().equals(requester.getUuid())) {
                                HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.NO_PENDING_REQUEST, requesterName));
                                return 0;
                            }

                            TeleportRequestManager.clearRequest(target);
                            int delay = HomeLinkConfig.getTeleportDelay();

                            if (request.type() == RequestType.TO) {
                                HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.TELEPORT_REQUEST_ACCEPTED, requester.getName().getString()));
                                HomeLinkMessages.PrivateMessage(requester, String.format(HomeLinkMessages.TELEPORTING_TO_IN_SECONDS, target.getName().getString(), delay));
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
                                    HomeLinkMessages.PrivateMessage(requester, String.format(HomeLinkMessages.YOU_TELEPORTED_TO_PLAYER, target.getName().getString()));
                                    HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.TELEPORTED_TO_YOU, requester.getName().getString()));
                                    HomeLinkMessages.Logger(0, String.format(HomeLinkMessages.LOG_TELEPORTED, requester.getName().getString(), target.getName().getString()));
                                });
                            } else {
                                HomeLinkMessages.PrivateMessage(requester, String.format(HomeLinkMessages.TELEPORT_REQUEST_ACCEPTED_BY, target.getName().getString()));
                                HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.TELEPORTING_YOU_TO_IN_SECONDS, requester.getName().getString(), delay));
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
                                    HomeLinkMessages.PrivateMessage(requester, String.format(HomeLinkMessages.PLAYER_WAS_TELEPORTED_TO_YOU, target.getName().getString()));
                                    HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.YOU_TELEPORTED_TO_PLAYER, requester.getName().getString()));
                                    HomeLinkMessages.Logger(0, String.format(HomeLinkMessages.LOG_PLAYER_WAS_TELEPORTED, target.getName().getString(), requester.getName().getString()));
                                });
                            }


                            return 1;
                        })
                )
        );
    }
}
