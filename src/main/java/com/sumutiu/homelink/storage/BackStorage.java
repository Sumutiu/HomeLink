package com.sumutiu.homelink.storage;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class BackStorage {

    public static class BackData {
        public BlockPos pos;
        public String world;
        public float yaw;
        public float pitch;

        public BackData(BlockPos pos, String world, float yaw, float pitch) {
            this.pos = pos;
            this.world = world;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static final Map<String, BackData> lastPositions = new HashMap<>();

    public static void save(ServerPlayerEntity player, BlockPos pos) {
        lastPositions.put(player.getUuidAsString(), new BackData(
                pos,
                player.getWorld().getRegistryKey().getValue().toString(),
                player.getYaw(),
                player.getPitch()
        ));
    }

    public static BackData get(ServerPlayerEntity player) {
        return lastPositions.get(player.getUuidAsString());
    }
}