package com.sumutiu.homelink.storage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

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

    public static void save(ServerPlayer player, BlockPos pos) {
        lastPositions.put(player.getStringUUID(), new BackData(
                pos,
                player.level().dimension().identifier().toString(),
                player.getYRot(),
                player.getXRot()
        ));
    }

    public static BackData get(ServerPlayer player) {
        return lastPositions.get(player.getStringUUID());
    }
}