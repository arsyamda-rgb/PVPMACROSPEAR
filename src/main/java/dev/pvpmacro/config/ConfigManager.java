package dev.pvpmacro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.pvpmacro.PvpMacroClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Loads/saves config/pvpmacro.json. Invalid files are kept as .bad and replaced by defaults. */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PvpMacroConfig config = new PvpMacroConfig();

    private ConfigManager() {
    }

    public static PvpMacroConfig get() {
        return config;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("pvpmacro.json");
    }

    /** @return null on success, or a warning message to show the user. */
    public static synchronized String load() {
        Path p = path();
        String warning = null;
        PvpMacroConfig loaded = null;
        if (Files.exists(p)) {
            try {
                loaded = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), PvpMacroConfig.class);
            } catch (JsonParseException | IOException e) {
                PvpMacroClient.LOG.error("Could not read {}: {}", p, e.toString());
                try {
                    Files.move(p, p.resolveSibling("pvpmacro.json.bad"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // keep going with defaults
                }
                warning = "config was invalid, saved as pvpmacro.json.bad and reset to defaults";
            }
        }
        if (loaded == null) {
            loaded = new PvpMacroConfig();
        }
        loaded.sanitize();
        config = loaded;
        save(); // writes defaults / newly added fields back
        return warning;
    }

    public static synchronized void save() {
        Path p = path();
        Path tmp = p.resolveSibling("pvpmacro.json.tmp");
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(tmp, GSON.toJson(config), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            PvpMacroClient.LOG.error("Could not save {}: {}", p, e.toString());
        }
    }
}
