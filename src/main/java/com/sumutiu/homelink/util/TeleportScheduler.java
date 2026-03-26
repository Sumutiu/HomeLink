package com.sumutiu.homelink.util;

import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import com.sumutiu.homelink.teleport.TeleportRequestManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;

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

        // Disconnect cleanup
        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> {
            ServerPlayer player = handler.getPlayer();
            dataCleanup(player.getUUID());
        });

        // Cancel on damage
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, _, _) -> {
            if (entity instanceof ServerPlayer player) {
                cancelPlayerTeleportOnDamage(player);
            }
            return true;
        });
    }

    private static final Map<UUID, BlockPos> teleportPositions = new ConcurrentHashMap<>();
    private static final Set<UUID> activeTeleports = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> cancelTeleportsOnDamage = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> cancelTeleportsOnCancel = ConcurrentHashMap.newKeySet();

    public static void schedule(ServerPlayer teleportedPlayer, ServerPlayer targetPlayer, int delaySeconds, Runnable teleportTask) {

        UUID uuid = teleportedPlayer != null ? teleportedPlayer.getUUID() : null;

        if (teleportedPlayer != null && isTeleporting(teleportedPlayer)) {
            HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_IN_PROGRESS);
            activeTeleports.add(uuid);
            return;
        }

        if (delaySeconds <= 0) {
            teleportTask.run();
            activeTeleports.remove(uuid);
            return;
        }

        if (teleportedPlayer != null) {
            BlockPos initialPos = teleportedPlayer.blockPosition();
            teleportPositions.put(uuid, initialPos);

            HomeLinkMessages.PrivateMessage(
                    teleportedPlayer,
                    String.format(HomeLinkMessages.TELEPORT_DELAY_MESSAGE, delaySeconds)
            );
        }

        scheduler.schedule(() -> {

            boolean teleportValid = HomeLinkMessages.isConnected(teleportedPlayer);
            boolean targetValid = targetPlayer == null || HomeLinkMessages.isConnected(targetPlayer);

            if (!teleportValid || !targetValid) {

                if (HomeLinkMessages.isConnected(targetPlayer)) {
                    HomeLinkMessages.PrivateMessage(targetPlayer, HomeLinkMessages.TELEPORT_CANCELLED_DISCONNECT);
                }

                if (HomeLinkMessages.isConnected(teleportedPlayer)) {
                    HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_CANCELLED_DISCONNECT);
                }

            } else {

                BlockPos currentPos = teleportedPlayer.blockPosition();
                boolean cancelOnMove = HomeLinkConfig.getCancelOnMove();

                if (cancelOnMove && !currentPos.equals(teleportPositions.get(uuid))) {

                    HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_CANCELLED_MOVEMENT);

                } else if (cancelTeleportsOnDamage.remove(uuid)) {

                    HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_CANCELLED_DAMAGED);

                } else if (cancelTeleportsOnCancel.remove(uuid)) {

                    HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_CANCELLED_CANCEL);

                } else {

                    BackStorage.save(teleportedPlayer, teleportedPlayer.blockPosition());

                    makePlayerInvulnerable(teleportedPlayer, HomeLinkConfig.getInvulnerabilityTime());

                    teleportTask.run();

                    ServerLevel level = teleportedPlayer.level();

                    level.playSound(
                            null,
                            teleportedPlayer.getX(),
                            teleportedPlayer.getY(),
                            teleportedPlayer.getZ(),
                            SoundEvents.ENDERMAN_TELEPORT,
                            SoundSource.PLAYERS,
                            1.0f,
                            1.0f
                    );

                    level.sendParticles(
                            ParticleTypes.PORTAL,
                            teleportedPlayer.getX(),
                            teleportedPlayer.getY() + 1,
                            teleportedPlayer.getZ(),
                            32,
                            0.5, 0.5, 0.5,
                            0.2
                    );
                }
            }

            dataCleanup(uuid);

        }, delaySeconds, TimeUnit.SECONDS);
    }

    public static boolean isTeleporting(ServerPlayer player) {
        return activeTeleports.contains(player.getUUID());
    }

    public static void cancelPlayerTeleportOnDamage(ServerPlayer player) {
        if (isTeleporting(player)) {
            cancelTeleportsOnDamage.add(player.getUUID());
        }
    }

    public static void cancelPlayerTeleportOnCancel(ServerPlayer player) {
        if (isTeleporting(player)) {
            cancelTeleportsOnCancel.add(player.getUUID());
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
            HomeLinkMessages.Logger(2,
                    String.format(HomeLinkMessages.TELEPORT_SCHEDULER_SHUTDOWN_FAILED, e.getMessage()));
        }
    }

    public static void makePlayerInvulnerable(ServerPlayer player, int durationSeconds) {
        player.setInvulnerable(true);

        scheduler.schedule(() -> {
            if (player.isAlive()) {
                player.setInvulnerable(false);
            }
        }, durationSeconds, TimeUnit.SECONDS);
    }
}