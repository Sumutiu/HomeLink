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
                                System.out.println("[HomeLink]: This command can only be used by players.");
                                return 0;
                            }

                            String name = StringArgumentType.getString(context, "name");

                            boolean success = HomeStorage.setHome(player, name, player.getBlockPos());

                            if (success) {
                                player.sendMessage(HomeLinkMessages.prefix("Home '" + name + "' set at your current location."), false);
                            } else {
                                player.sendMessage(HomeLinkMessages.prefix("You have reached the maximum number of homes (" +
                                        HomeLinkConfig.getMaxHomes() + "). Delete one to set another."), false);
                            }
                            return success ? 1 : 0;
                        })
                )
        );
    }
}
