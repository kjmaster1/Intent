package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.player.LocalPlayer;

public record SneakingContext() implements IIntentContext {

    public static final MapCodec<SneakingContext> CODEC = MapCodec.unit(new SneakingContext());

    @Override
    public boolean test(LocalPlayer player) {
        return player.isShiftKeyDown();
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.SNEAKING.get();
    }
}