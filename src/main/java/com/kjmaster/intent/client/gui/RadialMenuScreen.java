package com.kjmaster.intent.client.gui;

import com.kjmaster.intent.Config;
import com.kjmaster.intent.client.InputHandler;
import com.kjmaster.intent.data.IntentProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import java.util.List;

public class RadialMenuScreen extends Screen {

    private final List<IntentProfile.IntentEntry> options;
    private final InputConstants.Key triggerKey;

    // We cache the player's state when the menu opens
    private final boolean wasSneaking;
    private final boolean wasSprinting;

    private int selectedIndex = -1;

    public RadialMenuScreen(List<IntentProfile.IntentEntry> options, InputConstants.Key triggerKey) {
        super(Component.literal("Intent Selection"));
        this.options = options;
        this.triggerKey = triggerKey;

        var player = Minecraft.getInstance().player;
        this.wasSneaking = player != null && player.isShiftKeyDown();
        this.wasSprinting = player != null && player.isSprinting();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // 1. Keep the player sneaking/sprinting while the menu is open
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.input.shiftKeyDown = wasSneaking;
            minecraft.player.setSprinting(wasSprinting);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Do NOT render background (darkening)

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int count = options.size();

        // SCALING: Calculate dynamic radius based on screen size AND Config
        double baseScale = Math.min(this.width, this.height) * 0.35;
        double userScale = Config.RADIAL_MENU_SCALE.get();

        double radiusMax = baseScale * userScale;
        double radiusMin = radiusMax * 0.4;

        // --- 1. Draw the Wheel (The Donut) ---
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        double sectorSize = 360.0 / count;

        // Calculate Selection
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 10) { // Deadzone
            double angleRad = Math.atan2(dy, dx);
            double angleDeg = Math.toDegrees(angleRad) + 90;
            if (angleDeg < 0) angleDeg += 360;

            this.selectedIndex = (int) (angleDeg / sectorSize);
            if (this.selectedIndex >= count) this.selectedIndex = 0;
        } else {
            this.selectedIndex = -1;
        }

        for (int i = 0; i < count; i++) {
            boolean isSelected = (i == selectedIndex);

            // Colors (ARGB)
            int alpha = 180;
            int red = isSelected ? 255 : 0;
            int green = isSelected ? 255 : 0;
            int blue = isSelected ? 255 : 0;

            // If not selected, make it dark gray
            if (!isSelected) {
                red = 30;
                green = 30;
                blue = 30;
            }

            double startAngle = Math.toRadians((i * sectorSize) - 90);
            double endAngle = Math.toRadians(((i + 1) * sectorSize) - 90);

            // Draw Quad as 2 Triangles
            float x1 = (float) (centerX + Math.cos(startAngle) * radiusMin);
            float y1 = (float) (centerY + Math.sin(startAngle) * radiusMin);
            float x2 = (float) (centerX + Math.cos(startAngle) * radiusMax);
            float y2 = (float) (centerY + Math.sin(startAngle) * radiusMax);
            float x3 = (float) (centerX + Math.cos(endAngle) * radiusMax);
            float y3 = (float) (centerY + Math.sin(endAngle) * radiusMax);
            float x4 = (float) (centerX + Math.cos(endAngle) * radiusMin);
            float y4 = (float) (centerY + Math.sin(endAngle) * radiusMin);

            buffer.addVertex(matrix, x1, y1, 0).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, x2, y2, 0).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, x3, y3, 0).setColor(red, green, blue, alpha);

            buffer.addVertex(matrix, x1, y1, 0).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, x3, y3, 0).setColor(red, green, blue, alpha);
            buffer.addVertex(matrix, x4, y4, 0).setColor(red, green, blue, alpha);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();

        // --- 2. Draw Labels ---
        for (int i = 0; i < count; i++) {
            double midAngle = ((i * sectorSize) + (sectorSize / 2)) - 90;
            double rad = Math.toRadians(midAngle);
            double textDist = (radiusMin + radiusMax) / 2.0;

            int tx = centerX + (int) (Math.cos(rad) * textDist);
            int ty = centerY + (int) (Math.sin(rad) * textDist);

            String name = simplifyName(options.get(i).actionId());
            int textWidth = font.width(name);

            // TEXT SCALING
            double arcLength = (2 * Math.PI * textDist) * (sectorSize / 360.0);
            double maxTextWidth = arcLength * 0.85;

            float scale = 1.0f;
            if (textWidth > maxTextWidth) {
                scale = (float) (maxTextWidth / textWidth);
            }

            graphics.pose().pushPose();
            graphics.pose().translate(tx, ty, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            graphics.drawString(font, name, -textWidth / 2, -4, 0xFFFFFFFF, true);

            graphics.pose().popPose();
        }

        // --- 3. Draw Selected Action ---
        if (selectedIndex != -1) {
            String fullName = options.get(selectedIndex).actionId();
            graphics.drawCenteredString(font, "Selected: " + fullName, centerX, centerY - (int) radiusMax - 20, 0xFF55FF55);
        }
    }

    private String simplifyName(String id) {
        if (id.contains(".")) return id.substring(id.lastIndexOf('.') + 1).toUpperCase();
        return id;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (key.equals(triggerKey)) {
            executeSelection();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left Click
            executeSelection();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void executeSelection() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            IntentProfile.IntentEntry entry = options.get(selectedIndex);
            this.onClose();
            // We pass the key so InputHandler knows what to look for
            InputHandler.performAction(entry, triggerKey, 5);
        } else {
            this.onClose();
        }
    }
}