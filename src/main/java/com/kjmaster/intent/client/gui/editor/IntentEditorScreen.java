package com.kjmaster.intent.client.gui.editor;

import com.kjmaster.intent.Intent;
import com.kjmaster.intent.data.IntentProfile;
import com.kjmaster.intent.registry.IntentRegistries;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
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
        super(Component.literal("Intent Editor"));
    }

    @Override
    protected void init() {
        // 1. Left Pane (Keys)
        this.bindingList = new BindingList(this.minecraft, 150, this.height - 80, 30, 24);
        this.bindingList.setX(10);
        this.addRenderableWidget(bindingList);

        // 1b. Left Pane Buttons (Add/Remove Key)
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> openAddKeyPopup())
                .bounds(10, this.height - 75, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> removeSelectedKey())
                .bounds(85, this.height - 75, 70, 20).build());


        // 2. Right Pane (Rules)
        int leftStart = 170;
        int ruleListWidth = this.width - leftStart - 10;
        this.ruleList = new RuleList(this.minecraft, ruleListWidth, this.height - 80, 30, 36);
        this.ruleList.setX(leftStart);
        this.addRenderableWidget(ruleList);

        // 2b. Right Pane Buttons (Add/Remove Rule)
        this.addRenderableWidget(Button.builder(Component.literal("Add Rule"), b -> openAddRuleScreen())
                .bounds(leftStart, this.height - 75, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Remove Rule"), b -> removeSelectedRule())
                .bounds(leftStart + 110, this.height - 75, 100, 20).build());


        refreshLeftList();

        // 3. Footer Buttons
        int center = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            Intent.DATA_MANAGER.saveToDisk();
            this.onClose();
        }).bounds(center - 105, this.height - 25, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(center + 5, this.height - 25, 100, 20).build());
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
            graphics.drawCenteredString(this.font, "Select a Key to Edit", left + (right - left) / 2, top + 50, 0xAAAAAA);
        } else {
            graphics.drawCenteredString(this.font, "Use UP/DOWN to change priority", this.width / 2, bottom + 5, 0xAAAAAA);
        }
    }

    // ============================
    // INNER CLASS: ADD KEY SCREEN
    // ============================
    private static class AddKeyScreen extends Screen {
        private final Screen parent;
        private final Component message = Component.literal("Press a Key to Bind...");

        public AddKeyScreen(Screen parent) {
            super(Component.literal("Add Key"));
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
            graphics.drawString(font, "When: " + contextName, left + 5, top + 18, 0xAAAAAA, false);

            String prio = "P: " + entry.priority();
            int prioWidth = font.width(prio);
            graphics.drawString(font, prio, left + width - prioWidth - 10, top + 12, 0x55FF55, true);
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