package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.TeleportRequest;
import com.sumutiu.homelink.teleport.TeleportRequestManager.RequestType;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

import java.util.EnumSet;

import static com.sumutiu.homelink.HomeLink.HomeLinkInitialized;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class TeleportAcceptCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tpaccept")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    if (!(context.getSource().getEntity() instanceof ServerPlayer target)) {
                                        return builder.buildFuture();
                                    }
                                    return TeleportRequestManager.suggestPendingRequestNames(context.getSource().getServer(), target, builder);
                                })

                                .executes(ctx -> {

                                    CommandSourceStack source = ctx.getSource();
                                    if (!(source.getEntity() instanceof ServerPlayer target)) {
                                        Logger(1, PLAYER_ONLY_COMMAND);
                                        return 0;
                                    }

                                    if (HomeLinkInitialized) {
                                        String requesterName = StringArgumentType.getString(ctx, "name");
                                        MinecraftServer server = source.getServer();
                                        ServerPlayer requester = server.getPlayerList().getPlayerByName(requesterName);

                                        if (requester == null) {
                                            PrivateMessage(target, String.format(PLAYER_NOT_FOUND, requesterName));
                                            return 0;
                                        }

                                        TeleportRequest request = TeleportRequestManager.getRequest(target);

                                        if (request == null || !request.requesterId().equals(requester.getUUID())) {
                                            PrivateMessage(target, String.format(NO_PENDING_REQUEST, requesterName));
                                            return 0;
                                        }

                                        // The player who will be moved must not have another teleport pending.
                                        // If they do, keep the request so it can be accepted again in a moment.
                                        ServerPlayer teleported = request.type() == RequestType.TO ? requester : target;
                                        if (TeleportScheduler.isTeleporting(teleported)) {
                                            PrivateMessage(target, teleported == target
                                                    ? TELEPORT_IN_PROGRESS
                                                    : String.format(PLAYER_TELEPORT_IN_PROGRESS, requester.getName().getString()));
                                            return 0;
                                        }

                                        // Only this request; requests the player sent to others stay open
                                        TeleportRequestManager.removeRequestFor(target);

                                        int delay = HomeLinkConfig.getTeleportDelay();
                                        boolean scheduled;

                                        // =========================
                                        // REQUEST TYPE: TO
                                        // =========================
                                        if (request.type() == RequestType.TO) {

                                            PrivateMessage(target, String.format(TELEPORT_REQUEST_ACCEPTED, requester.getName().getString()));

                                            String delayMessage = String.format(TELEPORTING_TO_IN_SECONDS, target.getName().getString(), delay);
                                            scheduled = TeleportScheduler.schedule(requester, target, delay, delayMessage, () -> {
                                                // getX()/getZ() are exact positions, not block corners, so no +0.5
                                                requester.teleportTo(
                                                        target.level(),
                                                        target.getX(),
                                                        target.getY(),
                                                        target.getZ(),
                                                        EnumSet.noneOf(Relative.class),
                                                        requester.getYRot(),
                                                        requester.getXRot(),
                                                        false // don't reset camera
                                                );

                                                PrivateMessage(requester, String.format(YOU_TELEPORTED_TO_PLAYER, target.getName().getString()));
                                                PrivateMessage(target, String.format(TELEPORTED_TO_YOU, requester.getName().getString()));

                                                if (requester.isAlive() && target.isAlive()) {
                                                    Logger(0, String.format(LOG_TELEPORTED, requester.getName().getString(), target.getName().getString()));
                                                }
                                            });

                                        } else {

                                            // =========================
                                            // REQUEST TYPE: HERE
                                            // =========================
                                            PrivateMessage(requester, String.format(TELEPORT_REQUEST_ACCEPTED_BY, target.getName().getString()));

                                            String delayMessage = String.format(TELEPORTING_YOU_TO_IN_SECONDS, requester.getName().getString(), delay);
                                            scheduled = TeleportScheduler.schedule(target, requester, delay, delayMessage, () -> {
                                                target.teleportTo(
                                                        requester.level(),
                                                        requester.getX(),
                                                        requester.getY(),
                                                        requester.getZ(),
                                                        EnumSet.noneOf(Relative.class),
                                                        target.getYRot(),
                                                        target.getXRot(),
                                                        false // don't reset camera
                                                );

                                                PrivateMessage(requester, String.format(PLAYER_WAS_TELEPORTED_TO_YOU, target.getName().getString()));
                                                PrivateMessage(target, String.format(YOU_TELEPORTED_TO_PLAYER, requester.getName().getString()));

                                                if (requester.isAlive() && target.isAlive()) {
                                                    Logger(0, String.format(LOG_PLAYER_WAS_TELEPORTED, target.getName().getString(), requester.getName().getString()));
                                                }
                                            });
                                        }

                                        return scheduled ? 1 : 0;
                                    } else {
                                        PrivateMessage(target, MOD_INIT_NOT_READY);
                                        return 0;
                                    }
                                })
                        )
        );
    }
}