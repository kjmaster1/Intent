package com.kjmaster.intent.api;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class IntentAPI {
    public static final String MOD_ID = "intent";
    public static final ResourceKey<Registry<IIntentContext.ContextType<?>>> CONTEXT_TYPE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "context_types"));
}
