package com.sumutiu.homelink.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HomeStorage {
    private static final Map<String, Map<String, HomeData>> homes = new HashMap<>();
    private static final File STORAGE_FOLDER = new File("mods/HomeLink");
    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<Map<String, HomeData>>() {}.getType();

    public static void loadPlayerHomes(ServerPlayerEntity player) {
        String uuid = player.getUuidAsString();
        File file = new File(STORAGE_FOLDER, uuid + ".json");

        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Map<String, HomeData> data = GSON.fromJson(reader, TYPE);
                if (data != null) {
                    homes.put(uuid, data);
                }
            } catch (IOException e) {
                HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.HOME_LOAD_FAILED, uuid, e));
            }
        } else {
            homes.put(uuid, new HashMap<>());
        }
    }

    public static void savePlayerHomes(ServerPlayerEntity player) {
        String uuid = player.getUuidAsString();
        File file = new File(STORAGE_FOLDER, uuid + ".json");

        try {
            if (!STORAGE_FOLDER.exists()) {
                if (STORAGE_FOLDER.mkdirs()) {
                    HomeLinkMessages.Logger(0, HomeLinkMessages.MAIN_FOLDER_CREATED);
                } else {
                    HomeLinkMessages.Logger(2, HomeLinkMessages.MAIN_FOLDER_CREATION_FAILED);
                }
            }

            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(homes.getOrDefault(uuid, new HashMap<>()), writer);
            }
        } catch (IOException e) {
            HomeLinkMessages.Logger(2, String.format(HomeLinkMessages.HOME_SAVE_FAILED, uuid, e));
        }
    }

    public static Map<String, HomeData> getAllHomes(ServerPlayerEntity player) {
        return homes.getOrDefault(player.getUuidAsString(), new HashMap<>());
    }

    public static boolean setHome(ServerPlayerEntity player, String name, BlockPos pos) {
        String uuid = player.getUuidAsString();
        Map<String, HomeData> playerHomes = homes.computeIfAbsent(uuid, k -> new HashMap<>());

        if (!playerHomes.containsKey(name) && playerHomes.size() >= HomeLinkConfig.getMaxHomes()) {
            return false;
        }

        HomeData data = new HomeData(
                pos,
                player.getWorld().getRegistryKey().getValue().toString(),
                player.getYaw(),
                player.getPitch()
        );

        playerHomes.put(name, data);
        savePlayerHomes(player);
        return true;
    }

    public static HomeData getHome(ServerPlayerEntity player, String name) {
        String uuid = player.getUuidAsString();
        return homes.getOrDefault(uuid, new HashMap<>()).get(name);
    }

    public static void deleteHome(ServerPlayerEntity player, String name) {
        String uuid = player.getUuidAsString();
        Map<String, HomeData> playerHomes = homes.get(uuid);
        if (playerHomes != null) {
            playerHomes.remove(name);
            savePlayerHomes(player);
        }
    }

    public static CompletableFuture<Suggestions> suggestHomeNames(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder) {

        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }

        String uuid = player.getUuidAsString();
        Map<String, HomeData> playerHomes = homes.get(uuid);
        if (playerHomes != null) {
            for (String name : playerHomes.keySet()) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }
}
