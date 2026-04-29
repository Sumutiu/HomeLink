package com.sumutiu.homelink.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import com.sumutiu.homelink.storage.BackStorage.BackData;
import com.sumutiu.homelink.util.TeleportScheduler;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.Objects;

import static com.sumutiu.homelink.HomeLink.HomeLinkInitialized;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

public class BackCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("tpback")
                .executes(ctx -> {

                    CommandSourceStack source = ctx.getSource();
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        Logger(1, PLAYER_ONLY_COMMAND);
                        return 0;
                    }

                    if (HomeLinkInitialized) {
                        BackData back = BackStorage.get(player);

                        if (back == null) {
                            PrivateMessage(player, NO_BACK_LOCATION);
                            return 0;
                        }

                        MinecraftServer server = source.getServer();

                        ResourceKey<Level> dimensionKey = ResourceKey.create(
                                Registries.DIMENSION,
                                Objects.requireNonNull(Identifier.tryParse(back.world))
                        );

                        ServerLevel targetWorld = server.getLevel(dimensionKey);

                        if (targetWorld == null) {
                            PrivateMessage(player,
                                    String.format(BACK_WORLD_NOT_FOUND, back.world));
                            return 0;
                        }

                        TeleportScheduler.schedule(player, null, HomeLinkConfig.getBackDelay(), () -> {

                            player.teleportTo(
                                    targetWorld,
                                    back.pos.getX() + 0.5,
                                    back.pos.getY() + 0.5,
                                    back.pos.getZ() + 0.5,
                                    EnumSet.noneOf(Relative.class),
                                    back.yaw,
                                    back.pitch,
                                    false // don't reset camera
                            );


                            PrivateMessage(player, BACK_TELEPORTED);
                        });

                        return 1;
                    } else {
                        PrivateMessage(player, MOD_INIT_NOT_READY);
                        return 0;
                    }
                })
        );
    }
}