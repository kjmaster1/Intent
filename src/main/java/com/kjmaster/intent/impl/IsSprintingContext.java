package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.player.LocalPlayer;

public record IsSprintingContext() implements IIntentContext {

    public static final MapCodec<IsSprintingContext> CODEC = MapCodec.unit(new IsSprintingContext());

    @Override
    public boolean test(LocalPlayer player) {
        return player.isSprinting();
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.IS_SPRINTING.get();
    }
}