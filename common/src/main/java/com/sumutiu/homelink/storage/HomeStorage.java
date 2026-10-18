package com.sumutiu.homelink.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sumutiu.homelink.config.HomeLinkConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.sumutiu.homelink.HomeLink.*;
import static com.sumutiu.homelink.util.HomeLinkMessages.*;

/**
 * Homes, one JSON file per player. A player's homes are loaded when they join,
 * saved on every change, and saved and unloaded when they leave.
 */
public class HomeStorage {

    public enum SetHomeResult {
        SET,
        LIMIT_REACHED,
        UNAVAILABLE
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, HomeData>>() {}.getType();

    // Homes of the players currently online
    private static final Map<UUID, Map<String, HomeData>> homes = new ConcurrentHashMap<>();

    private static File getFile(UUID uuid) {
        return new File(STORAGE_FOLDER.toFile(), uuid + ".json");
    }

    // ----------------------------
    // Load / save
    // ----------------------------
    public static void loadPlayerHomes(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Map<String, HomeData> loaded = tryLoad(uuid);
        if (loaded != null) {
            homes.put(uuid, loaded);
        }
        // If it couldn't be read, nothing is stored, so the file is never overwritten;
        // getHomes() tries again the next time the player uses a home command
    }

    // Returns null if the file exists but can't be read right now
    private static Map<String, HomeData> tryLoad(UUID uuid) {
        File file = getFile(uuid);
        if (!file.exists()) return new HashMap<>();

        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Map<String, HomeData> data = GSON.fromJson(reader, TYPE);
            Map<String, HomeData> result = new HashMap<>();
            if (data != null) {
                // Skip broken entries instead of failing later
                data.forEach((name, home) -> {
                    if (name != null && home != null && home.position != null && home.world != null) {
                        result.put(name, home);
                    }
                });
            }
            return result;
        } catch (JsonParseException e) {
            Logger(2, String.format(HOME_LOAD_FAILED, uuid, e.getMessage()));
            if (isTemporaryReadError(e)) return null;

            // Corrupt file: move it aside (keeps the data for an admin) and start fresh
            return quarantineCorruptFile(file) ? new HashMap<>() : null;
        } catch (IOException e) {
            Logger(2, String.format(HOME_LOAD_FAILED, uuid, e.getMessage()));
            return null;
        }
    }

    private static void savePlayerHomes(UUID uuid) {
        Map<String, HomeData> playerHomes = homes.get(uuid);
        if (playerHomes == null) return; // not loaded: nothing to save, and never overwrite

        File file = getFile(uuid);
        Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");

        try {
            // Serialize to a String first: Gson wraps write errors in an unchecked JsonIOException
            Files.writeString(tmp, GSON.toJson(playerHomes), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Logger(2, String.format(HOME_SAVE_FAILED, uuid, e.getMessage()));
        }
    }

    // Player left: save and forget their homes
    public static void saveAndUnload(ServerPlayer player) {
        UUID uuid = player.getUUID();
        savePlayerHomes(uuid);
        homes.remove(uuid);
    }

    // Server stopped: save anything still loaded and forget everything
    public static void saveAllAndClear() {
        for (UUID uuid : new ArrayList<>(homes.keySet())) {
            savePlayerHomes(uuid);
        }
        homes.clear();
    }

    // ----------------------------
    // Public API
    // ----------------------------

    // The player's homes, or null if their file can't be read right now
    public static Map<String, HomeData> getHomes(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Map<String, HomeData> playerHomes = homes.get(uuid);
        if (playerHomes != null) return playerHomes;

        Map<String, HomeData> loaded = tryLoad(uuid);
        if (loaded != null) homes.put(uuid, loaded);
        return loaded;
    }

    public static SetHomeResult setHome(ServerPlayer player, String name, BlockPos pos) {
        Map<String, HomeData> playerHomes = getHomes(player);
        if (playerHomes == null) return SetHomeResult.UNAVAILABLE;

        if (!playerHomes.containsKey(name)
                && playerHomes.size() >= HomeLinkConfig.getMaxHomes()) {
            return SetHomeResult.LIMIT_REACHED;
        }

        // Mojang 26.1 world key handling
        @SuppressWarnings("resource") // player.level() is the world, never close it
        String dimensionId = player.level().dimension().identifier().toString();

        HomeData data = new HomeData(
                pos,
                dimensionId,
                player.getYRot(),
                player.getXRot()
        );

        playerHomes.put(name, data);
        savePlayerHomes(player.getUUID());
        return SetHomeResult.SET;
    }

    // Returns false if the player has no home with that name
    public static boolean deleteHome(ServerPlayer player, String name) {
        Map<String, HomeData> playerHomes = getHomes(player);
        if (playerHomes == null || playerHomes.remove(name) == null) return false;

        savePlayerHomes(player.getUUID());
        return true;
    }

    public static CompletableFuture<Suggestions> suggestHomeNames(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {

        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }

        Map<String, HomeData> playerHomes = homes.get(player.getUUID());

        if (playerHomes != null) {
            for (String name : playerHomes.keySet()) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    // Only bad or cut-off JSON (a JsonSyntaxException) means the file itself is corrupt.
    // Gson also reports I/O errors that way, and other JsonParseExceptions are not about the file.
    private static boolean isTemporaryReadError(JsonParseException e) {
        if (!(e instanceof JsonSyntaxException)) return true;

        Throwable cause = e.getCause();
        return cause instanceof IOException
                && !(cause instanceof MalformedJsonException)
                && !(cause instanceof EOFException);
    }

    // Returns false if the file could not be moved; it must then be treated as unreadable
    private static boolean quarantineCorruptFile(File file) {
        File backup = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
        try {
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Logger(1, String.format(HOME_FILE_CORRUPT, file.getName(), backup.getName()));
            return true;
        } catch (IOException e) {
            Logger(2, String.format(HOME_FILE_CORRUPT_MOVE_FAILED, file.getName(), e.getMessage()));
            return false;
        }
    }
}
