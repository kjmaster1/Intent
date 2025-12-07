package com.kjmaster.intent.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {

    // Allows us to READ the click count
    @Accessor("clickCount")
    int intent$getClickCount();

    // Allows us to WRITE the click count
    @Accessor("clickCount")
    void intent$setClickCount(int count);
}