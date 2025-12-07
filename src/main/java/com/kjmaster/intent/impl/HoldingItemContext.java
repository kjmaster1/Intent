package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public record HoldingItemContext(Item item) implements IIntentContext {

    public static final MapCodec<HoldingItemContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(HoldingItemContext::item)
            ).apply(instance, HoldingItemContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        // Check both hands
        return player.getMainHandItem().is(this.item) || player.getOffhandItem().is(this.item);
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.HOLDING_ITEM.get();
    }
}