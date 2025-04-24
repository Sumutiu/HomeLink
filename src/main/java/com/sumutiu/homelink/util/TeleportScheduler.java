package com.sumutiu.homelink.util;

import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

// This handles active teleports

public class TeleportScheduler {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName(HomeLinkMessages.Mod_ID + " - " + HomeLinkMessages.SCHEDULER_SERVICE_NAME);
        return thread;
    });

    private static final Map<UUID, BlockPos> teleportPositions = new ConcurrentHashMap<>();
    private static final Set<UUID> activeTeleports = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> cancelTeleportsOnDamage = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> cancelTeleportsOnCancel = ConcurrentHashMap.newKeySet();

    public static void schedule(ServerPlayerEntity player, int delaySeconds, Runnable teleportTask) {
        UUID uuid = player.getUuid();

        if (isTeleporting(player)) {
            HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.TELEPORT_IN_PROGRESS);
            return;
        }

        activeTeleports.add(uuid);

        if (delaySeconds <= 0) {
            teleportTask.run();
            activeTeleports.remove(uuid);
            return;
        }

        BlockPos initialPos = player.getBlockPos();
        teleportPositions.put(uuid, initialPos);

        HomeLinkMessages.PrivateMessage(player, String.format(HomeLinkMessages.TELEPORT_DELAY_MESSAGE, delaySeconds));

        scheduler.schedule(() -> {
            boolean cancelOnMove = HomeLinkConfig.getCancelOnMove();
            BlockPos currentPos = player.getBlockPos();

            if (cancelOnMove && !currentPos.equals(teleportPositions.get(uuid))) {
                HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.TELEPORT_CANCELLED_MOVEMENT);
            } else if (cancelTeleportsOnDamage.contains(uuid)) {
                HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.TELEPORT_CANCELLED_DAMAGED);
                cancelTeleportsOnDamage.remove(uuid);
            } else if (cancelTeleportsOnCancel.contains(uuid)) {
                HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.TELEPORT_CANCELLED_CANCEL);
                cancelTeleportsOnCancel.remove(uuid);
            } else {
                BackStorage.save(player, player.getBlockPos());
                teleportTask.run();

                player.getWorld().playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.PLAYERS,
                        1.0f,
                        1.0f
                );

                ((ServerWorld) player.getWorld()).spawnParticles(
                        ParticleTypes.PORTAL,
                        player.getX(),
                        player.getY() + 1,
                        player.getZ(),
                        32,     // count
                        0.5,    // offset X
                        0.5,    // offset Y
                        0.5,    // offset Z
                        0.2     // speed
                );
            }

            teleportPositions.remove(uuid);
            activeTeleports.remove(uuid);

        }, delaySeconds, TimeUnit.SECONDS);
    }

    public static boolean isTeleporting(ServerPlayerEntity player) {
        return activeTeleports.contains(player.getUuid());
    }

    public static void cancelPlayerTeleportOnDamage(ServerPlayerEntity player) {
        if (TeleportScheduler.isTeleporting(player)) { cancelTeleportsOnDamage.add(player.getUuid()); }
    }

    public static void cancelPlayerTeleportOnCancel(ServerPlayerEntity player) {
        if (TeleportScheduler.isTeleporting(player)) {
            cancelTeleportsOnCancel.add(player.getUuid());
            HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.TELEPORT_CANCEL_QUEUED);
        } else { HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.NO_PENDING_TELEPORT); }
    }

    public static void shutdown() {
        try {
            scheduler.shutdownNow();
            HomeLinkMessages.Logger(0, HomeLinkMessages.TELEPORT_SCHEDULER_SHUTDOWN);
        } catch (Exception e) {
            HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.TELEPORT_SCHEDULER_SHUTDOWN_FAILED, e.getMessage()));
        }
    }
}
