package eu.pb4.warps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Config-backed list of "warp admins" — a permission level above op.
 * <p>
 * Warp admins may view, warp to, and modify ANY private warp, even ones they don't own.
 * The list lives in {@code config/pbwarp-admins.json} and is edited by hand.
 */
public class WarpAdmins {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("pbwarp-admins.json");
    private static WarpAdmins instance = new WarpAdmins();

    private final Set<UUID> admins = new HashSet<>();

    public static WarpAdmins get() {
        return instance;
    }

    public static void load() {
        var loaded = new WarpAdmins();
        Config config = null;
        if (Files.exists(PATH)) {
            try {
                config = GSON.fromJson(Files.readString(PATH), Config.class);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            // No config yet: write a default one (seeded with the built-in admin) and use it.
            config = writeDefault();
        }

        if (config != null && config.admins != null) {
            for (var entry : config.admins) {
                if (entry == null || entry.uuid == null) {
                    continue;
                }
                try {
                    loaded.admins.add(UUID.fromString(entry.uuid));
                } catch (IllegalArgumentException e) {
                    System.err.println("[pbwarps] Skipping invalid UUID in pbwarp-admins.json: " + entry.uuid);
                }
            }
        }
        instance = loaded;
    }

    public boolean isAdmin(UUID uuid) {
        return uuid != null && this.admins.contains(uuid);
    }

    private static final String DEFAULT_ADMIN_UUID = ModInit.ALLOUTJAY_UUID.toString();

    private static Config writeDefault() {
        var config = new Config();
        config.admins = new ArrayList<>();

        var defaultAdmin = new Entry();
        defaultAdmin.uuid = DEFAULT_ADMIN_UUID;
        config.admins.add(defaultAdmin);

        try {
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    private static class Config {
        List<Entry> admins;
    }

    private static class Entry {
        String name;
        String uuid;
    }
}
