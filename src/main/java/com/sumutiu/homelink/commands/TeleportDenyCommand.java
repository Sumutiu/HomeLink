package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.TeleportRequest;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class TeleportDenyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("teleportdeny")
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

                            HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.TELEPORT_REQUEST_DENIED_FROM, requester.getName().getString()));
                            HomeLinkMessages.PrivateMessage(requester, String.format(HomeLinkMessages.TELEPORT_REQUEST_DENIED_TO, target.getName().getString()));

                            return 1;
                        })
                )
        );
    }
}
