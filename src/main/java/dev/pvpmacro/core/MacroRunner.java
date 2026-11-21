package dev.pvpmacro.core;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tick-driven macro scheduler. No threads, no sleeping: everything runs on the client thread.
 * Call onTick(ctx, START) at the start of a client tick and onTick(ctx, END) at its end.
 * Only one macro runs at a time (prevents overlapping/duplicated input).
 */
public final class MacroRunner<C> {

    /** Safety valve against a macro that keeps producing zero-delay steps. */
    public static final int MAX_STEPS_PER_PASS = 32;

    private final ArrayDeque<MacroStep<C>> queue = new ArrayDeque<>();
    private final Consumer<String> errorSink;
    private Macro<C> current;
    private int wait;
    private int cycle;

    public MacroRunner(Consumer<String> errorSink) {
        this.errorSink = errorSink;
    }

    public boolean isRunning() {
        return current != null;
    }

    public String runningId() {
        return current == null ? null : current.id();
    }

    /** @return null if started, otherwise the reason it did not start. */
    public String start(C ctx, Macro<C> macro) {
        if (current != null) {
            return "another macro is running (" + current.id() + ")";
        }
        String err = macro.prepare(ctx);
        if (err != null) {
            return err;
        }
        current = macro;
        queue.clear();
        wait = 0;
        cycle = 0;
        refill(ctx);
        return null;
    }

    /** Abort the running macro and let it clean up (e.g. restore the hotbar slot). */
    public void cancel(C ctx) {
        if (current != null) {
            finish(ctx, true);
        }
    }

    /** Drop all state without cleanup (use when the world/player no longer exists). */
    public void reset() {
        current = null;
        queue.clear();
        wait = 0;
    }

    public void onTick(C ctx, Phase phase) {
        if (current == null) {
            return;
        }
        if (phase == Phase.START) {
            boolean stop;
            try {
                stop = current.isStopRequested(ctx);
            } catch (RuntimeException e) {
                fail(ctx, e);
                return;
            }
            if (stop) {
                finish(ctx, true);
                return;
            }
            if (wait > 0) {
                wait--;
            }
        }
        int guard = 0;
        while (current != null && !queue.isEmpty() && wait == 0
                && queue.peek().phase() == phase && guard++ < MAX_STEPS_PER_PASS) {
            MacroStep<C> step = queue.poll();
            try {
                step.action().run(ctx);
            } catch (RuntimeException e) {
                fail(ctx, e);
                return;
            }
            if (queue.isEmpty()) {
                refill(ctx);
            } else {
                wait = queue.peek().delayTicks();
            }
        }
    }

    private void refill(C ctx) {
        List<MacroStep<C>> next;
        try {
            next = current.nextCycle(ctx, cycle++);
        } catch (RuntimeException e) {
            fail(ctx, e);
            return;
        }
        if (next == null || next.isEmpty()) {
            finish(ctx, false);
            return;
        }
        queue.addAll(next);
        wait = queue.peek().delayTicks();
    }

    private void finish(C ctx, boolean cancelled) {
        Macro<C> m = current;
        current = null;
        queue.clear();
        wait = 0;
        try {
            m.onFinish(ctx, cancelled);
        } catch (RuntimeException e) {
            errorSink.accept("macro '" + m.id() + "' cleanup failed: " + e);
        }
    }

    private void fail(C ctx, RuntimeException e) {
        errorSink.accept("macro '" + current.id() + "' aborted: " + e);
        finish(ctx, true);
    }
}
