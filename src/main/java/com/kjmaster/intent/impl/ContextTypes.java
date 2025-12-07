package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.kjmaster.intent.registry.IntentRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ContextTypes {

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<SneakingContext>> SNEAKING =
            IntentRegistries.CONTEXT_TYPES.register("is_sneaking", () -> () -> SneakingContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<HoldingItemContext>> HOLDING_ITEM =
            IntentRegistries.CONTEXT_TYPES.register("holding_item", () -> () -> HoldingItemContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<LookingAtBlockContext>> LOOKING_AT_BLOCK =
            IntentRegistries.CONTEXT_TYPES.register("looking_at_block", () -> () -> LookingAtBlockContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<LookingAtEntityContext>> LOOKING_AT_ENTITY =
            IntentRegistries.CONTEXT_TYPES.register("looking_at_entity", () -> () -> LookingAtEntityContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<CompositeContext>> COMPOSITE =
            IntentRegistries.CONTEXT_TYPES.register("composite", () -> () -> CompositeContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<GuiIsOpenContext>> GUI_IS_OPEN =
            IntentRegistries.CONTEXT_TYPES.register("gui_is_open", () -> () -> GuiIsOpenContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<HoldingItemTagContext>> HOLDING_TAG =
            IntentRegistries.CONTEXT_TYPES.register("holding_item_tag", () -> () -> HoldingItemTagContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<InCombatContext>> IN_COMBAT =
            IntentRegistries.CONTEXT_TYPES.register("in_combat", () -> () -> InCombatContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<HealthThresholdContext>> HEALTH_THRESHOLD =
            IntentRegistries.CONTEXT_TYPES.register("health_threshold", () -> () -> HealthThresholdContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<IsSprintingContext>> IS_SPRINTING =
            IntentRegistries.CONTEXT_TYPES.register("is_sprinting", () -> () -> IsSprintingContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<IsAirborneContext>> IS_AIRBORNE =
            IntentRegistries.CONTEXT_TYPES.register("is_airborne", () -> () -> IsAirborneContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<IsRidingContext>> IS_RIDING =
            IntentRegistries.CONTEXT_TYPES.register("is_riding", () -> () -> IsRidingContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<DurabilityThresholdContext>> DURABILITY_THRESHOLD =
            IntentRegistries.CONTEXT_TYPES.register("durability_threshold", () -> () -> DurabilityThresholdContext.CODEC);

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<HasBlockEntityContext>> HAS_BLOCK_ENTITY =
            IntentRegistries.CONTEXT_TYPES.register("has_block_entity", () -> () -> HasBlockEntityContext.CODEC);

    public static void register() {
        // Calling this class triggers the static initializers above
    }
}