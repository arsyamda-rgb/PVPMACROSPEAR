package dev.pvpmacro.macro.spear;

import dev.pvpmacro.config.AttributeSwapConfig;
import dev.pvpmacro.core.Macro;
import dev.pvpmacro.core.MacroEnvironment;
import dev.pvpmacro.core.MacroStep;
import dev.pvpmacro.core.Phase;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

/**
 * Spear "attribute swap" / lunge swap.
 *
 * Idea (UNVERIFIED in-game, see README): attributes are refreshed from equipment once per server tick.
 * A jab done in the same tick as the switch to the spear is judged with the attack speed of the item
 * held BEFORE it. So the player is parked on a swap item with no attack-speed penalty, then in one
 * tick: select spear -> jab -> select swap item again.
 *
 * Flow:  park on swap item -> [spear -> jab -> swap item] -> wait interval -> repeat (while held)
 *        -> finally return to the original slot.
 */
public final class AttributeSwapMacro implements Macro<MacroEnvironment> {

    private enum State { START, AFTER_JAB, AFTER_WAIT, RESTORING, DONE }

    private final AttributeSwapConfig cfg;
    private final BooleanSupplier keyHeld;

    private State state = State.START;
    private int originalSlot;
    private int spearSlot;
    private int carrierSlot;
    private int jabs;

    public AttributeSwapMacro(AttributeSwapConfig cfg, BooleanSupplier keyHeld) {
        this.cfg = cfg;
        this.keyHeld = keyHeld;
    }

    @Override
    public String id() {
        return "spear_attribute_swap";
    }

    @Override
    public String prepare(MacroEnvironment env) {
        originalSlot = env.selectedSlot();
        jabs = 0;
        state = State.START;
        if (env.usingItem()) {
            return "Attribute Swap: stop using the current item first";
        }
        String err = resolve(env);
        if (err != null) {
            return err;
        }
        if (!foodOk(env)) {
            return "Attribute Swap: food level too low";
        }
        return null;
    }

    @Override
    public List<MacroStep<MacroEnvironment>> nextCycle(MacroEnvironment env, int cycleIndex) {
        switch (state) {
            case START: {
                state = State.AFTER_JAB;
                List<MacroStep<MacroEnvironment>> steps = new ArrayList<>();
                final int carrier = carrierSlot;
                steps.add(new MacroStep<>(Phase.START, 0, "park on swap item", e -> e.selectSlot(carrier)));
                steps.addAll(jabSteps(cfg.parkTicks));
                return steps;
            }
            case AFTER_JAB:
                if (!cfg.holdToRepeat || !canContinue(env)) {
                    return finishSteps();
                }
                state = State.AFTER_WAIT;
                return List.of(new MacroStep<>(Phase.START, cfg.repeatIntervalTicks, "repeat interval", e -> { }));
            case AFTER_WAIT:
                if (!canContinue(env)) {
                    return finishSteps();
                }
                state = State.AFTER_JAB;
                return jabSteps(0);
            default:
                state = State.DONE;
                return List.of();
        }
    }

    /** In hold mode, releasing the key ends the macro on the next tick. */
    @Override
    public boolean isStopRequested(MacroEnvironment env) {
        return cfg.holdToRepeat
                && state != State.RESTORING && state != State.DONE
                && !keyHeld.getAsBoolean();
    }

    @Override
    public void onFinish(MacroEnvironment env, boolean cancelled) {
        if (cancelled && cfg.swapBackToOriginal) {
            env.selectSlot(originalSlot);
        }
    }

    private List<MacroStep<MacroEnvironment>> jabSteps(int spearDelay) {
        jabs++;
        final int spear = spearSlot;
        final int carrier = carrierSlot;
        List<MacroStep<MacroEnvironment>> steps = new ArrayList<>();
        steps.add(new MacroStep<>(Phase.START, spearDelay, "select spear", e -> e.selectSlot(spear)));
        steps.add(new MacroStep<>(Phase.START, cfg.jabDelayTicks, "jab", e -> {
            if (!e.queueAttack()) {
                throw new IllegalStateException("attack key is unbound");
            }
        }));
        // END phase: runs after vanilla input handling has processed the jab in the same tick.
        steps.add(new MacroStep<>(Phase.END, cfg.returnDelayTicks, "select swap item", e -> e.selectSlot(carrier)));
        return steps;
    }

    private List<MacroStep<MacroEnvironment>> finishSteps() {
        if (!cfg.swapBackToOriginal) {
            state = State.DONE;
            return List.of();
        }
        state = State.RESTORING;
        final int original = originalSlot;
        // END phase + delay >= 1: the swap item stays selected across at least one tick boundary.
        return List.of(new MacroStep<>(Phase.END, cfg.restoreDelayTicks, "restore original slot",
                e -> e.selectSlot(original)));
    }

    private boolean canContinue(MacroEnvironment env) {
        return keyHeld.getAsBoolean()
                && (cfg.maxJabs == 0 || jabs < cfg.maxJabs)
                && foodOk(env)
                && !env.usingItem()
                && resolve(env) == null;
    }

    private boolean foodOk(MacroEnvironment env) {
        return cfg.minFoodLevel == 0 || env.foodLevel() >= cfg.minFoodLevel;
    }

    /** Re-resolve slots from the live hotbar. @return null if OK, else a reason. */
    private String resolve(MacroEnvironment env) {
        OptionalInt spear = SlotResolver.findSpear(env, cfg);
        if (spear.isEmpty()) {
            return cfg.requireLunge
                    ? "Attribute Swap: no Lunge spear found in hotbar"
                    : "Attribute Swap: no spear found in hotbar";
        }
        OptionalInt carrier = SlotResolver.findCarrier(env, cfg, spear.getAsInt());
        if (carrier.isEmpty()) {
            return "Attribute Swap: no swap item found in hotbar (see carrierItems)";
        }
        spearSlot = spear.getAsInt();
        carrierSlot = carrier.getAsInt();
        return null;
    }
}
