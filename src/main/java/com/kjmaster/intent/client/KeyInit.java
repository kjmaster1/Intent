package com.kjmaster.intent.client;

import com.kjmaster.intent.Intent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Intent.MODID)
public class KeyInit {

    // Default to 'I' for Intent
    public static final Lazy<KeyMapping> OPEN_EDITOR = Lazy.of(() -> new KeyMapping(
            "key.intent.editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "key.categories.intent"
    ));

    public static final Lazy<KeyMapping> TOGGLE_DEBUG = Lazy.of(() -> new KeyMapping(
            "key.intent.debug",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "key.categories.intent"
    ));

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR.get());
        event.register(TOGGLE_DEBUG.get());
    }
}