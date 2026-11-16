package io.github.ikiuox.pirouette.core;

import java.util.function.DoubleUnaryOperator;

/**
 * Motion curves for a spin. Every curve maps time 0..1 to progress 0..1,
 * starts at exactly 0 and ends at exactly 1, so a 360 degree spin always
 * lands on exactly 360 degrees.
 */
public enum Easing {
    /** Constant speed. */
    LINEAR(t -> t),
    /** Gentle start and stop. */
    SINE(t -> -(Math.cos(Math.PI * t) - 1.0) / 2.0),
    /** Accelerates, then brakes. Feels like a real wrist swing. */
    CUBIC(t -> t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0),
    /** Like CUBIC but with a sharper burst in the middle. */
    QUINT(t -> t < 0.5 ? 16.0 * t * t * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 5.0) / 2.0),
    /** Explodes out of the gate and glides to a stop. */
    FLICK(t -> 1.0 - Math.pow(1.0 - t, 4.0)),
    /** Swings slightly past the target, then settles back. */
    OVERSHOOT(t -> {
        double c1 = 1.70158;
        double c3 = c1 + 1.0;
        double u = t - 1.0;
        return 1.0 + c3 * u * u * u + c1 * u * u;
    });

    private final DoubleUnaryOperator curve;

    Easing(DoubleUnaryOperator curve) {
        this.curve = curve;
    }

    /** Progress for a time value; t is clamped to 0..1. */
    public double apply(double t) {
        if (t <= 0.0) {
            return 0.0;
        }
        if (t >= 1.0) {
            return 1.0;
        }
        return curve.applyAsDouble(t);
    }

    public Easing next() {
        Easing[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
