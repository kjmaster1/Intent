package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

import java.util.Optional;

public record GuiIsOpenContext(Optional<String> screenClass) implements IIntentContext {

    public static final MapCodec<GuiIsOpenContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("screen_class").forGetter(GuiIsOpenContext::screenClass)
            ).apply(instance, GuiIsOpenContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        Screen currentScreen = Minecraft.getInstance().screen;

        // If context expects a screen, but none is open -> False
        if (currentScreen == null) {
            return false;
        }

        // If no specific class was requested, just "Any Screen" -> True
        if (screenClass.isEmpty()) {
            return true;
        }

        // Check if the current screen class name matches (or contains) the config string
        // We use 'contains' to allow lazy matching like "Inventory" instead of "net.minecraft..."
        String currentName = currentScreen.getClass().getName();
        return currentName.contains(screenClass.get());
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.GUI_IS_OPEN.get();
    }
}