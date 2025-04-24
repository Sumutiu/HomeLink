package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CancelCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpcancel")
                .executes(ctx -> {
                    ServerCommandSource source = ctx.getSource();
                    if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                        return 0;
                    }

                    MinecraftServer server = player.getServer();
                    if (server == null) {
                        HomeLinkMessages.Logger(2, HomeLinkMessages.SERVER_NOT_AVAILABLE);
                        return 0;
                    }

                    TeleportScheduler.cancelPlayerTeleportOnCancel(player);
                    return 1;
                })
        );
    }
}
