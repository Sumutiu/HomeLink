package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class DelHomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("delhome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(HomeStorage::suggestHomeNames)
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                return 0;
                            }

                            String name = StringArgumentType.getString(ctx, "name");

                            if (HomeStorage.getHome(player, name) != null) {
                                HomeStorage.deleteHome(player, name);
                                HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.HOME_DELETED, name));
                                return 1;
                            } else {
                                HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.HOME_NOT_FOUND, name));
                                return 0;
                            }
                        })
                )
        );
    }
}
