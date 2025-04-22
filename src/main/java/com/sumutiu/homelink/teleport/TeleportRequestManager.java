package com.sumutiu.homelink.teleport;

import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.*;

import static com.sumutiu.homelink.HomeLink.LOGGER;

public class TeleportRequestManager {

    public enum RequestType {
        TO, HERE
    }

    public record TeleportRequest(UUID requesterId, RequestType type) { }

    private static final Map<UUID, TeleportRequest> pendingRequests = new HashMap<>();
    private static final Map<UUID, ScheduledFuture<?>> expirationTasks = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("HomeLink-TeleportRequestScheduler");
        return thread;
    });

    public static void sendRequest(ServerPlayerEntity requester, ServerPlayerEntity target, RequestType type) {
        UUID targetId = target.getUuid();
        UUID requesterId = requester.getUuid();

        pendingRequests.put(targetId, new TeleportRequest(requesterId, type));

        // Cancel existing expiration task for the target (if any)
        if (expirationTasks.containsKey(targetId)) {
            expirationTasks.get(targetId).cancel(false);
        }

        int timeout = HomeLinkConfig.getTeleportAcceptDelay();

        // Schedule expiration
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            pendingRequests.remove(targetId);
            expirationTasks.remove(targetId);
            target.sendMessage(HomeLinkMessages.prefix("Teleport request from " + requester.getName().getString() + " expired."), false);
            requester.sendMessage(HomeLinkMessages.prefix("Your teleport request to " + target.getName().getString() + " expired."), false);
        }, timeout, TimeUnit.SECONDS);

        expirationTasks.put(targetId, task);
    }

    public static TeleportRequest getRequest(ServerPlayerEntity target) {
        return pendingRequests.get(target.getUuid());
    }

    public static void clearRequest(ServerPlayerEntity target) {
        UUID targetId = target.getUuid();
        pendingRequests.remove(targetId);
        if (expirationTasks.containsKey(targetId)) {
            expirationTasks.get(targetId).cancel(false);
            expirationTasks.remove(targetId);
        }
    }

    public static boolean hasRequest(ServerPlayerEntity target) {
        return pendingRequests.containsKey(target.getUuid());
    }

    public static void shutdown() {
        try {
            scheduler.shutdownNow();
            LOGGER.info("[HomeLink]: TeleportRequestManager has been shut down.");
        } catch (Exception e) {
            LOGGER.error("[HomeLink]: Failed to shut down TeleportRequestManager: {}", e.getMessage());
        }
    }
}
