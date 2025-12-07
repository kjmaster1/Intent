package com.kjmaster.intent.mixin;

import com.kjmaster.intent.client.InputHandler;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void intent$onKeyInput(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        // Delegate to our handler. If it returns true, we swallow the input.
        if (InputHandler.handleKeyInput(key, scancode, action, modifiers)) {
            ci.cancel();
        }
    }
}