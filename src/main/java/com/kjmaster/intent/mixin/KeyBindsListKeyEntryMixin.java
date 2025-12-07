package com.kjmaster.intent.mixin;

import com.kjmaster.intent.Intent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsList.KeyEntry.class)
public class KeyBindsListKeyEntryMixin {

    @Shadow
    @Final
    private KeyMapping key;
    @Shadow
    @Final
    private Button changeButton;

    @Inject(method = "refreshEntry", at = @At("TAIL"))
    private void intent$updateButtonText(CallbackInfo ci) {
        // If the key is unbound in Vanilla...
        if (this.key.isUnbound()) {
            // ...check if Intent is using it as an Action in ANY active profile.
            boolean isUsedByIntent = Intent.DATA_MANAGER.getMasterProfile().bindings().stream()
                    .flatMap(b -> b.stack().stream())
                    .anyMatch(entry -> entry.actionId().equals(this.key.getName()));

            if (isUsedByIntent) {
                // Set text to "Intent" in Gold color to signify it's managed externally
                this.changeButton.setMessage(Component.literal("Intent").withStyle(net.minecraft.ChatFormatting.GOLD));
            }
        }
    }
}