package dev.pvpmacro.mc;

import dev.pvpmacro.core.MacroEnvironment;
import dev.pvpmacro.core.SlotInfo;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Adapter between the Minecraft-free core and the real client.
 * Every Yarn name here that is NOT confirmed against the 1.21.11 Yarn sources is marked VERIFY.
 */
public final class MinecraftEnvironment implements MacroEnvironment {

    private final MinecraftClient client;

    public MinecraftEnvironment(MinecraftClient client) {
        this.client = client;
    }

    private ClientPlayerEntity player() {
        ClientPlayerEntity p = client.player;
        if (p == null) {
            throw new IllegalStateException("no player");
        }
        return p;
    }

    @Override
    public int selectedSlot() {
        // VERIFY: PlayerInventory#getSelectedSlot() (selectedSlot became private in 1.21.5+)
        return player().getInventory().getSelectedSlot();
    }

    @Override
    public void selectSlot(int slot) {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("hotbar slot out of range: " + slot);
        }
        // VERIFY: PlayerInventory#setSelectedSlot(int)
        player().getInventory().setSelectedSlot(slot);
    }

    @Override
    public SlotInfo hotbarSlot(int slot) {
        ItemStack stack = player().getInventory().getStack(slot); // VERIFY: getStack(int)
        if (stack.isEmpty()) {
            return SlotInfo.EMPTY;
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        // All 7 spear variants are named *_spear (wooden ... netherite).
        boolean spear = id.getPath().endsWith("_spear");
        return new SlotInfo(id.toString(), false, spear, spear ? lungeLevel(stack) : 0);
    }

    private static int lungeLevel(ItemStack stack) {
        // VERIFY: ItemStack#getEnchantments() -> ItemEnchantmentsComponent;
        //         ItemEnchantmentsComponent#getEnchantments() / #getLevel(RegistryEntry)
        ItemEnchantmentsComponent enchants = stack.getEnchantments();
        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            boolean lunge = entry.getKey()
                    .map(key -> key.getValue().getPath().equals("lunge"))
                    .orElse(false);
            if (lunge) {
                return enchants.getLevel(entry);
            }
        }
        return 0;
    }

    @Override
    public int foodLevel() {
        return player().getHungerManager().getFoodLevel();
    }

    @Override
    public boolean usingItem() {
        return player().isUsingItem();
    }

    /**
     * Queues a real press of the attack key. Vanilla input handling then performs the normal
     * attack/jab routine in the same tick, with the currently selected slot. No mixin needed.
     * Note: vanilla discards attack input while the player is using an item (see usingItem()).
     */
    @Override
    public boolean queueAttack() {
        KeyBinding attack = client.options.attackKey;
        if (attack.isUnbound()) { // VERIFY: KeyBinding#isUnbound()
            return false;
        }
        // VERIFY: static KeyBinding.onKeyPressed(InputUtil.Key); getBoundKeyOf from Fabric API
        KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(attack));
        return true;
    }
}
