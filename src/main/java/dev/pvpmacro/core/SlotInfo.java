package dev.pvpmacro.core;

/** Minecraft-free snapshot of one hotbar slot. */
public record SlotInfo(String itemId, boolean empty, boolean spear, int lungeLevel) {
    public static final SlotInfo EMPTY = new SlotInfo("minecraft:air", true, false, 0);
}
