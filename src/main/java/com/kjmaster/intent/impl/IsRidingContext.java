package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.player.LocalPlayer;

public record IsRidingContext() implements IIntentContext {

    public static final MapCodec<IsRidingContext> CODEC = MapCodec.unit(new IsRidingContext());

    @Override
    public boolean test(LocalPlayer player) {
        return player.isPassenger();
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.IS_RIDING.get();
    }
}