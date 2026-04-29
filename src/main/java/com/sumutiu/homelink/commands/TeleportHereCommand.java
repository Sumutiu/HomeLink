package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import com.sumutiu.homelink.teleport.TeleportRequestManager.RequestType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import static com.sumutiu.homelink.HomeLink.HomeLinkInitialized;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class TeleportHereCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tphere")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    return SharedSuggestionProvider.suggest(server.getPlayerNames(), builder);
                                })
                                .executes(ctx -> {

                                    CommandSourceStack source = ctx.getSource();
                                    if (!(source.getEntity() instanceof ServerPlayer requester)) {
                                        Logger(1, PLAYER_ONLY_COMMAND);
                                        return 0;
                                    }

                                    if (HomeLinkInitialized) {

                                        String targetName = StringArgumentType.getString(ctx, "target");
                                        MinecraftServer server = source.getServer();
                                        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);

                                        if (target == null) {
                                            PrivateMessage(requester, String.format(PLAYER_NOT_FOUND, targetName));
                                            return 0;
                                        }

                                        if (target.equals(requester)) {
                                            PrivateMessage(requester, TELEPORT_SELF_DENIED);
                                            return 0;
                                        }

                                        if (TeleportRequestManager.hasRequest(target)) {
                                            PrivateMessage(requester, PLAYER_HAS_PENDING_REQUEST);
                                            return 0;
                                        }

                                        TeleportRequestManager.sendRequest(requester, target, RequestType.HERE);
                                        PrivateMessage(requester, String.format(TELEPORT_REQUEST_SENT_TO, target.getName().getString()));
                                        PrivateMessage(target, String.format(TELEPORT_REQUEST_PROMPT_HERE, requester.getName().getString(), requester.getName().getString()));

                                        return 1;

                                    } else {
                                        PrivateMessage(requester, MOD_INIT_NOT_READY);
                                        return 0;
                                    }
                                })
                        )
        );
    }
}