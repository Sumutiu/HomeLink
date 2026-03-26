package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.TeleportRequest;
import com.sumutiu.homelink.teleport.TeleportRequestManager.RequestType;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

import java.util.EnumSet;

public class TeleportAcceptCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tpaccept")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {

                                    if (!(context.getSource().getEntity() instanceof ServerPlayer target)) {
                                        return builder.buildFuture();
                                    }

                                    return TeleportRequestManager.suggestPendingRequestNames(target, builder);
                                })
                                .executes(ctx -> {

                                    CommandSourceStack source = ctx.getSource();

                                    if (!(source.getEntity() instanceof ServerPlayer target)) {
                                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                        return 0;
                                    }

                                    String requesterName = StringArgumentType.getString(ctx, "name");

                                    MinecraftServer server = source.getServer();

                                    ServerPlayer requester = server.getPlayerList().getPlayerByName(requesterName);

                                    if (requester == null) {
                                        HomeLinkMessages.PrivateMessage(
                                                target,
                                                String.format(HomeLinkMessages.PLAYER_NOT_FOUND, requesterName)
                                        );
                                        return 0;
                                    }

                                    TeleportRequest request = TeleportRequestManager.getRequest(target);

                                    if (request == null || !request.requesterId().equals(requester.getUUID())) {
                                        HomeLinkMessages.PrivateMessage(
                                                target,
                                                String.format(HomeLinkMessages.NO_PENDING_REQUEST, requesterName)
                                        );
                                        return 0;
                                    }

                                    TeleportRequestManager.clearRequest(target.getUUID());

                                    int delay = HomeLinkConfig.getTeleportDelay();

                                    // =========================
                                    // REQUEST TYPE: TO
                                    // =========================
                                    if (request.type() == RequestType.TO) {

                                        HomeLinkMessages.PrivateMessage(
                                                target,
                                                String.format(HomeLinkMessages.TELEPORT_REQUEST_ACCEPTED, requester.getName().getString())
                                        );

                                        HomeLinkMessages.PrivateMessage(
                                                requester,
                                                String.format(HomeLinkMessages.TELEPORTING_TO_IN_SECONDS,
                                                        target.getName().getString(),
                                                        delay)
                                        );

                                        TeleportScheduler.schedule(requester, target, delay, () -> {

                                            requester.teleportTo(
                                                    target.level(),
                                                    target.getX() + 0.5,
                                                    target.getY(),
                                                    target.getZ() + 0.5,
                                                    EnumSet.noneOf(Relative.class),
                                                    requester.getYRot(),
                                                    requester.getXRot(),
                                                    false // don't reset camera
                                            );

                                            HomeLinkMessages.PrivateMessage(
                                                    requester,
                                                    String.format(HomeLinkMessages.YOU_TELEPORTED_TO_PLAYER,
                                                            target.getName().getString())
                                            );

                                            HomeLinkMessages.PrivateMessage(
                                                    target,
                                                    String.format(HomeLinkMessages.TELEPORTED_TO_YOU,
                                                            requester.getName().getString())
                                            );

                                            if (requester.isAlive() && target.isAlive()) {
                                                HomeLinkMessages.Logger(
                                                        0,
                                                        String.format(HomeLinkMessages.LOG_TELEPORTED,
                                                                requester.getName().getString(),
                                                                target.getName().getString())
                                                );
                                            }
                                        });

                                    }

                                    // =========================
                                    // REQUEST TYPE: HERE
                                    // =========================
                                    else {

                                        HomeLinkMessages.PrivateMessage(
                                                requester,
                                                String.format(HomeLinkMessages.TELEPORT_REQUEST_ACCEPTED_BY,
                                                        target.getName().getString())
                                        );

                                        HomeLinkMessages.PrivateMessage(
                                                target,
                                                String.format(HomeLinkMessages.TELEPORTING_YOU_TO_IN_SECONDS,
                                                        requester.getName().getString(),
                                                        delay)
                                        );

                                        TeleportScheduler.schedule(target, requester, delay, () -> {

                                            target.teleportTo(
                                                    requester.level(),
                                                    requester.getX() + 0.5,
                                                    requester.getY(),
                                                    requester.getZ() + 0.5,
                                                    EnumSet.noneOf(Relative.class),
                                                    target.getYRot(),
                                                    target.getXRot(),
                                                    false // don't reset camera
                                            );

                                            HomeLinkMessages.PrivateMessage(
                                                    requester,
                                                    String.format(HomeLinkMessages.PLAYER_WAS_TELEPORTED_TO_YOU,
                                                            target.getName().getString())
                                            );

                                            HomeLinkMessages.PrivateMessage(
                                                    target,
                                                    String.format(HomeLinkMessages.YOU_TELEPORTED_TO_PLAYER,
                                                            requester.getName().getString())
                                            );

                                            if (requester.isAlive() && target.isAlive()) {
                                                HomeLinkMessages.Logger(
                                                        0,
                                                        String.format(HomeLinkMessages.LOG_PLAYER_WAS_TELEPORTED,
                                                                target.getName().getString(),
                                                                requester.getName().getString())
                                                );
                                            }
                                        });
                                    }

                                    return 1;
                                })
                        )
        );
    }
}