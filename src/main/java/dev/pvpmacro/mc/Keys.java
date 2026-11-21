package dev.pvpmacro.mc;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class Keys {

    // Confirmed for 1.21.11 (Fabric docs/wiki): KeyBinding.Category.create(Identifier).
    // Translation key: key.category.pvpmacro.main
    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("pvpmacro", "main"));

    public static KeyBinding attributeSwap;
    public static KeyBinding masterToggle;

    private Keys() {
    }

    public static void register() {
        attributeSwap = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpmacro.attribute_swap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
        masterToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpmacro.master_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
    }
}
