package io.github.ikiuox.pirouette.core;

/** What a spin key does when it is pressed while a spin is already running. */
public enum RepeatMode {
    IGNORE,
    RESTART,
    CANCEL;

    public RepeatMode next() {
        RepeatMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
