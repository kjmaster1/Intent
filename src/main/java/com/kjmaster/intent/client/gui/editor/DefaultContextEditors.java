package com.kjmaster.intent.client.gui.editor;

import com.kjmaster.intent.api.IIntentContext;
import com.kjmaster.intent.api.client.ContextEditorRegistry;
import com.kjmaster.intent.api.client.IContextEditor;
import com.kjmaster.intent.impl.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DefaultContextEditors {

    public static void registerAll() {
        // Simple Boolean States
        registerSimple(ContextTypes.SNEAKING, SneakingContext::new);
        registerSimple(ContextTypes.IS_SPRINTING, IsSprintingContext::new);
        registerSimple(ContextTypes.IS_AIRBORNE, IsAirborneContext::new);
        registerSimple(ContextTypes.IS_RIDING, IsRidingContext::new);
        registerSimple(ContextTypes.HAS_BLOCK_ENTITY, HasBlockEntityContext::new);

        // Thresholds - Call .get() on DeferredHolders
        ContextEditorRegistry.register(ContextTypes.HEALTH_THRESHOLD.get(), HealthEditor::new);
        ContextEditorRegistry.register(ContextTypes.DURABILITY_THRESHOLD.get(), DurabilityEditor::new);
        ContextEditorRegistry.register(ContextTypes.IN_COMBAT.get(), InCombatEditor::new);

        // Items & GUIs - Call .get()
        ContextEditorRegistry.register(ContextTypes.HOLDING_ITEM.get(), HoldingItemEditor::new);
        ContextEditorRegistry.register(ContextTypes.HOLDING_TAG.get(), HoldingItemTagEditor::new);
        ContextEditorRegistry.register(ContextTypes.GUI_IS_OPEN.get(), GuiIsOpenEditor::new);

        // World Interaction - Call .get()
        ContextEditorRegistry.register(ContextTypes.LOOKING_AT_BLOCK.get(), LookingAtBlockEditor::new);
        ContextEditorRegistry.register(ContextTypes.LOOKING_AT_ENTITY.get(), LookingAtEntityEditor::new);

        // Composite - Call .get()
        ContextEditorRegistry.register(ContextTypes.COMPOSITE.get(), CompositeEditor::new);
    }

    private static <T extends IIntentContext> void registerSimple(
            Supplier<IIntentContext.ContextType<T>> type,
            Supplier<T> factory
    ) {
        // Call type.get() to extract the ContextType from the Supplier/DeferredHolder
        ContextEditorRegistry.register(type.get(), () -> new SimpleStateEditor<>(factory));
    }

    // ==========================================
    // 1. SIMPLE STATE EDITOR (No Config)
    // ==========================================
    public static class SimpleStateEditor<T extends IIntentContext> implements IContextEditor<T> {
        private final Supplier<T> factory;

        public SimpleStateEditor(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            widgetConsumer.accept(new StringWidget(x, y, width, 20, Component.translatable("intent.gui.label.no_config"), Minecraft.getInstance().font));
        }

        @Override
        public T buildContext() {
            return factory.get();
        }

        @Override
        public void populate(T context) {
            // Nothing to populate
        }
    }

    // ==========================================
    // 2. HEALTH EDITOR
    // ==========================================
    public static class HealthEditor implements IContextEditor<HealthThresholdContext> {
        private EditBox valueBox;
        private CycleButton<Boolean> isPercentageBtn;
        private CycleButton<Boolean> isInvertedBtn;

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            this.valueBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.translatable("intent.gui.label.value"));
            this.valueBox.setHint(Component.translatable("intent.editor.rule.hint.value_threshold"));

            this.isPercentageBtn = CycleButton.onOffBuilder().create(x, y + 25, width / 2 - 5, 20,
                    Component.translatable("intent.editor.rule.toggle.is_percentage"), (b, v) -> {
                    });

            this.isInvertedBtn = CycleButton.onOffBuilder().create(x + width / 2 + 5, y + 25, width / 2 - 5, 20,
                    Component.translatable("intent.editor.rule.toggle.invert"), (b, v) -> {
                    });

            widgetConsumer.accept(valueBox);
            widgetConsumer.accept(isPercentageBtn);
            widgetConsumer.accept(isInvertedBtn);
        }

        @Override
        public HealthThresholdContext buildContext() {
            try {
                float val = Float.parseFloat(valueBox.getValue());
                return new HealthThresholdContext(val, isPercentageBtn.getValue(), isInvertedBtn.getValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public void populate(HealthThresholdContext context) {
            valueBox.setValue(String.valueOf(context.value()));
            isPercentageBtn.setValue(context.isPercentage());
            isInvertedBtn.setValue(context.invert());
        }
    }

    // ==========================================
    // 3. DURABILITY EDITOR
    // ==========================================
    public static class DurabilityEditor implements IContextEditor<DurabilityThresholdContext> {
        private EditBox valueBox;
        private CycleButton<Boolean> isPercentageBtn;
        private CycleButton<Boolean> isInvertedBtn;

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            this.valueBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.translatable("intent.gui.label.value"));
            this.valueBox.setHint(Component.translatable("intent.editor.rule.hint.value_threshold"));

            this.isPercentageBtn = CycleButton.onOffBuilder().create(x, y + 25, width / 2 - 5, 20,
                    Component.translatable("intent.editor.rule.toggle.is_percentage"), (b, v) -> {
                    });

            this.isInvertedBtn = CycleButton.onOffBuilder().create(x + width / 2 + 5, y + 25, width / 2 - 5, 20,
                    Component.translatable("intent.editor.rule.toggle.invert"), (b, v) -> {
                    });

            widgetConsumer.accept(valueBox);
            widgetConsumer.accept(isPercentageBtn);
            widgetConsumer.accept(isInvertedBtn);
        }

        @Override
        public DurabilityThresholdContext buildContext() {
            try {
                float val = Float.parseFloat(valueBox.getValue());
                return new DurabilityThresholdContext(val, isPercentageBtn.getValue(), isInvertedBtn.getValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public void populate(DurabilityThresholdContext context) {
            valueBox.setValue(String.valueOf(context.threshold()));
            isPercentageBtn.setValue(context.isPercentage());
            isInvertedBtn.setValue(context.invert());
        }
    }

    // ==========================================
    // 4. IN COMBAT EDITOR
    // ==========================================
    public static class InCombatEditor implements IContextEditor<InCombatContext> {
        private EditBox valueBox;

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            this.valueBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.translatable("intent.gui.label.timeout"));
            this.valueBox.setHint(Component.translatable("intent.editor.rule.hint.combat_timeout"));
            widgetConsumer.accept(valueBox);
        }

        @Override
        public InCombatContext buildContext() {
            String val = valueBox.getValue();
            if (val.isEmpty()) return new InCombatContext(Optional.empty());
            try {
                return new InCombatContext(Optional.of(Integer.parseInt(val)));
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public void populate(InCombatContext context) {
            context.timeoutOverride().ifPresent(i -> valueBox.setValue(String.valueOf(i)));
        }
    }

    // ==========================================
    // 5. HOLDING ITEM EDITOR
    // ==========================================
    public static class HoldingItemEditor implements IContextEditor<HoldingItemContext> {
        private EditBox itemBox;

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            this.itemBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.translatable("intent.gui.label.item"));
            this.itemBox.setHint(Component.translatable("intent.editor.rule.hint.item_id"));
            widgetConsumer.accept(itemBox);
        }

        @Override
        public HoldingItemContext buildContext() {
            ResourceLocation loc = ResourceLocation.tryParse(itemBox.getValue());
            if (loc == null) return null;
            Item item = BuiltInRegistries.ITEM.get(loc);
            return new HoldingItemContext(item);
        }

        @Override
        public void populate(HoldingItemContext context) {
            itemBox.setValue(BuiltInRegistries.ITEM.getKey(context.item()).toString());
        }
    }

    // ==========================================
    // 6. HOLDING TAG EDITOR
    // ==========================================
    public static class HoldingItemTagEditor implements IContextEditor<HoldingItemTagContext> {
        private EditBox tagBox;

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            this.tagBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.translatable("intent.gui.label.tag"));
            this.tagBox.setHint(Component.translatable("intent.gui.hint.item.tag"));
            widgetConsumer.accept(tagBox);
        }

        @Override
        public HoldingItemTagContext buildContext() {
            ResourceLocation loc = ResourceLocation.tryParse(tagBox.getValue());
            if (loc == null) return null;
            TagKey<Item> tag = TagKey.create(Registries.ITEM, loc);
            return new HoldingItemTagContext(tag);
        }

        @Override
        public void populate(HoldingItemTagContext context) {
            tagBox.setValue(context.tag().location().toString());
        }
    }

    // ==========================================
    // 7. GUI IS OPEN EDITOR
    // ==========================================
    public static class GuiIsOpenEditor implements IContextEditor<GuiIsOpenContext> {
        private EditBox classBox;
        private CycleButton<Boolean> strictBtn;

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            this.classBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.translatable("intent.gui.label.class"));
            this.classBox.setHint(Component.translatable("intent.editor.rule.hint.screen_class"));

            this.strictBtn = CycleButton.onOffBuilder(false).create(x, y + 25, width, 20,
                    Component.translatable("intent.gui.label.strict"), (b, v) -> {});

            widgetConsumer.accept(classBox);
        }

        @Override
        public GuiIsOpenContext buildContext() {
            String val = classBox.getValue();
            return new GuiIsOpenContext(val.isEmpty() ? Optional.empty() : Optional.of(val), strictBtn.getValue());
        }

        @Override
        public void populate(GuiIsOpenContext context) {
            context.screenClass().ifPresent(classBox::setValue);
            strictBtn.setValue(context.strict());
        }
    }

    // ==========================================
    // 8. LOOKING AT BLOCK EDITOR
    // ==========================================
    public static class LookingAtBlockEditor implements IContextEditor<LookingAtBlockContext> {
        private EditBox blockBox;
        private EditBox tagBox;

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            this.blockBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.translatable("intent.gui.label.block"));
            this.blockBox.setHint(Component.translatable("intent.gui.hint.block"));

            this.tagBox = new EditBox(Minecraft.getInstance().font, x, y + 25, width, 20, Component.translatable("intent.gui.label.tag"));
            this.tagBox.setHint(Component.translatable("intent.gui.hint.block.tag"));

            widgetConsumer.accept(blockBox);
            widgetConsumer.accept(tagBox);
        }

        @Override
        public LookingAtBlockContext buildContext() {
            Optional<Block> block = Optional.empty();
            if (!blockBox.getValue().isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(blockBox.getValue());
                if (loc != null) block = Optional.of(BuiltInRegistries.BLOCK.get(loc));
            }

            Optional<TagKey<Block>> tag = Optional.empty();
            if (!tagBox.getValue().isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(tagBox.getValue());
                if (loc != null) tag = Optional.of(TagKey.create(Registries.BLOCK, loc));
            }

            return new LookingAtBlockContext(block, tag);
        }

        @Override
        public void populate(LookingAtBlockContext context) {
            context.block().ifPresent(b -> blockBox.setValue(BuiltInRegistries.BLOCK.getKey(b).toString()));
            context.tag().ifPresent(t -> tagBox.setValue(t.location().toString()));
        }
    }

    // ==========================================
    // 9. LOOKING AT ENTITY EDITOR
    // ==========================================
    public static class LookingAtEntityEditor implements IContextEditor<LookingAtEntityContext> {
        private EditBox entityBox;
        private EditBox tagBox;

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            this.entityBox = new EditBox(Minecraft.getInstance().font, x, y, width, 20, Component.translatable("intent.gui.label.entity"));
            this.entityBox.setHint(Component.translatable("intent.gui.hint.entity"));

            this.tagBox = new EditBox(Minecraft.getInstance().font, x, y + 25, width, 20, Component.literal("Tag"));
            this.tagBox.setHint(Component.translatable("intent.gui.hint.entity.tag"));

            widgetConsumer.accept(entityBox);
            widgetConsumer.accept(tagBox);
        }

        @Override
        public LookingAtEntityContext buildContext() {
            Optional<EntityType<?>> entity = Optional.empty();
            if (!entityBox.getValue().isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(entityBox.getValue());
                if (loc != null) entity = Optional.of(BuiltInRegistries.ENTITY_TYPE.get(loc));
            }

            Optional<TagKey<EntityType<?>>> tag = Optional.empty();
            if (!tagBox.getValue().isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(tagBox.getValue());
                if (loc != null) tag = Optional.of(TagKey.create(Registries.ENTITY_TYPE, loc));
            }

            return new LookingAtEntityContext(entity, tag);
        }

        @Override
        public void populate(LookingAtEntityContext context) {
            context.entityType().ifPresent(e -> entityBox.setValue(BuiltInRegistries.ENTITY_TYPE.getKey(e).toString()));
            context.tag().ifPresent(t -> tagBox.setValue(t.location().toString()));
        }
    }

    // ==========================================
    // 10. COMPOSITE EDITOR (Basic)
    // ==========================================
    public static class CompositeEditor implements IContextEditor<CompositeContext> {
        private CycleButton<CompositeContext.Operator> opBtn;
        private java.util.List<IIntentContext> existingChildren = java.util.Collections.emptyList();

        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
            // Convert Operator to Component properly for the CycleButton builder
            this.opBtn = CycleButton.builder((CompositeContext.Operator op) -> Component.literal(op.getSerializedName()))
                    // Pass a List, not an Array
                    .withValues(List.of(CompositeContext.Operator.values()))
                    .withInitialValue(CompositeContext.Operator.AND)
                    .create(x, y, width, 20, Component.translatable("intent.gui.label.operator"), (b, v) -> {
                    });

            widgetConsumer.accept(opBtn);

            StringWidget warning = new StringWidget(x, y + 25, width, 20,
                    Component.translatable("intent.gui.editor.composite.warning"), Minecraft.getInstance().font);
            warning.setColor(0xFFAAAA);
            widgetConsumer.accept(warning);
        }

        @Override
        public CompositeContext buildContext() {
            // Preserves existing children, only updates Operator
            return new CompositeContext(opBtn.getValue(), existingChildren);
        }

        @Override
        public void populate(CompositeContext context) {
            opBtn.setValue(context.operator());
            this.existingChildren = context.children();
        }
    }
}