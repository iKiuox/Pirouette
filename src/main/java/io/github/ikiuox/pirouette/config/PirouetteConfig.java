package io.github.ikiuox.pirouette.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.ikiuox.pirouette.PirouetteClient;
import io.github.ikiuox.pirouette.core.Direction;
import io.github.ikiuox.pirouette.core.Easing;
import io.github.ikiuox.pirouette.core.RepeatMode;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

/** Settings, stored in config/pirouette.json. */
public final class PirouetteConfig {
    public static final int MAX_COOLDOWN_MS = 10000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PirouetteConfig instance = new PirouetteConfig();

    /** Master switch for every Pirouette key. */
    public boolean enabled = true;
    /** What a spin key does while a spin is already running. */
    public RepeatMode repeatMode = RepeatMode.IGNORE;
    /** Stop the spin as soon as the player moves the mouse. */
    public boolean cancelOnMouseMove = false;
    /** Minimum pause after a spin before the next one may start. */
    public int cooldownMs = 0;
    /** Profile for the Spin key (full turn). */
    public SpinProfile spin = new SpinProfile(360.0f, 300, Easing.CUBIC, Direction.RIGHT);
    /** Profile for the Quick turn key. */
    public SpinProfile turn = new SpinProfile(180.0f, 160, Easing.CUBIC, Direction.RIGHT);

    public static PirouetteConfig get() {
        return instance;
    }

    /** A fresh copy of the factory settings. */
    public static PirouetteConfig defaults() {
        return new PirouetteConfig();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pirouette.json");
    }

    public static void load() {
        Path file = file();
        PirouetteConfig loaded = null;
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, PirouetteConfig.class);
            } catch (IOException | RuntimeException e) {
                PirouetteClient.LOGGER.warn("Could not read pirouette.json, using defaults (old file kept as pirouette.json.broken)", e);
                keepBrokenCopy(file);
            }
        }
        instance = loaded != null ? loaded : new PirouetteConfig();
        instance.sanitize();
        save();
    }

    /** A file we cannot parse is moved aside instead of being silently overwritten. */
    private static void keepBrokenCopy(Path file) {
        try {
            Files.move(file, file.resolveSibling("pirouette.json.broken"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // nothing more we can do; the defaults will overwrite the file
        }
    }

    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            PirouetteClient.LOGGER.warn("Could not save pirouette.json", e);
        }
    }

    /** Repairs missing or out-of-range values so a hand-edited file can never break the game. */
    private void sanitize() {
        PirouetteConfig fallback = new PirouetteConfig();
        if (repeatMode == null) {
            repeatMode = fallback.repeatMode;
        }
        cooldownMs = Math.max(0, Math.min(MAX_COOLDOWN_MS, cooldownMs));
        if (spin == null) {
            spin = fallback.spin;
        }
        if (turn == null) {
            turn = fallback.turn;
        }
        spin.sanitize(fallback.spin);
        turn.sanitize(fallback.turn);
    }
}
