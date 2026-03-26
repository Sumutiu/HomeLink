package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.RequestType;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class TeleportHereCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tphere")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    return SharedSuggestionProvider.suggest(
                                            server.getPlayerNames(),
                                            builder
                                    );

                                })
                                .executes(ctx -> {

                                    CommandSourceStack source = ctx.getSource();

                                    if (!(source.getEntity() instanceof ServerPlayer requester)) {
                                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                        return 0;
                                    }

                                    String targetName = StringArgumentType.getString(ctx, "target");

                                    MinecraftServer server = source.getServer();

                                    ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);

                                    if (target == null) {
                                        HomeLinkMessages.PrivateMessage(
                                                requester,
                                                String.format(HomeLinkMessages.PLAYER_NOT_FOUND, targetName)
                                        );
                                        return 0;
                                    }

                                    if (target.equals(requester)) {
                                        HomeLinkMessages.PrivateMessage(
                                                requester,
                                                HomeLinkMessages.TELEPORT_SELF_DENIED
                                        );
                                        return 0;
                                    }

                                    if (TeleportRequestManager.hasRequest(target)) {
                                        HomeLinkMessages.PrivateMessage(
                                                requester,
                                                HomeLinkMessages.PLAYER_HAS_PENDING_REQUEST
                                        );
                                        return 0;
                                    }

                                    TeleportRequestManager.sendRequest(
                                            requester,
                                            target,
                                            RequestType.HERE
                                    );

                                    HomeLinkMessages.PrivateMessage(
                                            requester,
                                            String.format(
                                                    HomeLinkMessages.TELEPORT_REQUEST_SENT_TO,
                                                    target.getName().getString()
                                            )
                                    );

                                    HomeLinkMessages.PrivateMessage(
                                            target,
                                            String.format(
                                                    HomeLinkMessages.TELEPORT_REQUEST_PROMPT_HERE,
                                                    requester.getName().getString(),
                                                    requester.getName().getString()
                                            )
                                    );

                                    return 1;
                                })
                        )
        );
    }
}