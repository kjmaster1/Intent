package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public record HasBlockEntityContext() implements IIntentContext {

    public static final MapCodec<HasBlockEntityContext> CODEC = MapCodec.unit(new HasBlockEntityContext());

    @Override
    public boolean test(LocalPlayer player) {
        HitResult hit = Minecraft.getInstance().hitResult;

        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;

        return player.level().getBlockEntity(blockHit.getBlockPos()) != null;
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.HAS_BLOCK_ENTITY.get();
    }
}