package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.storage.HomeStorage;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.sumutiu.homelink.HomeLink.LOGGER;

public class DelHomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("delhome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(HomeStorage::suggestHomeNames)
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                LOGGER.warn("[HomeLink]: This command can only be used by players.");
                                return 0;
                            }

                            String name = StringArgumentType.getString(ctx, "name");

                            if (HomeStorage.getHome(player, name) != null) {
                                HomeStorage.deleteHome(player, name);
                                player.sendMessage(HomeLinkMessages.prefix("Home '" + name + "' deleted."), false);
                                return 1;
                            } else {
                                player.sendMessage(HomeLinkMessages.prefix("Home '" + name + "' not found."), false);
                                return 0;
                            }
                        })
                )
        );
    }
}
