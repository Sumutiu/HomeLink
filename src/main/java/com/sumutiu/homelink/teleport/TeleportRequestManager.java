package com.sumutiu.homelink.teleport;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class TeleportRequestManager {

    public enum RequestType {
        TO,
        HERE
    }

    public record TeleportRequest(UUID requesterId, RequestType type) {}

    public static final Map<UUID, TeleportRequest> activeRequests = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, runnable -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                thread.setName(HomeLinkMessages.Mod_ID + " - " + HomeLinkMessages.MANAGER_SERVICE_NAME);
                return thread;
            });

    public static void sendRequest(ServerPlayer requester, ServerPlayer target, RequestType type) {
        UUID targetId = target.getUUID();
        UUID requesterId = requester.getUUID();

        activeRequests.put(targetId, new TeleportRequest(requesterId, type));

        int timeoutSeconds = HomeLinkConfig.getTeleportAcceptDelay();

        scheduler.schedule(() -> {
            if (activeRequests.remove(targetId) != null) {
                HomeLinkMessages.PrivateMessage(requester,
                        String.format(HomeLinkMessages.TELEPORT_REQUEST_TO_TIMEOUT, target.getName().getString()));

                HomeLinkMessages.PrivateMessage(target,
                        String.format(HomeLinkMessages.TELEPORT_REQUEST_FROM_TIMEOUT, requester.getName().getString()));

                HomeLinkMessages.Logger(0,
                        String.format(HomeLinkMessages.LOG_TELEPORT_TIMEOUT,
                                requester.getName().getString(),
                                target.getName().getString()));
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    public static boolean hasRequest(ServerPlayer target) {
        return activeRequests.containsKey(target.getUUID());
    }

    public static TeleportRequest getRequest(ServerPlayer target) {
        return activeRequests.get(target.getUUID());
    }

    public static void clearRequest(UUID playerId) {
        // Remove if player is the target
        activeRequests.remove(playerId);

        // Remove if player is requester
        activeRequests.entrySet().removeIf(entry ->
                entry.getValue().requesterId().equals(playerId));
    }

    public static CompletableFuture<Suggestions> suggestPendingRequestNames(ServerPlayer target, SuggestionsBuilder builder) {
        TeleportRequest request = activeRequests.get(target.getUUID());

        if (request != null) {
            ServerLevel level = target.level();

            MinecraftServer server = level.getServer();

            ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId());

            if (requester != null) {
                builder.suggest(requester.getName().getString());
            }
        }

        return builder.buildFuture();
    }

    public static void shutdown() {
        try {
            scheduler.shutdownNow();
            HomeLinkMessages.Logger(0, HomeLinkMessages.TELEPORT_REQUEST_MANAGER_SHUTDOWN);
        } catch (Exception e) {
            HomeLinkMessages.Logger(2,
                    String.format(HomeLinkMessages.TELEPORT_REQUEST_MANAGER_SHUTDOWN_FAILED, e.getMessage()));
        }
    }
}