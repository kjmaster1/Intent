package com.kjmaster.intent.impl;

import com.kjmaster.intent.Config;
import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;

import java.util.Optional;

public record InCombatContext(Optional<Integer> timeoutOverride) implements IIntentContext {

    public static final MapCodec<InCombatContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("timeout").forGetter(InCombatContext::timeoutOverride)
            ).apply(instance, InCombatContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        // Use override if present, otherwise use Global Config
        int timeoutSeconds = timeoutOverride.orElse(Config.IN_COMBAT_TIMEOUT.get());

        // Convert seconds to ticks
        int threshold = timeoutSeconds * 20;
        int lastHurtBy = player.tickCount - player.getLastHurtByMobTimestamp();
        int lastAttacked = player.tickCount - player.getLastHurtMobTimestamp();

        // Check if either even happened recently
        boolean wasHurtRecently = lastHurtBy < threshold;
        boolean attackedRecently = lastAttacked < threshold;

        return wasHurtRecently || attackedRecently;
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.IN_COMBAT.get();
    }
}