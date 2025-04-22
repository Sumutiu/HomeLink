package com.sumutiu.homelink.util;

import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.storage.BackStorage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static com.sumutiu.homelink.HomeLink.LOGGER;

public class TeleportScheduler {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("HomeLink-TeleportScheduler");
        return thread;
    });
    private static final Map<UUID, BlockPos> teleportPositions = new ConcurrentHashMap<>();
    private static final Set<UUID> activeTeleports = ConcurrentHashMap.newKeySet();

    public static void schedule(ServerPlayerEntity player, int delaySeconds, Runnable teleportTask) {
        UUID uuid = player.getUuid();

        if (isTeleporting(player)) {
            player.sendMessage(HomeLinkMessages.prefix("You already have a teleport in progress."), false);
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

        player.sendMessage(HomeLinkMessages.prefix("Teleporting in " + delaySeconds + " seconds..."), false);

        scheduler.schedule(() -> {
            boolean cancelOnMove = HomeLinkConfig.getCancelOnMove();
            BlockPos currentPos = player.getBlockPos();

            if (cancelOnMove && !currentPos.equals(teleportPositions.get(uuid))) {
                player.sendMessage(HomeLinkMessages.prefix("Teleport cancelled due to movement."), false);
            } else {
                BackStorage.save(player, player.getBlockPos());
                teleportTask.run();
            }

            teleportPositions.remove(uuid);
            activeTeleports.remove(uuid);

        }, delaySeconds, TimeUnit.SECONDS);
    }

    public static boolean isTeleporting(ServerPlayerEntity player) {
        return activeTeleports.contains(player.getUuid());
    }

    public static void shutdown() {
        try {
            scheduler.shutdownNow();
            LOGGER.info("[HomeLink]: TeleportScheduler has been shut down.");
        } catch (Exception e) {
            LOGGER.error("[HomeLink]: Failed to shut down TeleportScheduler: {}", e.getMessage());
        }
    }
}
