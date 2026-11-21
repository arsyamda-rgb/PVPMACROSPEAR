package dev.pvpmacro.mc;

import dev.pvpmacro.PvpMacroClient;
import dev.pvpmacro.config.ConfigManager;
import dev.pvpmacro.config.PvpMacroConfig;
import dev.pvpmacro.core.MacroEnvironment;
import dev.pvpmacro.core.MacroRunner;
import dev.pvpmacro.core.Phase;
import dev.pvpmacro.macro.spear.AttributeSwapMacro;
import net.minecraft.client.MinecraftClient;

/** Wires keybinds, the input guard and the MacroRunner into the client tick. */
public final class MacroEngine {

    private final MacroRunner<MacroEnvironment> runner =
            new MacroRunner<>(msg -> PvpMacroClient.LOG.error(msg));
    private boolean swapKeyWasDown;

    public boolean isRunning() {
        return runner.isRunning();
    }

    /** Hooked to ClientTickEvents.START_CLIENT_TICK (before vanilla input handling). */
    public void onStartTick(MinecraftClient c) {
        PvpMacroConfig cfg = ConfigManager.get();

        while (Keys.masterToggle.wasPressed()) {
            cfg.masterEnabled = !cfg.masterEnabled;
            ConfigManager.save();
            Feedback.actionBar(c, "macros " + (cfg.masterEnabled ? "ON" : "OFF"));
        }

        String blocked = guardOrCancel(c, cfg);
        boolean down = Keys.attributeSwap.isPressed();
        if (blocked != null) {
            swapKeyWasDown = false;
            return;
        }

        boolean allowed = cfg.masterEnabled && cfg.attributeSwap.enabled;
        if (!allowed) {
            swapKeyWasDown = down;
            return;
        }

        MacroEnvironment env = new MinecraftEnvironment(c);
        // Edge-triggered: a held key never restarts the macro by itself (no unintended repeat).
        if (down && !swapKeyWasDown && !runner.isRunning()) {
            String err = runner.start(env, new AttributeSwapMacro(cfg.attributeSwap, Keys.attributeSwap::isPressed));
            if (err != null) {
                Feedback.actionBar(c, err);
            }
        }
        swapKeyWasDown = down;
        runner.onTick(env, Phase.START);
    }

    /** Hooked to ClientTickEvents.END_CLIENT_TICK (after vanilla input handling). */
    public void onEndTick(MinecraftClient c) {
        if (!runner.isRunning()) {
            return;
        }
        if (guardOrCancel(c, ConfigManager.get()) != null) {
            return;
        }
        runner.onTick(new MinecraftEnvironment(c), Phase.END);
    }

    /** @return the block reason (macro cancelled if one was running), or null if all is fine. */
    private String guardOrCancel(MinecraftClient c, PvpMacroConfig cfg) {
        String reason = InputGuard.blockReason(c);
        if (reason == null && runner.isRunning() && !cfg.masterEnabled) {
            reason = "macros disabled";
        }
        if (reason != null && runner.isRunning()) {
            if (c.player == null || c.world == null) {
                runner.reset();      // nothing left to restore
            } else {
                runner.cancel(new MinecraftEnvironment(c));
                Feedback.actionBar(c, "macro cancelled: " + reason);
            }
        }
        return reason;
    }
}
