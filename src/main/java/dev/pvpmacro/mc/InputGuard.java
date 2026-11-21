package dev.pvpmacro.mc;

import net.minecraft.client.MinecraftClient;

/** Decides whether macros may run this tick. */
public final class InputGuard {

    private InputGuard() {
    }

    /** @return null if macros may run, otherwise a short reason. */
    public static String blockReason(MinecraftClient c) {
        if (c.player == null || c.world == null) {
            return "no world";
        }
        if (c.currentScreen != null) {      // chat, inventory, any GUI incl. text fields
            return "screen open";
        }
        if (!c.isWindowFocused()) {          // VERIFY: MinecraftClient#isWindowFocused()
            return "window not focused";
        }
        if (!c.mouse.isCursorLocked()) {     // VERIFY: Mouse#isCursorLocked()
            return "mouse not captured";
        }
        if (c.isPaused()) {                  // VERIFY: MinecraftClient#isPaused()
            return "game paused";
        }
        if (!c.player.isAlive()) {
            return "player dead";
        }
        return null;
    }
}
