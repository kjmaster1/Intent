package com.kjmaster.intent.impl;

import com.kjmaster.intent.api.IIntentContext;
import com.kjmaster.intent.registry.IntentRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record CompositeContext(Operator operator, List<IIntentContext> children) implements IIntentContext {

    // Helper Codec to decode the list of children
    private static final Codec<IIntentContext> CHILD_CODEC = IntentRegistries.CONTEXT_TYPE_REGISTRY.byNameCodec()
            .dispatch(IIntentContext::getType, ContextType::codec);

    public static final MapCodec<CompositeContext> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Operator.CODEC.fieldOf("operator").forGetter(CompositeContext::operator),
                    CHILD_CODEC.listOf().fieldOf("children").forGetter(CompositeContext::children)
            ).apply(instance, CompositeContext::new)
    );

    @Override
    public boolean test(LocalPlayer player) {
        if (children.isEmpty()) return true;

        if (operator == Operator.AND) {
            // All children must be true
            return children.stream().allMatch(ctx -> ctx.test(player));
        } else if (operator == Operator.OR) {
            // At least one child must be true
            return children.stream().anyMatch(ctx -> ctx.test(player));
        } else if (operator == Operator.NOT) {
            // None of the children must be true
            return children.stream().noneMatch(ctx -> ctx.test(player));
        }
        return false;
    }

    @Override
    public ContextType<?> getType() {
        return ContextTypes.COMPOSITE.get();
    }

    public enum Operator implements StringRepresentable {
        AND("AND"),
        OR("OR"),
        NOT("NOT");

        public static final Codec<Operator> CODEC = StringRepresentable.fromEnum(Operator::values);
        private final String name;

        Operator(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}