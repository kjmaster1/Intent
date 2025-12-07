package com.kjmaster.intent.client.gui.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class ActionSelectionScreen extends Screen {

    private final Screen parent;
    private final Consumer<KeyMapping> onSelect;
    private ActionList actionList;
    private EditBox searchBox;
    private final List<KeyMapping> allMappings;

    public ActionSelectionScreen(Screen parent, Consumer<KeyMapping> onSelect) {
        super(Component.translatable("intent.gui.title.select_action"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.allMappings = new ArrayList<>(Arrays.asList(Minecraft.getInstance().options.keyMappings));
        // Sort by category, then name
        this.allMappings.sort(Comparator.comparing(KeyMapping::getCategory).thenComparing(KeyMapping::getName));
    }

    @Override
    protected void init() {
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 20, 200, 20, Component.translatable("intent.gui.label.search"));
        this.searchBox.setResponder(this::refreshList);
        this.addRenderableWidget(searchBox);

        this.actionList = new ActionList(this.minecraft, this.width, this.height - 90, 50, 24);
        this.addRenderableWidget(actionList);

        refreshList("");

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    private void refreshList(String query) {
        this.actionList.clearEntries();
        String lower = query.toLowerCase(Locale.ROOT);

        String currentCat = "";

        for (KeyMapping key : allMappings) {
            String name = Component.translatable(key.getName()).getString();
            String cat = Component.translatable(key.getCategory()).getString();

            if (name.toLowerCase(Locale.ROOT).contains(lower) || cat.toLowerCase(Locale.ROOT).contains(lower)) {
                // Add Category Header if changed
                if (!cat.equals(currentCat)) {
                    this.actionList.addEntry(new CategoryEntry(cat));
                    currentCat = cat;
                }
                this.actionList.addEntry(new ActionEntry(key, name));
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    // --- LIST CLASSES ---

    static class ActionList extends ObjectSelectionList<ActionList.Entry> {
        public ActionList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return 300; // Wider rows for better readability
        }

        @Override
        protected int addEntry(@NotNull Entry entry) {
            return super.addEntry(entry);
        }

        @Override
        protected void clearEntries() {
            super.clearEntries();
        }

        public abstract static class Entry extends ObjectSelectionList.Entry<Entry> {}
    }

    class CategoryEntry extends ActionList.Entry {
        private final String name;
        public CategoryEntry(String name) { this.name = name; }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            graphics.drawCenteredString(font, name, left + width / 2, top + 5, 0xFFFF55);
        }

        @Override
        public @NotNull Component getNarration() { return Component.literal(name); }
    }

    class ActionEntry extends ActionList.Entry {
        private final KeyMapping key;
        private final String name;

        public ActionEntry(KeyMapping key, String name) {
            this.key = key;
            this.name = name;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            int color = isHovered ? 0xFFFFFF : 0xAAAAAA;
            graphics.drawString(font, name, left + 10, top + 5, color);

            // Show current binding
            String bind = key.getTranslatedKeyMessage().getString();
            graphics.drawString(font, bind, left + width - font.width(bind) - 10, top + 5, 0x55FF55);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            onSelect.accept(key);
            onClose();
            return true;
        }

        @Override
        public @NotNull Component getNarration() { return Component.literal(name); }
    }
}