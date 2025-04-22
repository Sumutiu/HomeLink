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

import static com.sumutiu.homelink.HomeLink.LOGGER;

public class TeleportDenyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("teleportdeny")
                .then(CommandManager.argument("name", StringArgumentType.word())
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

                            target.sendMessage(HomeLinkMessages.prefix("Denied teleport request from " + requester.getName().getString() + "."), false);
                            requester.sendMessage(HomeLinkMessages.prefix("Your teleport request to " + target.getName().getString() + " was denied."), false);

                            return 1;
                        })
                )
        );
    }
}
