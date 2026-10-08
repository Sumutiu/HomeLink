package com.sumutiu.homelink.util;

import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delayed teleports. Everything here runs on the server thread: pending teleports are
 * checked every server tick (see HomeLink), never from a background thread, because
 * Minecraft's world and players must only be changed from the server thread.
 */
public class TeleportScheduler {

    private static final class PendingTeleport {
        final ServerPlayer player;      // the player being teleported
        final ServerPlayer otherPlayer; // the other player for /tpto and /tphere, null for /home and /tpback
        final Runnable teleportTask;
        final BlockPos startPos;
        final long runAtMs;

        PendingTeleport(ServerPlayer player, ServerPlayer otherPlayer, Runnable teleportTask, long runAtMs) {
            this.player = player;
            this.otherPlayer = otherPlayer;
            this.teleportTask = teleportTask;
            this.startPos = player.blockPosition();
            this.runAtMs = runAtMs;
        }
    }

    // Player UUID -> their pending teleport
    private static final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    // Player UUID -> time until which they can't take damage (after arriving)
    private static final Map<UUID, Long> protectedUntilMs = new ConcurrentHashMap<>();

    // Player UUID -> when their last protection started (protection can't be chained back to back)
    private static final Map<UUID, Long> lastProtectionStartMs = new ConcurrentHashMap<>();

    /**
     * Schedules a teleport. Returns false (and tells the player) if they already have one pending.
     */
    public static boolean schedule(ServerPlayer teleportedPlayer, ServerPlayer targetPlayer, int delaySeconds, Runnable teleportTask) {
        return schedule(teleportedPlayer, targetPlayer, delaySeconds, null, teleportTask);
    }

    /**
     * Same as above, with a custom "teleporting in N seconds" message (null = the default one).
     */
    public static boolean schedule(ServerPlayer teleportedPlayer, ServerPlayer targetPlayer, int delaySeconds, String delayMessage, Runnable teleportTask) {

        if (isTeleporting(teleportedPlayer)) {
            HomeLinkMessages.PrivateMessage(teleportedPlayer, HomeLinkMessages.TELEPORT_IN_PROGRESS);
            return false;
        }

        PendingTeleport teleport = new PendingTeleport(teleportedPlayer, targetPlayer, teleportTask, nowMs() + delaySeconds * 1000L);

        if (delaySeconds <= 0) {
            performTeleport(teleport);
            return true;
        }

        pendingTeleports.put(teleportedPlayer.getUUID(), teleport);

        HomeLinkMessages.PrivateMessage(
                teleportedPlayer,
                delayMessage != null ? delayMessage : String.format(HomeLinkMessages.TELEPORT_DELAY_MESSAGE, delaySeconds)
        );
        return true;
    }

    // Called every server tick
    public static void tick(MinecraftServer server) {
        long now = nowMs();

        protectedUntilMs.values().removeIf(until -> until <= now);

        if (pendingTeleports.isEmpty()) return;

        boolean cancelOnMove = HomeLinkConfig.getCancelOnMove();
        List<PendingTeleport> due = new ArrayList<>();

        for (Iterator<PendingTeleport> it = pendingTeleports.values().iterator(); it.hasNext(); ) {
            PendingTeleport teleport = it.next();

            String problem = checkPlayers(server, teleport);
            if (problem != null) {
                it.remove();
                notifyBoth(server, teleport, problem);
                continue;
            }

            if (cancelOnMove && !teleport.player.blockPosition().equals(teleport.startPos)) {
                it.remove();
                notifyBoth(server, teleport, HomeLinkMessages.TELEPORT_CANCELLED_MOVEMENT);
                continue;
            }

            if (teleport.runAtMs <= now) {
                it.remove();
                due.add(teleport);
            }
        }

        // Run after the loop, so a teleport task can't change the map while we iterate it
        for (PendingTeleport teleport : due) {
            performTeleport(teleport);
        }
    }

    // Returns why the teleport can't happen (left, or died / respawned), or null if both players are fine
    private static String checkPlayers(MinecraftServer server, PendingTeleport teleport) {
        for (ServerPlayer player : new ServerPlayer[] { teleport.player, teleport.otherPlayer }) {
            if (player == null) continue;

            ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
            if (current == null) return HomeLinkMessages.TELEPORT_CANCELLED_DISCONNECT;

            // A respawned player is a new object; a dead one isn't alive
            if (current != player || !player.isAlive()) return HomeLinkMessages.TELEPORT_CANCELLED_DIED;
        }
        return null;
    }

