package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;

public record HealthThresholdContext(float value, boolean isPercentage, boolean invert) implements IIntentContext {

    public static final MapCodec<HealthThresholdContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("value").forGetter(HealthThresholdContext::value),
                    Codec.BOOL.optionalFieldOf("is_percentage", true).forGetter(HealthThresholdContext::isPercentage),
                    Codec.BOOL.optionalFieldOf("invert", false).forGetter(HealthThresholdContext::invert)
            ).apply(instance, HealthThresholdContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        float health = player.getHealth();
        float max = player.getMaxHealth();

        boolean passed;
        if (isPercentage) {
            // e.g., value 0.3 (30%)
            // if health (5.0) / max (20.0) = 0.25. 0.25 < 0.3 -> True (Low Health)
            passed = (health / max) < value;
        } else {
            // Absolute hearts (e.g. value 4.0 = 2 hearts)
            passed = health < value;
        }

        // Invert allows checking "Health > X" (Healthy) instead of "Health < X" (Hurt)
        return invert ? !passed : passed;
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.HEALTH_THRESHOLD.get();
    }
}