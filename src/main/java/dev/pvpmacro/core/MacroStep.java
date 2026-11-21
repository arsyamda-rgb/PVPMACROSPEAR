package dev.pvpmacro.core;

import java.util.Objects;

/**
 * One scheduled action.
 *
 * @param phase      tick half in which the action runs
 * @param delayTicks ticks to wait after the previous step ran (0 = same tick, if the phase allows)
 */
public record MacroStep<C>(Phase phase, int delayTicks, String label, StepAction<C> action) {
    public MacroStep {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
        if (delayTicks < 0) {
            throw new IllegalArgumentException("delayTicks < 0");
        }
    }
}
