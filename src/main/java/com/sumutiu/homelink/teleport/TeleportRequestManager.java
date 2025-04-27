package com.sumutiu.homelink.teleport;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

// This handles request lifetimes.

public class TeleportRequestManager {

    public enum RequestType {
        TO,
        HERE
    }

    public record TeleportRequest(UUID requesterId, RequestType type) { }

    public static final Map<UUID, TeleportRequest> activeRequests = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true); // Allows server shutdown
        thread.setName(HomeLinkMessages.Mod_ID + " - " + HomeLinkMessages.MANAGER_SERVICE_NAME);
        return thread;
    });

    public static void sendRequest(ServerPlayerEntity requester, ServerPlayerEntity target, RequestType type) {
        UUID targetId = target.getUuid();
        UUID requesterId = requester.getUuid();

        activeRequests.put(targetId, new TeleportRequest(requesterId, type));

        int timeoutSeconds = HomeLinkConfig.getTeleportAcceptDelay();

        scheduler.schedule(() -> {
            if (activeRequests.remove(targetId) != null) {
                HomeLinkMessages.PrivateMessage(requester, String.format(HomeLinkMessages.TELEPORT_REQUEST_TO_TIMEOUT, target.getName().getString()));
                HomeLinkMessages.PrivateMessage(target, String.format(HomeLinkMessages.TELEPORT_REQUEST_FROM_TIMEOUT, requester.getName().getString()));
                HomeLinkMessages.Logger(0, String.format(HomeLinkMessages.LOG_TELEPORT_TIMEOUT, requester.getName().getString(), target.getName().getString()));
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    public static boolean hasRequest(ServerPlayerEntity target) {
        return activeRequests.containsKey(target.getUuid());
    }

    public static TeleportRequest getRequest(ServerPlayerEntity target) {
        return activeRequests.get(target.getUuid());
    }

    public static void clearRequest(UUID playerId) {
        activeRequests.remove(playerId); // If player is the target
        activeRequests.entrySet().removeIf(entry -> entry.getValue().requesterId().equals(playerId)); // If player is the requester
    }

    public static CompletableFuture<Suggestions> suggestPendingRequestNames(ServerPlayerEntity target, SuggestionsBuilder builder) {
        TeleportRequest request = activeRequests.get(target.getUuid());
        if (request != null) {
            MinecraftServer server = target.getServer();
            if (server != null) {
                ServerPlayerEntity requester = server.getPlayerManager().getPlayer(request.requesterId());
                if (requester != null) {
                    builder.suggest(requester.getName().getString());
                }
            }
        }
        return builder.buildFuture();
    }

    public static void shutdown() {
        try {
            scheduler.shutdownNow();
            HomeLinkMessages.Logger(0, HomeLinkMessages.TELEPORT_REQUEST_MANAGER_SHUTDOWN);
        } catch (Exception e) {
            HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.TELEPORT_REQUEST_MANAGER_SHUTDOWN_FAILED, e.getMessage()));
        }
    }
}
