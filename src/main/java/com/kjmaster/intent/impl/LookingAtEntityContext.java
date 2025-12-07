package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public record LookingAtEntityContext(Optional<EntityType<?>> entityType, Optional<TagKey<EntityType<?>>> tag) implements IIntentContext {

    public static final MapCodec<LookingAtEntityContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("entity_id").forGetter(LookingAtEntityContext::entityType),
                    TagKey.codec(Registries.ENTITY_TYPE).optionalFieldOf("tag").forGetter(LookingAtEntityContext::tag)
            ).apply(instance, LookingAtEntityContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        HitResult hit = Minecraft.getInstance().hitResult;

        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            return false;
        }

        Entity target = ((EntityHitResult) hit).getEntity();
        var type = target.getType();

        if (entityType.isPresent() && type != entityType.get()) {
            return false;
        }

        return tag.isEmpty() || type.is(tag.get());
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.LOOKING_AT_ENTITY.get();
    }
}