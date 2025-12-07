package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public record LookingAtBlockContext(Optional<Block> block, Optional<TagKey<Block>> tag) implements IIntentContext {

    public static final MapCodec<LookingAtBlockContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(LookingAtBlockContext::block),
                    TagKey.codec(Registries.BLOCK).optionalFieldOf("tag").forGetter(LookingAtBlockContext::tag)
            ).apply(instance, LookingAtBlockContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        HitResult hit = Minecraft.getInstance().hitResult;

        // Ensure we are actually looking at a block (not air/entity/nothing)
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        var state = player.level().getBlockState(blockHit.getBlockPos());

        // Check Specific Block ID (if defined in JSON)
        if (block.isPresent() && !state.is(block.get())) {
            return false;
        }

        // Check Tag (if defined in JSON)
        return tag.isEmpty() || state.is(tag.get());
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.LOOKING_AT_BLOCK.get();
    }
}