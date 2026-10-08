package com.sumutiu.homelink.teleport;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pending /tpto and /tphere requests, keyed by the target player.
 * Expired requests are removed on the server thread every tick (see HomeLink).
 */
public class TeleportRequestManager {

    public enum RequestType {
        TO,
        HERE
    }

    // Each request carries its own expiry time, so an old timeout can never remove a newer request
    public record TeleportRequest(UUID requesterId, RequestType type, long expiresAtMs) {}

    private static final Map<UUID, TeleportRequest> activeRequests = new ConcurrentHashMap<>();

    public static void sendRequest(ServerPlayer requester, ServerPlayer target, RequestType type) {
        long timeoutMs = HomeLinkConfig.getTeleportAcceptDelay() * 1000L;
        activeRequests.put(target.getUUID(), new TeleportRequest(requester.getUUID(), type, nowMs() + timeoutMs));
    }

    public static boolean hasRequest(ServerPlayer target) {
        return getRequest(target) != null;
    }

    // The target's pending request, or null if there is none (or it has expired)
    public static TeleportRequest getRequest(ServerPlayer target) {
        TeleportRequest request = activeRequests.get(target.getUUID());
        if (request == null || request.expiresAtMs() <= nowMs()) return null;
        return request;
    }

    // Removes only the target's incoming request (after accept / deny)
    public static void removeRequestFor(ServerPlayer target) {
        activeRequests.remove(target.getUUID());
    }

    // Player left: remove requests they sent and requests sent to them
    public static void clearRequest(UUID playerId) {
        activeRequests.remove(playerId);
        activeRequests.values().removeIf(request -> request.requesterId().equals(playerId));
    }

    // Called every server tick: tell both players when a request runs out
    public static void tick(MinecraftServer server) {
        if (activeRequests.isEmpty()) return;

        long now = nowMs();

        for (Iterator<Map.Entry<UUID, TeleportRequest>> it = activeRequests.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, TeleportRequest> entry = it.next();
            TeleportRequest request = entry.getValue();
            if (request.expiresAtMs() > now) continue;

            it.remove();

            ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId());
            if (target == null || requester == null) continue;

            String requesterName = requester.getName().getString();
            String targetName = target.getName().getString();

            HomeLinkMessages.PrivateMessage(requester,
                    String.format(HomeLinkMessages.TELEPORT_REQUEST_TO_TIMEOUT, targetName));
            HomeLinkMessages.PrivateMessage(target,
                    String.format(HomeLinkMessages.TELEPORT_REQUEST_FROM_TIMEOUT, requesterName));
            HomeLinkMessages.Logger(0,
                    String.format(HomeLinkMessages.LOG_TELEPORT_TIMEOUT, requesterName, targetName));
        }
    }

    public static CompletableFuture<Suggestions> suggestPendingRequestNames(MinecraftServer server, ServerPlayer target, SuggestionsBuilder builder) {
        TeleportRequest request = getRequest(target);

        if (request != null) {
            ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId());

            if (requester != null) {
                builder.suggest(requester.getName().getString());
            }
        }

        return builder.buildFuture();
    }

    // Server stopped: forget everything
    public static void clear() {
        activeRequests.clear();
    }

    // Monotonic clock (not affected by system clock changes)
    private static long nowMs() {
        return System.nanoTime() / 1_000_000L;
    }
}
