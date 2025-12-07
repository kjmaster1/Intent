package com.kjmaster.intent.client.gui;

import com.kjmaster.intent.Intent;
import com.kjmaster.intent.data.IntentProfile;
import com.kjmaster.intent.util.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class IntentOverlay implements LayeredDraw.Layer {

    // Maximum width in pixels before text is truncated
    private static final int MAX_WIDTH = 150;

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Position: Bottom right, -55 to generally sit above the Food/Armor bar layer
        int x = screenWidth - 5;
        int y = screenHeight - 55;

        IntentProfile profile = Intent.DATA_MANAGER.getMasterProfile();

        for (IntentProfile.Binding binding : profile.bindings()) {
            var activeMatch = binding.stack().stream()
                    .filter(entry -> entry.context().test(player))
                    .max(Comparator.comparingInt(IntentProfile.IntentEntry::priority));

            if (activeMatch.isPresent()) {
                IntentProfile.IntentEntry entry = activeMatch.get();

                String keyName = InputConstants.getKey(binding.triggerKey()).getDisplayName().getString();

                // Safely get the action name (handles missing mappings)
                var mapping = KeyMappingHelper.getMapping(entry.actionId());
                String actionName = (mapping != null)
                        ? Component.translatable(mapping.getName()).getString()
                        : entry.actionId();

                String text = "[" + keyName + "] " + actionName;

                // Truncate logic to prevent HUD overlap
                if (mc.font.width(text) > MAX_WIDTH) {
                    String ellipsis = "...";
                    String trimmed = mc.font.plainSubstrByWidth(text, MAX_WIDTH - mc.font.width(ellipsis));
                    text = trimmed + ellipsis;
                }

                int width = mc.font.width(text);

                // Draw with shadow
                graphics.drawString(mc.font, text, x - width, y, 0xFFFFFF, true);

                // Stack upwards
                y -= 12;
            }
        }
    }
}