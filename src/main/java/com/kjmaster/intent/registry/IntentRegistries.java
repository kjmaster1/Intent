package com.kjmaster.intent.registry;

import com.kjmaster.intent.Intent;
import com.kjmaster.intent.api.IIntentContext;
import com.kjmaster.intent.api.IntentAPI;
import net.minecraft.core.Registry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class IntentRegistries {

    public static final DeferredRegister<IIntentContext.ContextType<?>> CONTEXT_TYPES =
            DeferredRegister.create(IntentAPI.CONTEXT_TYPE_REGISTRY_KEY, Intent.MODID);

    public static final Registry<IIntentContext.ContextType<?>> CONTEXT_TYPE_REGISTRY =
            CONTEXT_TYPES.makeRegistry(builder -> {
            });

    public static void register(IEventBus modEventBus) {
        CONTEXT_TYPES.register(modEventBus);
    }
}