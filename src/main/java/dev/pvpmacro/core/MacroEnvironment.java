package dev.pvpmacro.core;

/**
 * Everything a macro may do to the game. Implemented by the Minecraft adapter
 * (dev.pvpmacro.mc.MinecraftEnvironment) and by a fake in the tests.
 */
public interface MacroEnvironment {

    /** Selected hotbar slot, 0..8. */
    int selectedSlot();

    void selectSlot(int slot);

    SlotInfo hotbarSlot(int slot);

    int foodLevel();

    /** True while eating/drawing a bow/blocking etc. (vanilla drops attack input then). */
    boolean usingItem();

    /**
     * Queue one press of the player's attack key, processed by vanilla input handling.
     * @return false if it could not be queued (attack key unbound)
     */
    boolean queueAttack();
}
