package com.ariesninja.skulkpk.client.core;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static final String CATEGORY = "SkulkPK";

    public static final KeyBinding SELECT_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.skulkpk.select",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    CATEGORY
            )
    );

    public static final KeyBinding EXECUTE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.skulkpk.execute",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    CATEGORY
            )
    );

    public static final KeyBinding TEST_KEY_1 = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.skulkpk.test",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_J,
                    CATEGORY
            )
    );

    public static final KeyBinding TEST_KEY_2 = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.skulkpk.test2",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    CATEGORY
            )
    );

    public static void register() {
        // This method is called to ensure the keybindings are registered.
    }
} 