package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public record HoldingItemTagContext(TagKey<Item> tag) implements IIntentContext {

    public static final MapCodec<HoldingItemTagContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(HoldingItemTagContext::tag)
            ).apply(instance, HoldingItemTagContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        return player.getMainHandItem().is(tag) || player.getOffhandItem().is(tag);
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.HOLDING_TAG.get();
    }
}