package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class CancelCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpcancel")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                        return 0;
                    }

                    TeleportScheduler.cancelPlayerTeleportOnCancel(player);
                    return 1;
                })
        );
    }
}