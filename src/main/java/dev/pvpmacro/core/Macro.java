package dev.pvpmacro.core;

import java.util.List;

/**
 * A macro is a small state machine that hands the runner batches of steps.
 * It is single-use: create a new instance per run.
 */
public interface Macro<C> {

    String id();

    /** Validate and capture state before running. Return null if OK, otherwise a short reason. */
    String prepare(C ctx);

    /**
     * Next batch of steps. Called once after prepare() and again every time the previous
     * batch has fully executed. Return an empty list to finish. The macro can inspect live
     * state here (key still held? spear still there?) to decide whether to continue.
     */
    List<MacroStep<C>> nextCycle(C ctx, int cycleIndex);

    /** Polled once per tick (START). Returning true aborts the macro (finish with cancelled=true). */
    default boolean isStopRequested(C ctx) {
        return false;
    }

    /** Always called exactly once when the macro ends. cancelled=true on abort/guard/error. */
    default void onFinish(C ctx, boolean cancelled) {
    }
}
