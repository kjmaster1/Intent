package com.kjmaster.intent.registry;

import com.kjmaster.intent.Intent;
import com.kjmaster.intent.api.IIntentContext;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class IntentRegistries {

    public static final ResourceKey<Registry<IIntentContext.ContextType<?>>> CONTEXT_TYPE_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Intent.MODID, "context_types"));

    public static final DeferredRegister<IIntentContext.ContextType<?>> CONTEXT_TYPES =
            DeferredRegister.create(CONTEXT_TYPE_KEY, Intent.MODID);

    public static final Registry<IIntentContext.ContextType<?>> CONTEXT_TYPE_REGISTRY =
            CONTEXT_TYPES.makeRegistry(builder -> {
            });

    public static void register(IEventBus modEventBus) {
        CONTEXT_TYPES.register(modEventBus);
    }
}