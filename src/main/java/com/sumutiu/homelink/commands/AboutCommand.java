package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class AboutCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("homelinkabout")
                .executes(ctx -> {
                    if (ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
                        HomeLinkMessages.sendModInfoToPlayer(player);
                    }
                    return 1;
                }));
    }
}
