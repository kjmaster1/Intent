package com.kjmaster.intent.mixin;

import com.kjmaster.intent.client.gui.editor.IntentEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin extends OptionsSubScreen {

    public KeyBindsScreenMixin(Screen parent, net.minecraft.client.Options options, Component title) {
        super(parent, options, title);
    }

    @Redirect(
            method = "addFooter",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 1 // 0 is Reset Button, 1 is Done Button
            )
    )
    private LayoutElement intent$addFooterButtons(LinearLayout layout, LayoutElement originalDoneButton) {
        // 1. Add Intent Rules button first (it will appear to the left of "Done")
        layout.addChild(Button.builder(Component.translatable("intent.mixin.keybinds.screen.button.title"), button -> {
            Minecraft.getInstance().setScreen(new IntentEditorScreen());
        }).build());

        // 2. Add the original "Done" button (passed as the argument to the redirected call)
        return layout.addChild(originalDoneButton);
    }
}