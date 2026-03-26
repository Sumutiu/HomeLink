package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class SetHomeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("sethome")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> {

                                    CommandSourceStack source = context.getSource();

                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        HomeLinkMessages.Logger(1, HomeLinkMessages.PLAYER_ONLY_COMMAND);
                                        return 0;
                                    }

                                    String name = StringArgumentType.getString(context, "name");

                                    boolean success = HomeStorage.setHome(
                                            player,
                                            name,
                                            player.blockPosition()
                                    );

                                    if (success) {
                                        HomeLinkMessages.PrivateMessage(
                                                player,
                                                String.format(HomeLinkMessages.HOME_SET_NAMED, name)
                                        );
                                    } else {
                                        HomeLinkMessages.PrivateMessage(
                                                player,
                                                String.format(
                                                        HomeLinkMessages.HOME_LIMIT_REACHED,
                                                        HomeLinkConfig.getMaxHomes()
                                                )
                                        );
                                    }

                                    return success ? 1 : 0;
                                })
                        )
        );
    }
}