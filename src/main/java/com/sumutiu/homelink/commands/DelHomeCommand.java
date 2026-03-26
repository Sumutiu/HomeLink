package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class DelHomeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("delhome")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(HomeStorage::suggestHomeNames)
                                .executes(ctx -> {

                                    CommandSourceStack source = ctx.getSource();

                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                        return 0;
                                    }

                                    String name = StringArgumentType.getString(ctx, "name");

                                    if (HomeStorage.getHome(player, name) != null) {

                                        HomeStorage.deleteHome(player, name);

                                        HomeLinkMessages.PrivateMessage(
                                                player,
                                                String.format(HomeLinkMessages.HOME_DELETED, name)
                                        );

                                        return 1;

                                    } else {

                                        HomeLinkMessages.PrivateMessage(
                                                player,
                                                String.format(HomeLinkMessages.HOME_NOT_FOUND, name)
                                        );

                                        return 0;
                                    }
                                })
                        )
        );
    }
}