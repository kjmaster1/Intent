package com.kjmaster.intent.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Invoker("startAttack")
    boolean intent$startAttack();

    @Invoker("startUseItem")
    void intent$startUseItem();
}