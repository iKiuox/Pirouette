package io.github.ikiuox.pirouette.config;

import io.github.ikiuox.pirouette.core.Direction;
import io.github.ikiuox.pirouette.core.Easing;

/** One configurable spin: how far, how fast, and with which motion curve. */
public final class SpinProfile {
    public static final float MIN_ANGLE = 1.0f;
    public static final float MAX_ANGLE = 7200.0f;
    public static final int MIN_DURATION_MS = 20;
    public static final int MAX_DURATION_MS = 10000;

    /** Degrees to turn. Always positive; {@link #direction} picks left or right. */
    public float angle;
    /** How long the spin takes, in milliseconds. */
    public int durationMs;
    public Easing easing;
    public Direction direction;

    public SpinProfile() {
        this(360.0f, 300, Easing.CUBIC, Direction.RIGHT);
    }

    public SpinProfile(float angle, int durationMs, Easing easing, Direction direction) {
        this.angle = angle;
        this.durationMs = durationMs;
        this.easing = easing;
        this.direction = direction;
    }

    /** Repairs anything missing or out of range after the file is loaded. */
    void sanitize(SpinProfile fallback) {
        if (Float.isNaN(angle)) {
            angle = fallback.angle;
        }
        angle = Math.max(MIN_ANGLE, Math.min(MAX_ANGLE, angle));
        durationMs = Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, durationMs));
        if (easing == null) {
            easing = fallback.easing;
        }
        if (direction == null) {
            direction = fallback.direction;
        }
    }
}
