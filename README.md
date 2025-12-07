# Intent
**Context-Aware Keybinds for Minecraft**

Intent transforms your keybinds into intelligent, context-aware actions. Instead of remembering dozens of keys for different mods, you can assign multiple functions to a single key, triggered automatically by specific conditions like your health, equipment, or movement state.

## Features

* **Contextual Bindings:** Bind a single physical key (e.g., `R`) to perform different actions based on the situation.
    * *Example:* `R` throws an Ender Pearl if holding one, but opens your Inventory if your hand is empty.
* **Radial Menu System:** If multiple actions are valid for a key press, holding the key opens a Radial Menu to select the desired action. Tapping the key triggers the highest priority action immediately.
* **In-Game Editor:** No need to edit JSON files manually. Press `I` (default) to open the **Intent Editor** to configure your rules visually.
* **Default Condition Library:**
    * **Player State:** Sneaking, Sprinting, Airborne, Riding.
    * **Inventory:** Holding specific Items or Item Tags.
    * **World:** Looking at specific Blocks or Entities.
    * **Stats:** Health or Item Durability thresholds (e.g., "Eat food when health < 50%").
    * **Combat:** Detect if you are currently in combat.
    * **GUI:** Detect if a specific screen (like an Inventory) is open.

## How to Use

### The Editor
Press `I` to open the Intent Editor.
1.  **Select a Key:** Choose a physical key (e.g., `Key R`) on the left.
2.  **Add Rules:** Add "Intent Entries" to the stack.
3.  **Define Contexts:** For each entry, define **When** it should trigger (e.g., `is_sneaking`).
4.  **Set Priority:** Rules are checked from top to bottom. Use the Up/Down arrows to prioritize specific actions.

### Virtualization
Intent allows you to "Virtualize" a keybinding. This unbinds the action from the vanilla controls options so it doesn't conflict with your custom Intent rules.

---

## Developer API

Intent exports a dedicated API jar, allowing other mods to add custom **Context Types** (conditions) and **Editors**.

### Usage (Gradle)
Intent is available via JitPack. Add the following to your `build.gradle`:

```groovy
repositories {
    maven { url '[https://jitpack.io](https://jitpack.io)' }
}

dependencies {
    // Compile against the API (Recommended)
    implementation "com.github.kjmaster1:Intent:v1.0.0:api"

    // Or include the full mod for runtime testing
    // implementation "com.github.kjmaster1:Intent:v1.0.0"
}
```

### Registering a Custom Context
To add a new condition (e.g., checking if it's raining), implement `IIntentContext` and register a `ContextType`.

1.  **Create the Context:**
    ```java
    public record RainingContext() implements IIntentContext {
        public static final MapCodec<RainingContext> CODEC = MapCodec.unit(new RainingContext());

        @Override
        public boolean test(LocalPlayer player) {
            return player.level().isRaining();
        }

        @Override
        public ContextType<?> getType() {
            return MyRegistries.IS_RAINING.get();
        }
    }
    ```

2.  **Register the Type:**
    Use `IntentAPI.CONTEXT_TYPE_REGISTRY_KEY` to register your type to the Intent registry.
    ```java
    public static final DeferredRegister<IIntentContext.ContextType<?>> CONTEXT_TYPES =
            DeferredRegister.create(IntentAPI.CONTEXT_TYPE_REGISTRY_KEY, "my_mod_id");

    public static final DeferredHolder<IIntentContext.ContextType<?>, IIntentContext.ContextType<RainingContext>> IS_RAINING =
            CONTEXT_TYPES.register("is_raining", () -> () -> RainingContext.CODEC);
    ```

### Adding a GUI Editor
To make your custom context editable in the Intent GUI, register an `IContextEditor`.

```java
// In your Client Setup
ContextEditorRegistry.register(MyRegistries.IS_RAINING.get(), () -> new SimpleStateEditor<>(RainingContext::new));
```

## License
This project is licensed under the [MIT License](LICENSE).