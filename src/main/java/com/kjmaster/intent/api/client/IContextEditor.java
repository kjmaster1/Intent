package com.kjmaster.intent.api.client;

import com.kjmaster.intent.api.IIntentContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

public interface IContextEditor<T extends IIntentContext> {

    /**
     * Called when the screen initializes or when this context type is selected.
     * Use this to create and position your widgets.
     *
     * @param screen         The parent screen (for width/height calculations).
     * @param x              The X position where the editor area starts.
     * @param y              The Y position where the editor area starts.
     * @param width          The width of the editor area.
     * @param widgetConsumer Call this to add your widgets to the screen.
     */
    void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer);

    /**
     * Called when the user clicks "Save".
     * You should read the state of your widgets and return a new Context instance.
     * Return null if the input is invalid.
     */
    T buildContext();

    /**
     * Called when loading an existing rule.
     * You should populate your widgets with values from the provided context.
     */
    void populate(T context);
}