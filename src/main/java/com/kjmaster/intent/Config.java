package com.kjmaster.intent;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue RADIAL_MENU_DELAY;
    public static final ModConfigSpec.BooleanValue DEBUG_MODE;
    public static final ModConfigSpec.DoubleValue RADIAL_MENU_SCALE;

    // NEW: Global Context Defaults
    public static final ModConfigSpec.IntValue IN_COMBAT_TIMEOUT;

    static {
        BUILDER.push("client");

        RADIAL_MENU_DELAY = BUILDER
                .comment("The time in ticks (1/20th second) to hold a key before the Radial Menu opens.",
                        "If 0, the menu opens instantly.",
                        "If > 0, releasing the key before this time will trigger the highest priority action (Tap behavior).")
                .defineInRange("radial_menu_delay", 0, 0, 40);

        DEBUG_MODE = BUILDER
                .comment("If true, the Intent Debug Overlay will be visible.")
                .define("debug_mode", false);

        RADIAL_MENU_SCALE = BUILDER
                .comment("Scale multiplier for the Radial Menu size.")
                .defineInRange("radial_menu_scale", 1.0, 0.5, 2.0);

        IN_COMBAT_TIMEOUT = BUILDER
                .comment("Default time in seconds to remain in 'Combat Mode' after taking or dealing damage.",
                        "This is used if a profile does not specify a specific timeout.")
                .defineInRange("in_combat_timeout", 10, 1, 600);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}