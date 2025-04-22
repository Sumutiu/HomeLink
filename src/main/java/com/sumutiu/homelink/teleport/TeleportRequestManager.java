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

import static com.sumutiu.homelink.HomeLink.LOGGER;

public class TeleportRequestManager {

    public enum RequestType {
        TO,
        HERE
    }

    public record TeleportRequest(UUID requesterId, RequestType type) {}

    private static final Map<UUID, TeleportRequest> activeRequests = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true); // Allows server shutdown
        thread.setName("HomeLink-TeleportRequestScheduler");
        return thread;
    });

    public static void sendRequest(ServerPlayerEntity requester, ServerPlayerEntity target, RequestType type) {
        UUID targetId = target.getUuid();
        UUID requesterId = requester.getUuid();

        activeRequests.put(targetId, new TeleportRequest(requesterId, type));

        int timeoutSeconds = HomeLinkConfig.getTeleportAcceptDelay();

        scheduler.schedule(() -> {
            if (activeRequests.remove(targetId) != null) {
                requester.sendMessage(
                        HomeLinkMessages.prefix("Teleport request to " + target.getName().getString() + " timed out."),
                        false
                );
                target.sendMessage(
                        HomeLinkMessages.prefix("Teleport request from " + requester.getName().getString() + " timed out."),
                        false
                );
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    public static boolean hasRequest(ServerPlayerEntity target) {
        return activeRequests.containsKey(target.getUuid());
    }

    public static TeleportRequest getRequest(ServerPlayerEntity target) {
        return activeRequests.get(target.getUuid());
    }

    public static void clearRequest(ServerPlayerEntity target) {
        activeRequests.remove(target.getUuid());
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
            LOGGER.info("[HomeLink]: TeleportRequestManager scheduler has been shut down.");
        } catch (Exception e) {
            LOGGER.error("[HomeLink]: Failed to shut down TeleportRequestManager scheduler: {}", e.getMessage());
        }
    }
}
