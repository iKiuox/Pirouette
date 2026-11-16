package io.github.ikiuox.pirouette.core;

/** Which way a spin turns. */
public enum Direction {
    RIGHT,
    LEFT,
    ALTERNATE;

    public Direction next() {
        Direction[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
