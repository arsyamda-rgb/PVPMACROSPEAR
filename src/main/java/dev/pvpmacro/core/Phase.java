package dev.pvpmacro.core;

/**
 * Which half of a client tick a step runs in.
 * START = before vanilla input handling (key presses queued here are processed in the same tick).
 * END   = after vanilla input handling (e.g. swap back after the attack was processed).
 */
public enum Phase {
    START,
    END
}
