package io.github.ikiuox.pirouette;

import io.github.ikiuox.pirouette.config.PirouetteConfig;
import io.github.ikiuox.pirouette.gui.PirouetteConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Pirouette: smooth, configurable full-turn spins. Client-side only. */
public final class PirouetteClient implements ClientModInitializer {
    public static final String MOD_ID = "pirouette";
    public static final Logger LOGGER = LoggerFactory.getLogger("Pirouette");

    private static final String KEY_CATEGORY = "category.pirouette";
    private static final SpinDriver DRIVER = new SpinDriver();

    private static KeyBinding spinKey;
    private static KeyBinding turnKey;
    private static KeyBinding settingsKey;

    /** Shared spin runner (the settings screen uses it for the Test button). */
    public static SpinDriver driver() {
        return DRIVER;
    }

    @Override
    public void onInitializeClient() {
        PirouetteConfig.load();

        spinKey = registerKey("key.pirouette.spin", GLFW.GLFW_KEY_R);
        turnKey = registerKey("key.pirouette.turn", GLFW.GLFW_KEY_G);
        settingsKey = registerKey("key.pirouette.settings", GLFW.GLFW_KEY_RIGHT_SHIFT);

        // Once per rendered frame: this is what makes the rotation smooth.
        WorldRenderEvents.START.register(context -> onFrame(MinecraftClient.getInstance()));
        // Opening a screen is safest from the tick loop.
        ClientTickEvents.END_CLIENT_TICK.register(PirouetteClient::onTick);

        LOGGER.info("Pirouette ready");
    }

    private static KeyBinding registerKey(String translationKey, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(
            new KeyBinding(translationKey, InputUtil.Type.KEYSYM, defaultKey, KEY_CATEGORY));
    }

    private static void onFrame(MinecraftClient client) {
        // Always drain the key presses, even when we ignore them,
        // so nothing piles up while a menu is open.
        boolean spinPressed = false;
        boolean turnPressed = false;
        while (spinKey.wasPressed()) {
            spinPressed = true;
        }
        while (turnKey.wasPressed()) {
            turnPressed = true;
        }

        PirouetteConfig config = PirouetteConfig.get();
        if (spinPressed) {
            DRIVER.trigger(client, config.spin);
        }
        if (turnPressed) {
            DRIVER.trigger(client, config.turn);
        }
        DRIVER.onFrame(client);
    }

    private static void onTick(MinecraftClient client) {
        while (settingsKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new PirouetteConfigScreen(null));
            }
        }
    }
}
