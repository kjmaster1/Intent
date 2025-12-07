package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public record DurabilityThresholdContext(float threshold, boolean isPercentage, boolean invert) implements IIntentContext {

    public static final MapCodec<DurabilityThresholdContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("threshold").forGetter(DurabilityThresholdContext::threshold),
                    Codec.BOOL.optionalFieldOf("is_percentage", true).forGetter(DurabilityThresholdContext::isPercentage),
                    Codec.BOOL.optionalFieldOf("invert", false).forGetter(DurabilityThresholdContext::invert)
            ).apply(instance, DurabilityThresholdContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();

        // If item doesn't take damage (e.g. Stone), context always fails (or passes if inverted?)
        // Usually, we assume "No Durability" means "Safe", so we fail the "Low Durability" check.
        if (!stack.isDamageableItem()) {
            return false;
        }

        float max = stack.getMaxDamage();
        float currentDamage = stack.getDamageValue(); // 0 = New, Max = Broken
        float remaining = max - currentDamage;

        boolean passed;
        if (isPercentage) {
            // e.g., threshold 0.1 (10%)
            // If remaining (10) / max (100) = 0.1.
            passed = (remaining / max) <= threshold;
        } else {
            // Absolute value (e.g., 50 uses left)
            passed = remaining <= threshold;
        }

        // Normal: Return TRUE if durability is LOW (<= threshold)
        // Invert: Return TRUE if durability is HIGH (> threshold)
        return invert ? !passed : passed;
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.DURABILITY_THRESHOLD.get();
    }
}