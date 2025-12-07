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

        // 1. Hurt by Mobs (Specific Aggressor)
        int lastHurtBy = player.tickCount - player.getLastHurtByMobTimestamp();

        // 2. Attacking Mobs (Active Aggression)
        int lastAttacked = player.tickCount - player.getLastHurtMobTimestamp();

        boolean wasHurtByMob = lastHurtBy < threshold;
        boolean attackedRecently = lastAttacked < threshold;

        // Return TRUE if any of these conditions are met
        return wasHurtByMob || attackedRecently;
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.IN_COMBAT.get();
    }
}