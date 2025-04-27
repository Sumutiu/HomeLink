package com.sumutiu.homelink.util;

import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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

public class TeleportScheduler {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName(HomeLinkMessages.Mod_ID + " - " + HomeLinkMessages.SCHEDULER_SERVICE_NAME);
        return thread;
    });

    public static void initialize() {
        // Cleanup when a player disconnects
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) { dataCleanup(player.getUuid()); }
        });

        // Cancel teleport if player takes damage
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) { cancelPlayerTeleportOnDamage(player); }
            return true;
        });
    }

    private static final Map<UUID, BlockPos> teleportPositions = new ConcurrentHashMap<>();
    private static final Set<UUID> activeTeleports = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> cancelTeleportsOnDamage = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> cancelTeleportsOnCancel = ConcurrentHashMap.newKeySet();

    public static void schedule(ServerPlayerEntity teleportedPlayer, ServerPlayerEntity targetPlayer, int delaySeconds, Runnable teleportTask) {
        UUID teleportedPlayerUUID = teleportedPlayer != null ? teleportedPlayer.getUuid() : null;

        if (teleportedPlayer != null && isTeleporting(teleportedPlayer)) {
            HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_IN_PROGRESS);
            activeTeleports.add(teleportedPlayerUUID);
            return;
        }

        if (delaySeconds <= 0) {
            teleportTask.run();
            activeTeleports.remove(teleportedPlayerUUID);
            return;
        }

        if (teleportedPlayer != null) {
            BlockPos initialPos = teleportedPlayer.getBlockPos();
            teleportPositions.put(teleportedPlayerUUID, initialPos);
            HomeLinkMessages.PrivateMessage(teleportedPlayer, String.format(HomeLinkMessages.TELEPORT_DELAY_MESSAGE, delaySeconds));
        }

        scheduler.schedule(() -> {
            boolean teleportValid = HomeLinkMessages.isConnected(teleportedPlayer);
            boolean targetStillConnected = targetPlayer == null || HomeLinkMessages.isConnected(targetPlayer);

            if (!teleportValid || !targetStillConnected) {
                if (HomeLinkMessages.isConnected(targetPlayer)) {
                    HomeLinkMessages.PrivateMessage(targetPlayer, HomeLinkMessages.TELEPORT_CANCELLED_DISCONNECT);
                }
                if (HomeLinkMessages.isConnected(teleportedPlayer)) {
                    HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_CANCELLED_DISCONNECT);
                }
            } else {
                BlockPos currentPos = teleportedPlayer.getBlockPos();
                boolean cancelOnMove = HomeLinkConfig.getCancelOnMove();

                if (cancelOnMove && !currentPos.equals(teleportPositions.get(teleportedPlayerUUID))) {
                    HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_CANCELLED_MOVEMENT);
                } else if (cancelTeleportsOnDamage.remove(teleportedPlayerUUID)) {
                    HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_CANCELLED_DAMAGED);
                } else if (cancelTeleportsOnCancel.remove(teleportedPlayerUUID)) {
                    HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_CANCELLED_CANCEL);
                } else {
                    // Successful teleport
                    BackStorage.save(teleportedPlayer, teleportedPlayer.getBlockPos());
                    teleportTask.run();

                    teleportedPlayer.getWorld().playSound(
                            null,
                            teleportedPlayer.getX(),
                            teleportedPlayer.getY(),
                            teleportedPlayer.getZ(),
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                    );

                    ((ServerWorld) teleportedPlayer.getWorld()).spawnParticles(
                            ParticleTypes.PORTAL,
                            teleportedPlayer.getX(),
                            teleportedPlayer.getY() + 1,
                            teleportedPlayer.getZ(),
                            32, 0.5, 0.5, 0.5, 0.2
                    );
                }
            }
            dataCleanup(teleportedPlayerUUID);
        }, delaySeconds, TimeUnit.SECONDS);
    }

    public static boolean isTeleporting(ServerPlayerEntity player) {
        return activeTeleports.contains(player.getUuid());
    }

    public static void cancelPlayerTeleportOnDamage(ServerPlayerEntity player) {
        if (isTeleporting(player)) {
            cancelTeleportsOnDamage.add(player.getUuid());
        }
    }

    public static void cancelPlayerTeleportOnCancel(ServerPlayerEntity player) {
        if (isTeleporting(player)) {
            cancelTeleportsOnCancel.add(player.getUuid());
            HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.TELEPORT_CANCEL_QUEUED);
        } else {
            HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.NO_PENDING_TELEPORT);
        }
    }

    private static void dataCleanup(UUID playerId) {
        teleportPositions.remove(playerId);
        activeTeleports.remove(playerId);
        cancelTeleportsOnDamage.remove(playerId);
        cancelTeleportsOnCancel.remove(playerId);
        TeleportRequestManager.clearRequest(playerId);
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
