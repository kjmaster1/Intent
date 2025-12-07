package com.kjmaster.intent.data;

import com.kjmaster.intent.api.IIntentContext;
import com.kjmaster.intent.registry.IntentRegistries;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Represents a full JSON profile containing multiple key bindings.
 */
public record IntentProfile(List<Binding> bindings) {

    public static final Codec<IntentProfile> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Binding.CODEC.listOf().fieldOf("bindings").forGetter(IntentProfile::bindings)
            ).apply(instance, IntentProfile::new)
    );

    /**
     * Represents a single physical key (e.g., 'R') and its stack of logic.
     */
    public record Binding(String triggerKey, List<IntentEntry> stack) {
        public static final Codec<Binding> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("trigger").forGetter(Binding::triggerKey),
                        IntentEntry.CODEC.listOf().fieldOf("stack").forGetter(Binding::stack)
                ).apply(instance, Binding::new)
        );

        // Helper to convert the string "key.keyboard.r" into a real Input object
        public InputConstants.Key getInput() {
            return InputConstants.getKey(triggerKey);
        }
    }

    /**
     * Represents one logic node: "If [Context], then trigger [Action]".
     */
    public record IntentEntry(String actionId, IIntentContext context, int priority) {

        private static final Codec<IIntentContext> CONTEXT_CODEC = IntentRegistries.CONTEXT_TYPE_REGISTRY.byNameCodec()
                .dispatch(
                        IIntentContext::getType,
                        IIntentContext.ContextType::codec
                );

        public static final Codec<IntentEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("action_id").forGetter(IntentEntry::actionId),
                        CONTEXT_CODEC.fieldOf("context").forGetter(IntentEntry::context),
                        Codec.INT.optionalFieldOf("priority", 0).forGetter(IntentEntry::priority)
                ).apply(instance, IntentEntry::new)
        );
    }
}