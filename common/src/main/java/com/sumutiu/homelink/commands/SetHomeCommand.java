package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.HomeStorage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static com.sumutiu.homelink.HomeLink.HomeLinkInitialized;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class SetHomeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("sethome")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> {

                                    CommandSourceStack source = context.getSource();
                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        Logger(1, PLAYER_ONLY_COMMAND);
                                        return 0;
                                    }

                                    if (HomeLinkInitialized) {
                                        String name = StringArgumentType.getString(context, "name");

                                        HomeStorage.SetHomeResult result = HomeStorage.setHome(player, name, player.blockPosition());

                                        switch (result) {
                                            case SET -> PrivateMessage(player, String.format(HOME_SET_NAMED, name));
                                            case LIMIT_REACHED -> PrivateMessage(player, String.format(HOME_LIMIT_REACHED, HomeLinkConfig.getMaxHomes()));
                                            case UNAVAILABLE -> PrivateMessage(player, HOME_DATA_UNAVAILABLE);
                                        }

                                        return result == HomeStorage.SetHomeResult.SET ? 1 : 0;
                                    } else {
                                        PrivateMessage(player, MOD_INIT_NOT_READY);
                                        return 0;
                                    }
                                })
                        )
        );
    }
}