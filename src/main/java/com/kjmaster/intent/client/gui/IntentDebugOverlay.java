package com.kjmaster.intent.client.gui;

import com.kjmaster.intent.Config;
import com.kjmaster.intent.Intent;
import com.kjmaster.intent.data.IntentProfile;
import com.kjmaster.intent.registry.IntentRegistries;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public class IntentDebugOverlay {

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!Config.DEBUG_MODE.get()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;

        // Position: Top Left (Active Status style)
        int x = 10;
        int y = 10;
        Font font = mc.font;

        // Draw Header
        graphics.drawString(font, "[Intent Debug]", x, y, 0xFFFFFF);
        y += 10;

        IntentProfile profile = Intent.DATA_MANAGER.getMasterProfile();
        for (IntentProfile.Binding binding : profile.bindings()) {

            // Header for the Key (e.g. "Key R")
            graphics.drawString(font, "Key: " + binding.triggerKey(), x, y, 0xAAAAAA);
            y += 10;

            for (IntentProfile.IntentEntry entry : binding.stack()) {
                boolean isActive = entry.context().test(player);

                // Green for Active, Red for Inactive
                int color = isActive ? 0x55FF55 : 0xFF5555;
                String status = isActive ? "[PASS]" : "[FAIL]";

                ResourceLocation typeId = IntentRegistries.CONTEXT_TYPE_REGISTRY.getKey(entry.context().getType());

                String line = String.format("  %s %s (%s)", status, entry.actionId(), typeId);

                graphics.drawString(font, line, x, y, color);
                y += 10;
            }

            // Spacer between keys
            y += 5;
        }
    }
}