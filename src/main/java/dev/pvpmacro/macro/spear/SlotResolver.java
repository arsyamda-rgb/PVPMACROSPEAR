package dev.pvpmacro.macro.spear;

import dev.pvpmacro.config.AttributeSwapConfig;
import dev.pvpmacro.core.MacroEnvironment;
import dev.pvpmacro.core.SlotInfo;

import java.util.OptionalInt;

/** Decides which hotbar slots are the spear and the swap item. Pure logic. */
public final class SlotResolver {

    private SlotResolver() {
    }

    public static OptionalInt findSpear(MacroEnvironment env, AttributeSwapConfig cfg) {
        if (cfg.spearSlot > 0) {
            int i = cfg.spearSlot - 1;
            return acceptable(env.hotbarSlot(i), cfg) ? OptionalInt.of(i) : OptionalInt.empty();
        }
        int best = -1;
        int bestLevel = -1;
        for (int i = 0; i < 9; i++) {
            SlotInfo info = env.hotbarSlot(i);
            if (!acceptable(info, cfg)) {
                continue;
            }
            if (info.lungeLevel() > bestLevel) { // strict: ties keep the lowest slot
                best = i;
                bestLevel = info.lungeLevel();
            }
        }
        return best < 0 ? OptionalInt.empty() : OptionalInt.of(best);
    }

    public static OptionalInt findCarrier(MacroEnvironment env, AttributeSwapConfig cfg, int spearSlot) {
        if (cfg.carrierSlot > 0) {
            int i = cfg.carrierSlot - 1;
            return i == spearSlot ? OptionalInt.empty() : OptionalInt.of(i);
        }
        for (String wanted : cfg.carrierItems) {
            boolean wantEmpty = wanted.equals("minecraft:air");
            for (int i = 0; i < 9; i++) {
                if (i == spearSlot) {
                    continue;
                }
                SlotInfo info = env.hotbarSlot(i);
                if (wantEmpty ? info.empty() : (!info.empty() && info.itemId().equals(wanted))) {
                    return OptionalInt.of(i);
                }
            }
        }
        return OptionalInt.empty();
    }

    private static boolean acceptable(SlotInfo info, AttributeSwapConfig cfg) {
        return info.spear() && (!cfg.requireLunge || info.lungeLevel() > 0);
    }
}
