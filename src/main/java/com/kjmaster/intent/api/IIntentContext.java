package com.kjmaster.intent.api;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.player.LocalPlayer;

public interface IIntentContext {
    /**
     * Checks if this context condition is currently met.
     *
     * @param player The client player instance.
     * @return true if the condition is active.
     */
    boolean test(LocalPlayer player);

    /**
     * Returns the Type of this context, used for serialization (saving/loading).
     */
    ContextType<?> getType();

    /**
     * Helper interface for the Codec system.
     */
    interface ContextType<T extends IIntentContext> {
        MapCodec<T> codec();
    }
}