    private static void performTeleport(PendingTeleport teleport) {
        ServerPlayer player = teleport.player;

        // This runs in the server tick: never let an error in a teleport crash the server
        try {
            BackStorage.save(player, player.blockPosition());

            grantProtection(player);

            teleport.teleportTask.run();

            @SuppressWarnings("resource") // player.level() is the world, never close it
            ServerLevel level = player.level();

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f
            );

            level.sendParticles(
                    ParticleTypes.PORTAL,
                    player.getX(),
                    player.getY() + 1,
                    player.getZ(),
                    32,
                    0.5, 0.5, 0.5,
                    0.2
            );
        } catch (Exception e) {
            HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.TELEPORT_FAILED, player.getName().getString(), e));
            HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.TELEPORT_FAILED_PRIVATE);
        }
    }

    // Short damage protection after arriving. Not given again until the previous one is
    // long over, so instant teleports can't be chained into permanent protection.
    private static void grantProtection(ServerPlayer player) {
        long durationMs = HomeLinkConfig.getInvulnerabilityTime() * 1000L;
        if (durationMs <= 0) return;

        long now = nowMs();
        Long lastStart = lastProtectionStartMs.get(player.getUUID());
        if (lastStart != null && now - lastStart < durationMs * 2) return;

        lastProtectionStartMs.put(player.getUUID(), now);
        protectedUntilMs.put(player.getUUID(), now + durationMs);
    }

    public static boolean isTeleporting(ServerPlayer player) {
        return player != null && pendingTeleports.containsKey(player.getUUID());
    }

    /**
     * Damage hook (ServerLivingEntityEvents.ALLOW_DAMAGE).
     * Any hit cancels a pending teleport. Returns false to block the damage while the
     * player is protected after arriving (void and /kill damage still go through).
     */
    public static boolean allowDamage(MinecraftServer server, ServerPlayer player, DamageSource source) {
        PendingTeleport pending = pendingTeleports.remove(player.getUUID());
        if (pending != null) {
            notifyBoth(server, pending, HomeLinkMessages.TELEPORT_CANCELLED_DAMAGED);
        }

        Long protectedUntil = protectedUntilMs.get(player.getUUID());
        return protectedUntil == null || protectedUntil <= nowMs()
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static void cancelPlayerTeleportOnCancel(ServerPlayer player) {
        if (pendingTeleports.remove(player.getUUID()) != null) {
            HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.TELEPORT_CANCELLED_CANCEL);
        } else {
            HomeLinkMessages.PrivateMessage(player, HomeLinkMessages.NO_PENDING_TELEPORT);
        }
    }

    // Player left: drop their teleport and protection, and cancel teleports that involve them
    public static void onDisconnect(UUID playerId) {
        PendingTeleport own = pendingTeleports.remove(playerId);
        if (own != null && HomeLinkMessages.isConnected(own.otherPlayer)) {
            HomeLinkMessages.PrivateMessage(own.otherPlayer, HomeLinkMessages.TELEPORT_CANCELLED_DISCONNECT);
        }
        protectedUntilMs.remove(playerId);
        lastProtectionStartMs.remove(playerId);

        for (Iterator<PendingTeleport> it = pendingTeleports.values().iterator(); it.hasNext(); ) {
            PendingTeleport teleport = it.next();
            if (teleport.otherPlayer != null && teleport.otherPlayer.getUUID().equals(playerId)) {
                it.remove();
                HomeLinkMessages.PrivateMessage(teleport.player, HomeLinkMessages.TELEPORT_CANCELLED_DISCONNECT);
            }
        }
    }

    // Server stopped: forget everything
    public static void clear() {
        pendingTeleports.clear();
        protectedUntilMs.clear();
        lastProtectionStartMs.clear();
    }

    // Tells both players (whoever is still online, using their current player object)
    private static void notifyBoth(MinecraftServer server, PendingTeleport teleport, String message) {
        for (ServerPlayer player : new ServerPlayer[] { teleport.player, teleport.otherPlayer }) {
            if (player == null) continue;

            ServerPlayer current = server != null ? server.getPlayerList().getPlayer(player.getUUID()) : player;
            if (HomeLinkMessages.isConnected(current)) {
                HomeLinkMessages.PrivateMessage(current, message);
            }
        }
    }

    // Monotonic clock (not affected by system clock changes)
    private static long nowMs() {
        return System.nanoTime() / 1_000_000L;
    }
}
