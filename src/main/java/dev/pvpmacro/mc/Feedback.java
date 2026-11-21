package dev.pvpmacro.mc;

import dev.pvpmacro.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class Feedback {

    private Feedback() {
    }

    /** Short action-bar message, only if enabled in the config. */
    public static void actionBar(MinecraftClient c, String message) {
        if (c.player != null && ConfigManager.get().feedback) {
            c.player.sendMessage(Text.literal("[PvPMacro] " + message), true); // VERIFY: sendMessage(Text, boolean)
        }
    }
}
