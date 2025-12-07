package com.kjmaster.intent.client.gui.editor;

import com.kjmaster.intent.api.IIntentContext;
import com.kjmaster.intent.api.client.ContextEditorRegistry;
import com.kjmaster.intent.api.client.IContextEditor;
import com.kjmaster.intent.data.IntentProfile;
import com.kjmaster.intent.impl.ContextTypes;
import com.kjmaster.intent.registry.IntentRegistries;
import com.kjmaster.intent.util.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class EditRuleScreen extends Screen {

    private final Screen parent;
    private final Consumer<IntentProfile.IntentEntry> onComplete;
    private final IntentProfile.IntentEntry existingEntry;

    // --- Header Fields (Action & Priority) ---
    private String selectedActionId = "";
    private Button actionSelectBtn;
    private Button virtualizeBtn;
    private EditBox priorityBox;

    // --- Dynamic Editor Fields ---
    private IIntentContext.ContextType<?> selectedType;
    private IContextEditor<?> currentEditor;
    private final List<AbstractWidget> dynamicWidgets = new ArrayList<>();

    public EditRuleScreen(Screen parent, IntentProfile.IntentEntry existing, Consumer<IntentProfile.IntentEntry> onComplete) {
        super(Component.translatable("intent.gui.label.edit_rule"));
        this.parent = parent;
        this.existingEntry = existing;
        this.onComplete = onComplete;

        if (existing != null) {
            this.selectedType = existing.context().getType();
            this.selectedActionId = existing.actionId();
        } else {
            // Default to the first registered type or Sneaking
            this.selectedType = ContextTypes.SNEAKING.get();
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;

        // 1. Action Selection (Picker + Virtualize)
        // -------------------------------------------------------------------------
        this.addRenderableWidget(Button.builder(Component.translatable("intent.gui.label.action_id.button"), b -> {
                })
                .bounds(centerX - 100, startY - 12, 200, 10).build()).active = false;

        Component actionLabel = Component.translatable("intent.gui.label.action");
        if (!selectedActionId.isEmpty()) {
            KeyMapping km = KeyMappingHelper.getMapping(selectedActionId);
            actionLabel = (km != null) ? Component.translatable(km.getName()) : Component.literal(selectedActionId);
        }

        this.actionSelectBtn = Button.builder(actionLabel, b -> {
            this.minecraft.setScreen(new ActionSelectionScreen(this, (keyMapping) -> {
                this.selectedActionId = keyMapping.getName();
            }));
        }).bounds(centerX - 100, startY, 150, 20).build();
        this.addRenderableWidget(actionSelectBtn);

        this.virtualizeBtn = Button.builder(Component.translatable("intent.gui.label.virtualize"), b -> virtualizeTarget())
                .bounds(centerX + 55, startY, 45, 20)
                .tooltip(Tooltip.create(Component.translatable("intent.gui.label.virtualize.tooltip")))
                .build();
        this.addRenderableWidget(virtualizeBtn);

        updateVirtualizeButtonState();
        startY += 35;


        // 2. Priority Input
        // -------------------------------------------------------------------------
        this.addRenderableWidget(Button.builder(Component.translatable("intent.gui.label.priority.button"), b -> {
                })
                .bounds(centerX - 100, startY - 12, 200, 10).build()).active = false;

        this.priorityBox = new EditBox(this.font, centerX - 100, startY, 200, 20, Component.translatable("intent.gui.label.priority"));
        this.priorityBox.setValue(existingEntry != null ? String.valueOf(existingEntry.priority()) : "10");
        this.addRenderableWidget(priorityBox);
        startY += 35;


        // 3. Context Type Selector (Cycle Button)
        // -------------------------------------------------------------------------
        List<? extends IIntentContext.ContextType<?>> types = IntentRegistries.CONTEXT_TYPES.getEntries().stream()
                .map(holder -> (IIntentContext.ContextType<?>) holder.get())
                .toList();

        CycleButton.Builder<IIntentContext.ContextType<?>> builder = CycleButton.builder(this::formatContextName);

        @SuppressWarnings("unchecked")
        CycleButton<IIntentContext.ContextType<?>> contextTypeBtn = builder
                .withValues((Collection<IIntentContext.ContextType<?>>) types)
                .withInitialValue(selectedType)
                .create(centerX - 100, startY, 200, 20, Component.translatable("intent.gui.label.context"), (btn, val) -> {
                    this.selectedType = val;
                    refreshDynamicWidgets(); // Rebuild UI when type changes
                });
        this.addRenderableWidget(contextTypeBtn);


        // 4. Dynamic Editor Area
        // -------------------------------------------------------------------------
        refreshDynamicWidgets();


        // 5. Footer Buttons
        // -------------------------------------------------------------------------
        this.addRenderableWidget(Button.builder(Component.translatable("intent.gui.label.cancel"), b -> this.onClose())
                .bounds(centerX - 105, this.height - 30, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("intent.gui.label.save"), b -> save())
                .bounds(centerX + 5, this.height - 30, 100, 20).build());
    }

    /**
     * Ask the Registry for the correct editor and ask it to build its UI.
     */
    private void refreshDynamicWidgets() {
        // Clear old widgets
        for (AbstractWidget w : dynamicWidgets) {
            this.removeWidget(w);
        }
        dynamicWidgets.clear();

        // Fetch Editor from API
        this.currentEditor = ContextEditorRegistry.get(selectedType);

        // Calculate area
        int startY = 150;
        int width = 200;
        int x = this.width / 2 - 100;

        // Init Editor
        this.currentEditor.init(this, x, startY, width, widget -> {
            this.addRenderableWidget(widget);
            this.dynamicWidgets.add(widget);
        });

        // Populate if we are editing an existing rule of the same type
        if (existingEntry != null && existingEntry.context().getType() == selectedType) {
            populateEditorUnchecked(existingEntry.context());
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends IIntentContext> void populateEditorUnchecked(IIntentContext context) {
        // Safe cast because we checked getType() == selectedType
        ((IContextEditor<T>) currentEditor).populate((T) context);
    }

    private void save() {
        try {
            if (selectedActionId.isEmpty()) return;

            int priority = Integer.parseInt(priorityBox.getValue());

            // Delegate Context creation to the API
            IIntentContext context = currentEditor.buildContext();

            if (context != null) {
                onComplete.accept(new IntentProfile.IntentEntry(selectedActionId, context, priority));
                this.onClose();
            }
        } catch (Exception e) {
            // Ignore invalid inputs or parsing errors
        }
    }

    // --- Helper Logic ---

    private void updateVirtualizeButtonState() {
        if (virtualizeBtn == null) return;
        KeyMapping mapping = KeyMappingHelper.getMapping(selectedActionId);
        virtualizeBtn.active = (mapping != null && !mapping.isUnbound());
    }

    private void virtualizeTarget() {
        KeyMapping mapping = KeyMappingHelper.getMapping(selectedActionId);
        if (mapping != null) {
            mapping.setKey(InputConstants.UNKNOWN);
            this.minecraft.options.save();
            KeyMapping.resetMapping();
            updateVirtualizeButtonState();
        }
    }

    private Component formatContextName(IIntentContext.ContextType<?> type) {
        ResourceLocation key = IntentRegistries.CONTEXT_TYPE_REGISTRY.getKey(type);
        if (key == null) return Component.translatable("intent.gui.label.unknown");
        // Convert "intent:health_threshold" -> "Health Threshold"
        String name = key.getPath().replace("_", " ");
        // Capitalize first letter
        return Component.literal(name.substring(0, 1).toUpperCase() + name.substring(1));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}