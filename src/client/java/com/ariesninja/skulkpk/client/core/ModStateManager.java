package com.ariesninja.skulkpk.client.core;

public class ModStateManager {
    private static boolean modEnabled = true;

    public static boolean isModEnabled() {
        return modEnabled;
    }

    public static void setModEnabled(boolean enabled) {
        modEnabled = enabled;
    }

    public static void disableMod() {
        modEnabled = false;
    }

    public static void enableMod() {
        modEnabled = true;
    }
}
