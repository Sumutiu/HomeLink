package com.sumutiu.homelink.storage;

import net.minecraft.util.math.BlockPos;

public class HomeData {
    public BlockPos position;
    public String world;
    public float yaw;
    public float pitch;

    public HomeData(BlockPos position, String world, float yaw, float pitch) {
        this.position = position;
        this.world = world;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}