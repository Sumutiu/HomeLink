package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.RequestType;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class TeleportToCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpto")
                .then(CommandManager.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            MinecraftServer server = context.getSource().getServer();
                            if (server != null) {
                                return CommandSource.suggestMatching(
                                        server.getPlayerNames(), builder
                                );
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity requester)) {
                                HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                return 0;
                            }

                            String targetName = StringArgumentType.getString(ctx, "target");

                            MinecraftServer server = requester.getServer();
                            if (server == null) {
                                HomeLinkMessages.Logger(2, HomeLinkMessages.SERVER_NOT_AVAILABLE);
                                return 0;
                            }

                            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
                            if (target == null) {
                                HomeLinkMessages.PrivateMessage(requester, String.format(HomeLinkMessages.PLAYER_NOT_FOUND, targetName));
                                return 0;
                            }

                            if (target == requester) {
                                HomeLinkMessages.PrivateMessage(requester, HomeLinkMessages.TELEPORT_SELF_DENIED);
                                return 0;
                            }

                            if (TeleportRequestManager.hasRequest(target)) {
                                HomeLinkMessages.PrivateMessage(requester, HomeLinkMessages.PLAYER_HAS_PENDING_REQUEST);
                                return 0;
                            }

                            TeleportRequestManager.sendRequest(requester, target, RequestType.TO);

                            HomeLinkMessages.PrivateMessage(requester, String.format(HomeLinkMessages.TELEPORT_REQUEST_SENT_TO, target.getName().getString()));
                            HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.TELEPORT_REQUEST_PROMPT_TO, requester.getName().getString(), requester.getName().getString()));

                            return 1;
                        })
                )
        );
    }
}
