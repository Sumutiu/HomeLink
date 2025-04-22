package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.util.HomeLinkMessages;
import com.sumutiu.homelink.storage.HomeStorage;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class SetHomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sethome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                return 0;
                            }

                            String name = StringArgumentType.getString(context, "name");

                            boolean success = HomeStorage.setHome(player, name, player.getBlockPos());

                            if (success) {
                                HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.HOME_SET_NAMED, name));
                            } else {
                                HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.HOME_LIMIT_REACHED, HomeLinkConfig.getMaxHomes()));
                            }
                            return success ? 1 : 0;
                        })
                )
        );
    }
}
