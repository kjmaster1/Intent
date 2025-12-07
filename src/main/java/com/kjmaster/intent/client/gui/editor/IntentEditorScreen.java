package com.kjmaster.intent.client.gui.editor;

import com.kjmaster.intent.Intent;
import com.kjmaster.intent.data.IntentProfile;
import com.kjmaster.intent.registry.IntentRegistries;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class IntentEditorScreen extends Screen {

    private BindingList bindingList;
    private RuleList ruleList;
    private IntentProfile.Binding selectedBinding;

    public IntentEditorScreen() {
        super(Component.translatable("intent.gui.title.intent_editor_screen"));
    }

    @Override
    protected void init() {
        // --- 1. Left Pane (Keys) ---
        int leftPaneX = 10;
        int leftPaneWidth = 150;

        this.bindingList = new BindingList(this.minecraft, leftPaneWidth, this.height - 80, 30, 24);
        this.bindingList.setX(leftPaneX);
        this.addRenderableWidget(bindingList);

        // Use a LinearLayout to group and space the buttons automatically
        LinearLayout leftBtnLayout = LinearLayout.horizontal().spacing(5);
        leftBtnLayout.addChild(Button.builder(Component.translatable("intent.editor.button.add_key_symbol"), b -> openAddKeyPopup())
                .width(70).build());
        leftBtnLayout.addChild(Button.builder(Component.translatable("intent.editor.button.remove_key_symbol"), b -> removeSelectedKey())
                .width(70).build());

        // Calculate layout size
        leftBtnLayout.arrangeElements();

        // Center the layout horizontally within the Left Pane area
        int leftPaneCenter = leftPaneX + (leftPaneWidth / 2);
        leftBtnLayout.setPosition(leftPaneCenter - (leftBtnLayout.getWidth() / 2), this.height - 75);

        // Register widgets
        leftBtnLayout.visitWidgets(this::addRenderableWidget);


        // --- 2. Right Pane (Rules) ---
        int rightPaneX = 170;
        int rightPaneWidth = this.width - rightPaneX - 10;

        this.ruleList = new RuleList(this.minecraft, rightPaneWidth, this.height - 80, 30, 36);
        this.ruleList.setX(rightPaneX);
        this.addRenderableWidget(ruleList);

        // Use a LinearLayout for the Rule buttons
        LinearLayout rightBtnLayout = LinearLayout.horizontal().spacing(10);
        rightBtnLayout.addChild(Button.builder(Component.translatable("intent.editor.button.add_rule"), b -> openAddRuleScreen())
                .width(100).build());
        rightBtnLayout.addChild(Button.builder(Component.translatable("intent.editor.button.remove_rule"), b -> removeSelectedRule())
                .width(100).build());

        rightBtnLayout.arrangeElements();

        // Center the layout horizontally within the Right Pane area
        int rightPaneCenter = rightPaneX + (rightPaneWidth / 2);
        rightBtnLayout.setPosition(rightPaneCenter - (rightBtnLayout.getWidth() / 2), this.height - 75);

        rightBtnLayout.visitWidgets(this::addRenderableWidget);


        refreshLeftList();


        // --- 3. Footer Buttons ---
        // Use a LinearLayout for the bottom Save/Done buttons
        LinearLayout footerLayout = LinearLayout.horizontal().spacing(10);

        footerLayout.addChild(Button.builder(Component.translatable("intent.editor.button.save"), button -> {
            Intent.DATA_MANAGER.saveToDisk();
            this.onClose();
        }).width(100).build());

        footerLayout.addChild(Button.builder(Component.translatable("intent.editor.button.done"), button -> this.onClose())
                .width(100).build());

        footerLayout.arrangeElements();

        // Center on the entire screen
        footerLayout.setPosition((this.width - footerLayout.getWidth()) / 2, this.height - 25);

        footerLayout.visitWidgets(this::addRenderableWidget);
    }

    // --- Actions ---

    private void openAddKeyPopup() {
        this.minecraft.setScreen(new AddKeyScreen(this));
    }

    private void removeSelectedKey() {
        if (selectedBinding != null) {
            Intent.DATA_MANAGER.removeBinding(selectedBinding.triggerKey());
            this.selectedBinding = null;
            refreshLeftList();
        }
    }

    private void openAddRuleScreen() {
        if (selectedBinding == null) return;
        this.minecraft.setScreen(new EditRuleScreen(this, null, newEntry -> {
            List<IntentProfile.IntentEntry> stack = new ArrayList<>(selectedBinding.stack());
            stack.add(newEntry);
            updateBindingInManager(selectedBinding, new IntentProfile.Binding(selectedBinding.triggerKey(), stack));
        }));
    }

    private void removeSelectedRule() {
        RuleEntry selected = ruleList.getSelected();
        if (selected != null && selectedBinding != null) {
            List<IntentProfile.IntentEntry> stack = new ArrayList<>(selectedBinding.stack());
            stack.remove(selected.entry);
            updateBindingInManager(selectedBinding, new IntentProfile.Binding(selectedBinding.triggerKey(), stack));
        }
    }

    // --- Refresh Logic ---

    private void refreshLeftList() {
        String selectedKey = (selectedBinding != null) ? selectedBinding.triggerKey() : null;

        bindingList.clearEntries();
        IntentProfile master = Intent.DATA_MANAGER.getMasterProfile();

        for (IntentProfile.Binding binding : master.bindings()) {
            BindingEntry entry = new BindingEntry(binding);
            bindingList.addEntry(entry);

            if (binding.triggerKey().equals(selectedKey)) {
                bindingList.setSelected(entry);
                this.selectedBinding = binding;
            }
        }

        ruleList.refreshRules(selectedBinding);
    }

    public void selectBinding(IntentProfile.Binding binding) {
        this.selectedBinding = binding;
        this.ruleList.refreshRules(binding);
    }

    public void updateBindingInManager(IntentProfile.Binding oldBinding, IntentProfile.Binding newBinding) {
        Intent.DATA_MANAGER.updateBinding(newBinding);
        this.selectedBinding = newBinding;
        refreshLeftList();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        RuleEntry selectedRule = ruleList.getSelected();
        if (selectedRule != null && selectedBinding != null) {
            if (keyCode == GLFW.GLFW_KEY_UP) {
                selectedRule.changePriority(10);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                selectedRule.changePriority(-10);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int left = 170;
        int right = this.width - 10;
        int top = 30;
        int bottom = this.height - 50;
        graphics.fill(left, top, right, bottom, 0x80000000);

        if (selectedBinding == null) {
            graphics.drawCenteredString(this.font, Component.translatable("intent.editor.label.select_key"), left + (right - left) / 2, top + 50, 0xAAAAAA);
        } else {
            graphics.drawCenteredString(this.font, Component.translatable("intent.editor.label.priority_hint"), this.width / 2, bottom + 5, 0xAAAAAA);
        }
    }

    // ============================
    // INNER CLASS: ADD KEY SCREEN
    // ============================
    private static class AddKeyScreen extends Screen {
        private final Screen parent;
        private final Component message = Component.translatable("intent.editor.label.press_key");

        public AddKeyScreen(Screen parent) {
            super(Component.translatable("intent.editor.screen.add_key"));
            this.parent = parent;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.onClose();
                return true;
            }
            InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
            finish(key);
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
            finish(key);
            return true;
        }

        private void finish(InputConstants.Key key) {
            IntentProfile.Binding newBinding = new IntentProfile.Binding(key.getName(), new ArrayList<>());
            Intent.DATA_MANAGER.updateBinding(newBinding);
            this.onClose();
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(parent);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(this.font, message, this.width / 2, this.height / 2, 0xFFFFFF);
        }
    }

    // ============================
    // LEFT PANE (BINDINGS)
    // ============================
    public class BindingList extends ObjectSelectionList<BindingEntry> {
        public BindingList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return 130;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int addEntry(@NotNull BindingEntry entry) {
            return super.addEntry(entry);
        }
    }

    public class BindingEntry extends ObjectSelectionList.Entry<BindingEntry> {
        private final IntentProfile.Binding binding;
        private final String formattedName;

        public BindingEntry(IntentProfile.Binding binding) {
            this.binding = binding;
            String raw = binding.triggerKey();
            if (raw.startsWith("key.keyboard.")) raw = raw.replace("key.keyboard.", "");
            else if (raw.startsWith("key.mouse.")) raw = raw.replace("key.mouse.", "Mouse ");
            this.formattedName = raw.toUpperCase();
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            int color = (selectedBinding != null && selectedBinding.triggerKey().equals(binding.triggerKey())) ? 0xFFFF00 : 0xFFFFFF;
            graphics.drawString(Minecraft.getInstance().font, formattedName, left + 2, top + 5, color, false);
            String count = String.valueOf(binding.stack().size());
            graphics.drawString(Minecraft.getInstance().font, count, left + width - 15, top + 5, 0xAAAAAA, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            bindingList.setSelected(this);
            IntentEditorScreen.this.selectBinding(binding);
            return true;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(formattedName);
        }
    }


    // ============================
    // RIGHT PANE (RULES)
    // ============================
    public class RuleList extends ObjectSelectionList<RuleEntry> {
        public RuleList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        @Override
        protected void renderSelection(@NotNull GuiGraphics graphics, int top, int width, int height, int outerColor, int innerColor) {
        }

        public void refreshRules(IntentProfile.Binding binding) {
            super.clearEntries();
            if (binding == null) return;
            var sorted = binding.stack().stream()
                    .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                    .toList();
            for (IntentProfile.IntentEntry entry : sorted) {
                this.addEntry(new RuleEntry(entry));
            }
        }
    }

    public class RuleEntry extends ObjectSelectionList.Entry<RuleEntry> {
        public final IntentProfile.IntentEntry entry;
        private final String actionName;
        private final String contextName;

        public RuleEntry(IntentProfile.IntentEntry entry) {
            this.entry = entry;
            String act = entry.actionId();
            if (act.contains(".")) act = act.substring(act.lastIndexOf('.') + 1);
            this.actionName = act.toUpperCase();

            ResourceLocation id = IntentRegistries.CONTEXT_TYPE_REGISTRY.getKey(entry.context().getType());
            this.contextName = (id != null) ? id.getPath().replace("_", " ").toUpperCase() : "UNKNOWN";
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            boolean isSelected = (ruleList.getSelected() == this);
            int bgColor = isHovered ? 0x60000000 : 0x40000000;
            graphics.fill(left, top, left + width, top + height - 4, bgColor);
            int borderColor = isSelected ? 0xFFFFFF00 : 0xFF444444;
            graphics.renderOutline(left, top, width, height - 4, borderColor);

            graphics.drawString(font, actionName, left + 5, top + 5, 0xFFFFFF, true);

            // Using parameterized translation for "When: [Context]"
            graphics.drawString(font, Component.translatable("intent.editor.label.when", contextName), left + 5, top + 18, 0xAAAAAA, false);

            // Using parameterized translation for "P: [Priority]"
            Component prioComp = Component.translatable("intent.editor.label.priority_short", entry.priority());
            int prioWidth = font.width(prioComp);
            graphics.drawString(font, prioComp, left + width - prioWidth - 10, top + 12, 0x55FF55, true);
        }

        public void changePriority(int amount) {
            List<IntentProfile.IntentEntry> mutableStack = new ArrayList<>(selectedBinding.stack());
            mutableStack.remove(entry);

            IntentProfile.IntentEntry newEntry = new IntentProfile.IntentEntry(
                    entry.actionId(),
                    entry.context(),
                    entry.priority() + amount
            );
            mutableStack.add(newEntry);

            IntentProfile.Binding newBinding = new IntentProfile.Binding(selectedBinding.triggerKey(), mutableStack);
            updateBindingInManager(selectedBinding, newBinding);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            ruleList.setSelected(this);
            return true;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(actionName);
        }
    }
}