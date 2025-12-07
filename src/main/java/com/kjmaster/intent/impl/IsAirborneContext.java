package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.player.LocalPlayer;

public record IsAirborneContext() implements IIntentContext {

    public static final MapCodec<IsAirborneContext> CODEC = MapCodec.unit(new IsAirborneContext());

    @Override
    public boolean test(LocalPlayer player) {
        // onGround() is true if the player is touching the floor.
        // We return true if they are NOT on the ground.
        return !player.onGround();
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.IS_AIRBORNE.get();
    }
}