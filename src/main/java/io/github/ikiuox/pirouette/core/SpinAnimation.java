package io.github.ikiuox.pirouette.core;

/**
 * A single spin, driven by wall-clock time instead of ticks or frame counts.
 * That is what keeps it smooth at any frame rate: each frame asks "where
 * should I be right now?" and rotates by the difference.
 */
public final class SpinAnimation {
    private final double totalDegrees;
    private final long startNanos;
    private final long durationNanos;
    private final Easing easing;

    private double applied;
    private boolean finished;

    /**
     * @param totalDegrees signed angle to turn (positive = right)
     * @param durationNanos how long the whole spin takes
     * @param easing motion curve
     * @param startNanos clock value when the spin begins
     */
    public SpinAnimation(double totalDegrees, long durationNanos, Easing easing, long startNanos) {
        this.totalDegrees = totalDegrees;
        this.durationNanos = Math.max(1L, durationNanos);
        this.easing = easing;
        this.startNanos = startNanos;
    }

    /**
     * Returns how many degrees to rotate right now, i.e. the progress made
     * since the previous call. After the last step {@link #isFinished()} is true.
     */
    public double step(long nowNanos) {
        if (finished) {
            return 0.0;
        }
        double t = (double) (nowNanos - startNanos) / (double) durationNanos;
        if (t >= 1.0) {
            t = 1.0;
            finished = true;
        }
        double target = totalDegrees * easing.apply(t);
        double delta = target - applied;
        applied = target;
        return delta;
    }

    public boolean isFinished() {
        return finished;
    }

    /** Total degrees applied so far. */
    public double applied() {
        return applied;
    }
}
