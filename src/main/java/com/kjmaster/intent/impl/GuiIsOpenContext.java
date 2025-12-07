package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record GuiIsOpenContext(Optional<String> screenClass, boolean strict) implements IIntentContext {

    public static final MapCodec<GuiIsOpenContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("screen_class", null)
                            .xmap(Optional::ofNullable, opt -> opt.orElse(null))
                            .forGetter(GuiIsOpenContext::screenClass),
                    Codec.BOOL.optionalFieldOf("strict", false).forGetter(GuiIsOpenContext::strict)
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

        String target = screenClass.get();

        // 1. Stable Check: MenuType Registry Name
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            ResourceLocation key = BuiltInRegistries.MENU.getKey(containerScreen.getMenu().getType());
            if (key != null) {
                // Handle Strict vs Loose matching
                if (strict) {
                    if (key.toString().equals(target)) return true;
                } else {
                    if (key.toString().contains(target)) return true;
                }
            }
        }

        // 2. Fallback: Full Class Name
        String currentName = currentScreen.getClass().getName();
        if (strict) {
            if (currentName.equals(target)) return true;
        } else {
            if (currentName.contains(target)) return true;
        }

        // 3. Fallback: Simple Class Name
        String simpleName = currentScreen.getClass().getSimpleName();
        if (strict) {
            return simpleName.equals(target);
        } else {
            return simpleName.contains(target);
        }
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.GUI_IS_OPEN.get();
    }
}