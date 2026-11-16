package io.github.ikiuox.pirouette;

import io.github.ikiuox.pirouette.config.PirouetteConfig;
import io.github.ikiuox.pirouette.config.SpinProfile;
import io.github.ikiuox.pirouette.core.SpinAnimation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Runs the active spin. {@link #onFrame} is called once per rendered frame,
 * so the camera moves every frame instead of only 20 times per second.
 * Rotation goes through the same call the mouse uses, so it behaves exactly
 * like physically swinging the mouse (vehicles, other mods and the server
 * all see an ordinary rotation).
 */
public final class SpinDriver {
    /** Vanilla turns 0.15 degrees per mouse unit (see Entity#changeLookDirection). */
    private static final double DEGREES_PER_MOUSE_UNIT = 0.15;
    /** With "cancel on mouse move": total outside rotation tolerated before the spin stops. */
    private static final double EXTERNAL_CANCEL_DEGREES = 5.0;

    private SpinAnimation animation;
    private float lastYaw;
    private double externalDegrees;
    private boolean alternateNextRight = true;
    private long cooldownUntilNanos;

    public boolean isSpinning() {
        return animation != null;
    }

    public void cancel() {
        animation = null;
    }

    /**
     * Starts a spin if the situation allows it.
     *
     * @return true if the key press was used (spin started or cancelled)
     */
    public boolean trigger(MinecraftClient client, SpinProfile profile) {
        PirouetteConfig config = PirouetteConfig.get();
        ClientPlayerEntity player = client.player;
        if (!config.enabled || player == null || client.currentScreen != null || player.isDead()) {
            return false;
        }

        long now = System.nanoTime();
        if (animation != null) {
            switch (config.repeatMode) {
                case IGNORE -> {
                    return false;
                }
                case CANCEL -> {
                    cancel();
                    return true;
                }
                case RESTART -> {
                    // fall out of the switch and begin a fresh spin from here
                }
            }
        } else if (now < cooldownUntilNanos) {
            return false;
        }

        double sign = switch (profile.direction) {
            case RIGHT -> 1.0;
            case LEFT -> -1.0;
            case ALTERNATE -> {
                double next = alternateNextRight ? 1.0 : -1.0;
                alternateNextRight = !alternateNextRight;
                yield next;
            }
        };

        long durationNanos = (long) profile.durationMs * 1_000_000L;
        animation = new SpinAnimation(sign * profile.angle, durationNanos, profile.easing, now);
        lastYaw = player.getYaw();
        externalDegrees = 0.0;
        return true;
    }

    /** Called once per rendered frame. */
    public void onFrame(MinecraftClient client) {
        if (animation == null) {
            return;
        }
        PirouetteConfig config = PirouetteConfig.get();
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null || player.isDead() || !config.enabled) {
            animation = null;
            return;
        }

        if (config.cancelOnMouseMove) {
            externalDegrees += Math.abs(wrapDegrees(player.getYaw() - lastYaw));
            if (externalDegrees > EXTERNAL_CANCEL_DEGREES) {
                animation = null;
                return;
            }
        }

        double delta = animation.step(System.nanoTime());
        if (delta != 0.0) {
            player.changeLookDirection(delta / DEGREES_PER_MOUSE_UNIT, 0.0);
        }
        lastYaw = player.getYaw();

        if (animation.isFinished()) {
            animation = null;
            cooldownUntilNanos = System.nanoTime() + (long) config.cooldownMs * 1_000_000L;
        }
    }

    /** Maps any angle into -180..180. */
    private static float wrapDegrees(float degrees) {
        float f = degrees % 360.0F;
        if (f >= 180.0F) {
            f -= 360.0F;
        }
        if (f < -180.0F) {
            f += 360.0F;
        }
        return f;
    }
}
