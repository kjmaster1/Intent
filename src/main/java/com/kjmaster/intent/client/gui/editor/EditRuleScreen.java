package com.kjmaster.intent.client.gui.editor;

import com.kjmaster.intent.api.IIntentContext;
import com.kjmaster.intent.data.IntentProfile;
import com.kjmaster.intent.impl.*;
import com.kjmaster.intent.registry.IntentRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class EditRuleScreen extends Screen {

    private final Screen parent;
    private final Consumer<IntentProfile.IntentEntry> onComplete;
    private final IntentProfile.IntentEntry existingEntry;

    private EditBox actionBox;
    private EditBox priorityBox;

    // Dynamic Fields
    private EditBox valueBox;
    private CycleButton<Boolean> boolBtn1;
    private CycleButton<Boolean> boolBtn2;

    private IIntentContext.ContextType<?> selectedType;

    public EditRuleScreen(Screen parent, IntentProfile.IntentEntry existing, Consumer<IntentProfile.IntentEntry> onComplete) {
        super(Component.translatable("intent.gui.label.edit_rule"));
        this.parent = parent;
        this.existingEntry = existing;
        this.onComplete = onComplete;

        if (existing != null) {
            this.selectedType = existing.context().getType();
        } else {
            this.selectedType = ContextTypes.SNEAKING.get();
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;

        // 1. Action ID
        this.addRenderableWidget(Button.builder(Component.translatable("intent.gui.label.action_id.button"), b -> {
                })
                .bounds(centerX - 100, startY - 12, 200, 10).build()).active = false;
        this.actionBox = new EditBox(this.font, centerX - 100, startY, 200, 20, Component.translatable("intent.gui.label.action_id"));
        this.actionBox.setMaxLength(256);
        if (existingEntry != null) this.actionBox.setValue(existingEntry.actionId());
        this.addRenderableWidget(actionBox);

        startY += 35;

        // 2. Priority
        this.addRenderableWidget(Button.builder(Component.translatable("intent.gui.label.priority.button"), b -> {
                })
                .bounds(centerX - 100, startY - 12, 200, 10).build()).active = false;
        this.priorityBox = new EditBox(this.font, centerX - 100, startY, 200, 20, Component.translatable("intent.gui.label.priority"));
        this.priorityBox.setValue(existingEntry != null ? String.valueOf(existingEntry.priority()) : "10");
        this.addRenderableWidget(priorityBox);

        startY += 35;

        // 3. Context Type Selector
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
                    refreshDynamicWidgets();
                });
        this.addRenderableWidget(contextTypeBtn);

        startY += 35;

        // 4. Dynamic Widgets (Value, Bools)
        this.valueBox = new EditBox(this.font, centerX - 100, startY, 200, 20, Component.translatable("intent.gui.label.value"));
        this.boolBtn1 = CycleButton.onOffBuilder().create(centerX - 100, startY + 25, 95, 20, Component.translatable("intent.gui.label.flag_1"), (b, v) -> {
        });
        this.boolBtn2 = CycleButton.onOffBuilder().create(centerX + 5, startY + 25, 95, 20, Component.translatable("intent.gui.label.flag_2"), (b, v) -> {
        });

        this.addRenderableWidget(valueBox);
        this.addRenderableWidget(boolBtn1);
        this.addRenderableWidget(boolBtn2);

        refreshDynamicWidgets();

        // 5. Footer Buttons
        this.addRenderableWidget(Button.builder(Component.translatable("intent.gui.label.cancel"), b -> this.onClose())
                .bounds(centerX - 105, this.height - 30, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("intent.gui.label.save"), b -> save())
                .bounds(centerX + 5, this.height - 30, 100, 20).build());
    }

    private Component formatContextName(IIntentContext.ContextType<?> type) {
        ResourceLocation key = IntentRegistries.CONTEXT_TYPE_REGISTRY.getKey(type);
        return Component.literal(key != null ? key.getPath() : "Unknown");
    }

    private void refreshDynamicWidgets() {
        valueBox.visible = false;
        boolBtn1.visible = false;
        boolBtn2.visible = false;

        if (selectedType == ContextTypes.HEALTH_THRESHOLD.get() || selectedType == ContextTypes.DURABILITY_THRESHOLD.get()) {
            valueBox.visible = true;
            valueBox.setHint(Component.translatable("intent.editor.rule.hint.value_threshold"));

            boolBtn1.visible = true;
            boolBtn1.setMessage(Component.translatable("intent.editor.rule.toggle.is_percentage"));

            boolBtn2.visible = true;
            boolBtn2.setMessage(Component.translatable("intent.editor.rule.toggle.invert"));

        } else if (selectedType == ContextTypes.GUI_IS_OPEN.get()) {
            valueBox.visible = true;
            valueBox.setHint(Component.translatable("intent.editor.rule.hint.screen_class"));

        } else if (selectedType == ContextTypes.HOLDING_ITEM.get()) {
            valueBox.visible = true;
            valueBox.setHint(Component.translatable("intent.editor.rule.hint.item_id"));

        } else if (selectedType == ContextTypes.LOOKING_AT_BLOCK.get()) {
            valueBox.visible = true;
            valueBox.setHint(Component.translatable("intent.editor.rule.hint.block_id"));

        } else if (selectedType == ContextTypes.IN_COMBAT.get()) {
            valueBox.visible = true;
            valueBox.setHint(Component.translatable("intent.editor.rule.hint.combat_timeout"));
        }

        if (existingEntry != null && existingEntry.context().getType() == selectedType) {
            populateFromExisting();
        }
    }

    private void populateFromExisting() {
        IIntentContext ctx = existingEntry.context();

        if (ctx instanceof HealthThresholdContext(float value, boolean isPercentage, boolean invert)) {
            valueBox.setValue(String.valueOf(value));
            boolBtn1.setValue(isPercentage);
            boolBtn2.setValue(invert);
        } else if (ctx instanceof DurabilityThresholdContext(float threshold, boolean isPercentage, boolean invert)) {
            valueBox.setValue(String.valueOf(threshold));
            boolBtn1.setValue(isPercentage);
            boolBtn2.setValue(invert);
        } else if (ctx instanceof GuiIsOpenContext(Optional<String> screenClass)) {
            valueBox.setValue(screenClass.orElse(""));
        } else if (ctx instanceof HoldingItemContext(Item item)) {
            valueBox.setValue(BuiltInRegistries.ITEM.getKey(item).toString());
        } else if (ctx instanceof LookingAtBlockContext c) {
            c.block().ifPresent(b -> valueBox.setValue(BuiltInRegistries.BLOCK.getKey(b).toString()));
        } else if (ctx instanceof InCombatContext(Optional<Integer> timeout)) {
            timeout.ifPresent(t -> valueBox.setValue(String.valueOf(t)));
        }
    }

    private void save() {
        try {
            String action = actionBox.getValue();
            int priority = Integer.parseInt(priorityBox.getValue());
            IIntentContext context = buildContext();

            if (context != null && !action.isEmpty()) {
                onComplete.accept(new IntentProfile.IntentEntry(action, context, priority));
                this.onClose();
            }
        } catch (Exception e) {
            // Ignore invalid inputs
        }
    }

    private IIntentContext buildContext() {
        if (selectedType == ContextTypes.SNEAKING.get()) return new SneakingContext();
        if (selectedType == ContextTypes.IS_SPRINTING.get()) return new IsSprintingContext();
        if (selectedType == ContextTypes.IS_AIRBORNE.get()) return new IsAirborneContext();
        if (selectedType == ContextTypes.IS_RIDING.get()) return new IsRidingContext();
        if (selectedType == ContextTypes.HAS_BLOCK_ENTITY.get()) return new HasBlockEntityContext();

        try {
            String val = valueBox.getValue();
            if (selectedType == ContextTypes.HEALTH_THRESHOLD.get()) {
                return new HealthThresholdContext(Float.parseFloat(val), boolBtn1.getValue(), boolBtn2.getValue());
            }
            if (selectedType == ContextTypes.DURABILITY_THRESHOLD.get()) {
                return new DurabilityThresholdContext(Float.parseFloat(val), boolBtn1.getValue(), boolBtn2.getValue());
            }
            if (selectedType == ContextTypes.GUI_IS_OPEN.get()) {
                return new GuiIsOpenContext(val.isEmpty() ? Optional.empty() : Optional.of(val));
            }
            if (selectedType == ContextTypes.IN_COMBAT.get()) {
                Optional<Integer> timeout = val.isEmpty() ? Optional.empty() : Optional.of(Integer.parseInt(val));
                return new InCombatContext(timeout);
            }
            if (selectedType == ContextTypes.HOLDING_ITEM.get()) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(val));
                return new HoldingItemContext(item);
            }
            if (selectedType == ContextTypes.LOOKING_AT_BLOCK.get()) {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(val));
                return new LookingAtBlockContext(Optional.of(block), Optional.empty());
            }
        } catch (Exception e) {
            return null; // Failed to parse
        }
        return new SneakingContext(); // Fallback
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}