package com.sumutiu.homelink.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sumutiu.homelink.config.HomeLinkConfig;
import com.sumutiu.homelink.util.HomeLinkMessages;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.sumutiu.homelink.HomeLink.*;

public class HomeStorage {

    private static final Map<String, Map<String, HomeData>> homes = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<Map<String, HomeData>>() {}.getType();

    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> loadPlayerHomes(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> savePlayerHomes(handler.getPlayer()));
    }

    public static void loadPlayerHomes(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        File file = new File(STORAGE_FOLDER.toFile(), uuid + ".json");

        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Map<String, HomeData> data = GSON.fromJson(reader, TYPE);
                if (data != null) {
                    homes.put(uuid, data);
                }
            } catch (IOException e) {
                HomeLinkMessages.Logger(2,
                        String.format(HomeLinkMessages.HOME_LOAD_FAILED, uuid, e));
            }
        } else {
            homes.put(uuid, new HashMap<>());
        }
    }

    public static void savePlayerHomes(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        File file = new File(STORAGE_FOLDER.toFile(), uuid + ".json");

        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(homes.getOrDefault(uuid, new HashMap<>()), writer);
        } catch (IOException e) {
            HomeLinkMessages.Logger(2,
                    String.format(HomeLinkMessages.HOME_SAVE_FAILED, uuid, e));
        }
    }

    public static Map<String, HomeData> getAllHomes(ServerPlayer player) {
        return homes.getOrDefault(player.getUUID().toString(), new HashMap<>());
    }

    public static boolean setHome(ServerPlayer player, String name, BlockPos pos) {
        String uuid = player.getUUID().toString();
        Map<String, HomeData> playerHomes = homes.computeIfAbsent(uuid, _ -> new HashMap<>());

        if (!playerHomes.containsKey(name)
                && playerHomes.size() >= HomeLinkConfig.getMaxHomes()) {
            return false;
        }

        // Mojang 26.1 world key handling
        String dimensionId = player.level().dimension().identifier().toString();

        HomeData data = new HomeData(
                pos,
                dimensionId,
                player.getYRot(),
                player.getXRot()
        );

        playerHomes.put(name, data);
        savePlayerHomes(player);
        return true;
    }

    public static HomeData getHome(ServerPlayer player, String name) {
        String uuid = player.getUUID().toString();
        return homes.getOrDefault(uuid, new HashMap<>()).get(name);
    }

    public static void deleteHome(ServerPlayer player, String name) {
        String uuid = player.getUUID().toString();
        Map<String, HomeData> playerHomes = homes.get(uuid);

        if (playerHomes != null) {
            playerHomes.remove(name);
            savePlayerHomes(player);
        }
    }

    public static CompletableFuture<Suggestions> suggestHomeNames(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {

        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }

        String uuid = player.getUUID().toString();
        Map<String, HomeData> playerHomes = homes.get(uuid);

        if (playerHomes != null) {
            for (String name : playerHomes.keySet()) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }
}