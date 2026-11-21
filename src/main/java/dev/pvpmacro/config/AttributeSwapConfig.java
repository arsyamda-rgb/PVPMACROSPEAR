package dev.pvpmacro.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Plain data class (serialized by Gson). Slots are 1-9 as shown in-game; 0 = auto-detect. */
public final class AttributeSwapConfig {

    public boolean enabled = true;

    /** Hotbar slot (1-9) of the spear, or 0 to auto-pick the spear with the highest Lunge level. */
    public int spearSlot = 0;

    /** Hotbar slot (1-9) to swap to, or 0 to auto-pick using carrierItems. */
    public int carrierSlot = 0;

    /**
     * Swap items in priority order, used when carrierSlot = 0. "minecraft:air" means an empty slot.
     * Pick items with no attack-speed penalty (food, wind charge, empty hand), not swords/axes/maces.
     */
    public List<String> carrierItems = defaultCarrierItems();

    /** Only treat spears that have the Lunge enchantment as valid. */
    public boolean requireLunge = true;

    /** true: keep chaining jabs while the key is held. false: one jab per key press. */
    public boolean holdToRepeat = true;

    /** Ticks parked on the swap item before the very first jab. */
    public int parkTicks = 2;

    /** Ticks between jabs while holding (measured from the swap-back to the swap item). */
    public int repeatIntervalTicks = 5;

    /** Ticks between selecting the spear and the jab. 0 = same tick. */
    public int jabDelayTicks = 0;

    /** Ticks between the jab and swapping back to the swap item. 0 = same tick. */
    public int returnDelayTicks = 0;

    /** Return to the slot you were on when the macro ends. */
    public boolean swapBackToOriginal = true;

    /** Ticks spent on the swap item before returning to the original slot at the end. */
    public int restoreDelayTicks = 1;

    /** Stop after this many jabs. 0 = unlimited. */
    public int maxJabs = 0;

    /** Stop when food level drops below this (each Lunge costs hunger). 0 = never stop. */
    public int minFoodLevel = 0;

    public static List<String> defaultCarrierItems() {
        return new ArrayList<>(List.of("minecraft:air", "minecraft:cooked_beef", "minecraft:wind_charge"));
    }

    /** Clamp everything to sane ranges so a hand-edited file cannot cause input spam. */
    public void sanitize() {
        spearSlot = clamp(spearSlot, 0, 9);
        carrierSlot = clamp(carrierSlot, 0, 9);
        parkTicks = clamp(parkTicks, 1, 20);
        // >= 2: the swap item must be held across at least one tick boundary to register.
        repeatIntervalTicks = clamp(repeatIntervalTicks, 2, 100);
        jabDelayTicks = clamp(jabDelayTicks, 0, 20);
        returnDelayTicks = clamp(returnDelayTicks, 0, 20);
        restoreDelayTicks = clamp(restoreDelayTicks, 1, 20);
        maxJabs = clamp(maxJabs, 0, 1000);
        minFoodLevel = clamp(minFoodLevel, 0, 20);
        List<String> cleaned = new ArrayList<>();
        if (carrierItems != null) {
            for (String s : carrierItems) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                String id = s.trim().toLowerCase(Locale.ROOT);
                cleaned.add(id.contains(":") ? id : "minecraft:" + id);
            }
        }
        carrierItems = cleaned.isEmpty() ? defaultCarrierItems() : cleaned;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
