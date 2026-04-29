package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.TeleportRequest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import static com.sumutiu.homelink.HomeLink.HomeLinkInitialized;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class TeleportDenyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tpdeny")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {

                                    if (!(context.getSource().getEntity() instanceof ServerPlayer target)) { return builder.buildFuture(); }
                                    return TeleportRequestManager.suggestPendingRequestNames(target, builder);
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

                                        TeleportRequestManager.clearRequest(target.getUUID());

                                        PrivateMessage(target, String.format(TELEPORT_REQUEST_DENIED_FROM, requester.getName().getString()));
                                        PrivateMessage(requester, String.format(TELEPORT_REQUEST_DENIED_TO, target.getName().getString()));
                                        Logger(0, String.format(LOG_TELEPORT_REQUEST_DENIED, requester.getName().getString(), target.getName().getString()));

                                        return 1;
                                    } else {
                                        PrivateMessage(target, MOD_INIT_NOT_READY);
                                        return 0;
                                    }
                                })
                        )
        );
    }
}