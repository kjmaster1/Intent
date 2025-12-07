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

public record GuiIsOpenContext(Optional<String> screenClass) implements IIntentContext {

    public static final MapCodec<GuiIsOpenContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("screen_class", null)
                            .xmap(Optional::ofNullable, opt -> opt.orElse(null))
                            .forGetter(GuiIsOpenContext::screenClass)
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
        // This solves Obfuscation issues for 90% of gameplay screens (Inventory, Chests, Furnaces).
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            ResourceLocation key = BuiltInRegistries.MENU.getKey(containerScreen.getMenu().getType());
            // Check if the user's string matches/contains the registry key (e.g. "inventory" matches "minecraft:inventory")
            if (key != null && key.toString().contains(target)) {
                return true;
            }
        }

        // 2. Fallback: Full Class Name
        // Required for screens without Menus (Title Screen, Chat, etc.) if the user knows the full class name.
        String currentName = currentScreen.getClass().getName();
        if (currentName.contains(target)) {
            return true;
        }

        // 3. Fallback: Simple Class Name
        // This helps if packages are obfuscated but the class name is preserved.
        String simpleName = currentScreen.getClass().getSimpleName();
        return simpleName.contains(target);
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.GUI_IS_OPEN.get();
    }
}