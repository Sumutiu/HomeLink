package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.RequestType;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.sumutiu.homelink.HomeLink.LOGGER;

public class TeleportHereCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("teleporthere")
                .then(CommandManager.argument("target", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity requester)) {
                                LOGGER.warn("[HomeLink]: This command can only be used by players.");
                                return 0;
                            }

                            String targetName = StringArgumentType.getString(ctx, "target");

                            MinecraftServer server = requester.getServer();
                            if (server == null) {
                                LOGGER.error("[HomeLink]: Server not available.");
                                return 0;
                            }

                            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
                            if (target == null) {
                                requester.sendMessage(HomeLinkMessages.prefix("Player '" + targetName + "' not found."), false);
                                return 0;
                            }

                            if (target == requester) {
                                requester.sendMessage(HomeLinkMessages.prefix("You cannot teleport yourself to yourself."), false);
                                return 0;
                            }

                            if (TeleportRequestManager.hasRequest(target)) {
                                requester.sendMessage(HomeLinkMessages.prefix("That player already has a pending request."), false);
                                return 0;
                            }

                            TeleportRequestManager.sendRequest(requester, target, RequestType.HERE);

                            requester.sendMessage(HomeLinkMessages.prefix("Teleport request sent to " + target.getName().getString() + "."), false);
                            target.sendMessage(HomeLinkMessages.prefix(requester.getName().getString() + " wants to teleport you to them. Type /teleportaccept " + requester.getName().getString() + " to accept."), false);

                            return 1;
                        })
                )
        );
    }
}
