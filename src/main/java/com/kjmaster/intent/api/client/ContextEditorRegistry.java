package com.kjmaster.intent.api.client;

import com.kjmaster.intent.api.IIntentContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ContextEditorRegistry {

    private static final Map<IIntentContext.ContextType<?>, Supplier<IContextEditor<?>>> EDITORS = new HashMap<>();

    /**
     * Register a UI editor for a specific ContextType.
     */
    public static <T extends IIntentContext> void register(IIntentContext.ContextType<T> type, Supplier<IContextEditor<T>> factory) {
        EDITORS.put(type, (Supplier) factory);
    }

    public static IContextEditor<?> get(IIntentContext.ContextType<?> type) {
        Supplier<IContextEditor<?>> factory = EDITORS.get(type);
        return factory != null ? factory.get() : new EmptyEditor();
    }

    // Fallback for types with no registered editor (e.g. Sneaking)
    private static class EmptyEditor implements IContextEditor<IIntentContext> {
        @Override
        public void init(Screen screen, int x, int y, int width, Consumer<AbstractWidget> widgetConsumer) {
        }

        @Override
        public IIntentContext buildContext() {
            return null;
        } // Or handle gracefully

        @Override
        public void populate(IIntentContext context) {
        }
    }
}