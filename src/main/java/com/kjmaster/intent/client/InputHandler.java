package com.kjmaster.intent.client;

import com.kjmaster.intent.Config;
import com.kjmaster.intent.Intent;
import com.kjmaster.intent.client.gui.RadialMenuScreen;
import com.kjmaster.intent.client.gui.editor.IntentEditorScreen;
import com.kjmaster.intent.data.IntentProfile;
import com.kjmaster.intent.mixin.KeyMappingAccessor;
import com.kjmaster.intent.util.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;

@EventBusSubscriber(modid = Intent.MODID, value = Dist.CLIENT)
public class InputHandler {

    // --- QUEUE SYSTEM ---
    private static class DelayedTask {
        int ticksWait;
        final Runnable action;

        DelayedTask(int ticksWait, Runnable action) {
            this.ticksWait = ticksWait;
            this.action = action;
        }
    }

    private static final List<DelayedTask> taskQueue = new ArrayList<>();

    // Standard Redirects (Holding Jump/Mine)
    private static class RedirectState {
        final KeyMapping target;
        int ticksLeft;

        RedirectState(KeyMapping target, int minDuration) {
            this.target = target;
            this.ticksLeft = minDuration;
        }
    }

    private static final Map<InputConstants.Key, RedirectState> activeRedirects = new HashMap<>();

    // "Hold to Open" State
    private static final Map<InputConstants.Key, Integer> holdingKeys = new HashMap<>();

    // Recursion Guard
    private static final Set<String> processingActions = new HashSet<>();

    // --- EVENTS ---

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetAllState();
    }

    // Clear state when a non-Intent screen opens (e.g. Inventory, Chat, Pause)
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof RadialMenuScreen) && !(event.getNewScreen() instanceof IntentEditorScreen)) {
            resetAllState();
        }
    }

    private static void resetAllState() {
        if (!taskQueue.isEmpty()) {
            Intent.LOGGER.debug("InputHandler: Clearing {} pending tasks due to state reset.", taskQueue.size());
            taskQueue.clear();
        }

        if (!activeRedirects.isEmpty()) {
            Intent.LOGGER.debug("InputHandler: Releasing {} active key redirects.", activeRedirects.size());
            for (RedirectState state : activeRedirects.values()) {
                state.target.setDown(false);
            }
            activeRedirects.clear();
        }
        holdingKeys.clear();
        processingActions.clear();
    }

    /**
     * Called via Mixin from KeyboardHandler.
     *
     * @return true if the key was handled by Intent and vanilla processing should be cancelled.
     */
    public static boolean handleKeyInput(int keyCode, int scanCode, int action, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        InputConstants.Key physicalKey = InputConstants.getKey(keyCode, scanCode);

        // RELEASE EVENT
        if (action == GLFW.GLFW_RELEASE) {

            // Immediately release redirects to prevent "stickiness" or race conditions
            if (activeRedirects.containsKey(physicalKey)) {
                RedirectState state = activeRedirects.get(physicalKey);
                // Only interrupt if the minimum duration has passed
                if (state.ticksLeft <= 0) {
                    activeRedirects.remove(physicalKey);
                    state.target.setDown(false);
                    return true; // Consume event
                }
            }

            // If we were holding this key waiting for a menu, but released early -> TAP ACTION
            if (holdingKeys.containsKey(physicalKey)) {
                holdingKeys.remove(physicalKey);

                // Execute "Tap" behavior (Highest Priority Match)
                List<IntentProfile.Binding> bindings = Intent.DATA_MANAGER.getBindings(physicalKey);
                for (IntentProfile.Binding binding : bindings) {
                    List<IntentProfile.IntentEntry> matches = findMatches(binding.stack(), mc.player);
                    if (!matches.isEmpty()) {
                        performAction(matches.getFirst(), physicalKey, 0);
                        return true; // Cancel vanilla release logic for this key
                    }
                }
            }
            return false;
        }

        // PRESS EVENT
        if (action == GLFW.GLFW_PRESS) {

            // Allow Intent keys in GUIs (Inventory, etc.), BUT block them if typing in Chat or an EditBox.
            if (mc.screen != null && !(mc.screen instanceof RadialMenuScreen)) {
                if (mc.screen instanceof ChatScreen) return false;
                if (mc.screen.getFocused() instanceof EditBox) return false;
            }

            if (KeyInit.OPEN_EDITOR.get().matches(keyCode, scanCode)) {
                mc.setScreen(new IntentEditorScreen());
                return true; // Swallow key
            }

            if (KeyInit.TOGGLE_DEBUG.get().matches(keyCode, scanCode)) {
                Config.DEBUG_MODE.set(!Config.DEBUG_MODE.get());
                return true; // Swallow key
            }

            if (activeRedirects.containsKey(physicalKey)) return false;
            if (mc.screen instanceof RadialMenuScreen) return false;

            // Binding Logic
            List<IntentProfile.Binding> bindings = Intent.DATA_MANAGER.getBindings(physicalKey);
            for (IntentProfile.Binding binding : bindings) {
                List<IntentProfile.IntentEntry> matches = findMatches(binding.stack(), mc.player);
                if (matches.isEmpty()) continue;

                // 1. Single Match -> Always Instant
                if (matches.size() == 1) {
                    performAction(matches.getFirst(), physicalKey, 0);
                    return true; // BLOCK VANILLA
                }

                // 2. Multiple Matches -> Radial Menu
                int delay = Config.RADIAL_MENU_DELAY.get();
                if (delay <= 0) {
                    // Instant Open
                    mc.setScreen(new RadialMenuScreen(matches, physicalKey));
                    return true; // BLOCK VANILLA
                } else {
                    // Delayed Open (Wait for Tick)
                    holdingKeys.put(physicalKey, delay);
                    // We also block vanilla here, because we are "holding" it for potential Intent use
                    return true;
                }
            }
        }
        return false;
    }

    // Keep MouseInput as an event, because InputEvent.MouseButton.Pre IS cancelable.
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(event.getButton());

        // RELEASE EVENT
        if (event.getAction() == GLFW.GLFW_RELEASE) {

            // Immediately release redirects for Mouse Inputs too
            if (activeRedirects.containsKey(mouseKey)) {
                RedirectState state = activeRedirects.get(mouseKey);
                if (state.ticksLeft <= 0) {
                    activeRedirects.remove(mouseKey);
                    state.target.setDown(false);
                    event.setCanceled(true); // Swallow mouse up if we swallowed mouse down
                    return;
                }
            }

            // TAP BEHAVIOR: If we were holding this button waiting for a menu, but released early
            if (holdingKeys.containsKey(mouseKey)) {
                holdingKeys.remove(mouseKey);

                // Execute "Tap" behavior (Highest Priority Match)
                List<IntentProfile.Binding> bindings = Intent.DATA_MANAGER.getBindings(mouseKey);
                for (IntentProfile.Binding binding : bindings) {
                    List<IntentProfile.IntentEntry> matches = findMatches(binding.stack(), mc.player);
                    if (!matches.isEmpty()) {
                        performAction(matches.getFirst(), mouseKey, 0);
                        break;
                    }
                }
            }
            return;
        }

        // PRESS EVENT
        if (event.getAction() == GLFW.GLFW_PRESS) {

            // Safety: If this mouse button isn't bound in Intent, let Vanilla handle it.
            List<IntentProfile.Binding> bindings = Intent.DATA_MANAGER.getBindings(mouseKey);
            if (bindings.isEmpty()) return;

            if (activeRedirects.containsKey(mouseKey)) return;
            if (mc.screen instanceof RadialMenuScreen) return;

            for (IntentProfile.Binding binding : bindings) {
                List<IntentProfile.IntentEntry> matches = findMatches(binding.stack(), mc.player);
                if (matches.isEmpty()) continue;

                // PREVENT the default vanilla action for this key if Intent handled it
                event.setCanceled(true);

                // 1. Single Match -> Always Instant
                if (matches.size() == 1) {
                    performAction(matches.getFirst(), mouseKey, 0);
                    return;
                }

                // 2. Multiple Matches -> Radial Menu logic
                int delay = Config.RADIAL_MENU_DELAY.get();
                if (delay <= 0) {
                    mc.setScreen(new RadialMenuScreen(matches, mouseKey));
                } else {
                    holdingKeys.put(mouseKey, delay);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            resetAllState();
            return;
        }

        // Focus Loss Check: If we lose window focus, release everything immediately.
        if (!mc.isWindowActive()) {
            resetAllState();
            return;
        }

        // Check Holding Keys
        if (!holdingKeys.isEmpty()) {
            // Safe Iteration: Create a copy of the keys to avoid ConcurrentModificationException
            // if resetAllState() is triggered (e.g. by setScreen) while iterating.
            List<InputConstants.Key> keys = new ArrayList<>(holdingKeys.keySet());
            for (InputConstants.Key key : keys) {
                if (!holdingKeys.containsKey(key)) continue; // Was removed by something else

                int ticksLeft = holdingKeys.get(key) - 1;

                if (ticksLeft <= 0) {
                    // Time is up! Open the menu.
                    holdingKeys.remove(key); // Remove from holding so Release doesn't trigger Tap

                    List<IntentProfile.Binding> bindings = Intent.DATA_MANAGER.getBindings(key);
                    for (IntentProfile.Binding binding : bindings) {
                        List<IntentProfile.IntentEntry> matches = findMatches(binding.stack(), mc.player);
                        if (matches.size() > 1) {
                            mc.setScreen(new RadialMenuScreen(matches, key));
                            break;
                        } else if (!matches.isEmpty()) {
                            // State changed, only 1 match now? Execute it.
                            performAction(matches.getFirst(), key, 0);
                        }
                    }
                } else {
                    holdingKeys.put(key, ticksLeft);
                }
            }
        }

        // 1. PROCESS TASK QUEUE
        if (!taskQueue.isEmpty()) {
            Iterator<DelayedTask> it = taskQueue.iterator();
            while (it.hasNext()) {
                DelayedTask task = it.next();
                task.ticksWait--;
                if (task.ticksWait <= 0) {
                    task.action.run();
                    it.remove();
                }
            }
        }

        // 2. PROCESS HELD KEYS
        long window = mc.getWindow().getWindow();
        List<InputConstants.Key> toRemove = new ArrayList<>();

        for (Map.Entry<InputConstants.Key, RedirectState> entry : activeRedirects.entrySet()) {
            InputConstants.Key physicalKey = entry.getKey();
            RedirectState state = entry.getValue();

            state.target.setDown(true);

            if (state.ticksLeft > 0) {
                state.ticksLeft--;
            } else {
                // Correctly check mouse buttons vs keyboard keys
                boolean isPhysicalDown;
                if (physicalKey.getType() == InputConstants.Type.MOUSE) {
                    isPhysicalDown = GLFW.glfwGetMouseButton(window, physicalKey.getValue()) == GLFW.GLFW_PRESS;
                } else {
                    isPhysicalDown = InputConstants.isKeyDown(window, physicalKey.getValue());
                }

                if (!isPhysicalDown) {
                    state.target.setDown(false);
                    toRemove.add(physicalKey);
                }
            }
        }
        for (InputConstants.Key key : toRemove) activeRedirects.remove(key);
    }

    public static void performAction(IntentProfile.IntentEntry entry, InputConstants.Key physicalKey, int minDuration) {
        // [Fix 1.2] Guard against recursion (Key A -> Key B -> Key A)
        if (processingActions.contains(entry.actionId())) return;
        processingActions.add(entry.actionId());

        try {
            KeyMapping targetMapping = KeyMappingHelper.getMapping(entry.actionId());

            if (targetMapping != null) {
                Intent.LOGGER.info(">>> ACTIVATING: [{}]", entry.actionId());

                long window = Minecraft.getInstance().getWindow().getWindow();
                boolean isPhysicalDown;

                // Correctly check if the physical key is currently held (Handles both Keyboard and Mouse)
                if (physicalKey.getType() == InputConstants.Type.MOUSE) {
                    isPhysicalDown = GLFW.glfwGetMouseButton(window, physicalKey.getValue()) == GLFW.GLFW_PRESS;
                } else {
                    isPhysicalDown = InputConstants.isKeyDown(window, physicalKey.getValue());
                }

                // Check if this is a "Continuous" action like Attack or Use
                boolean isContinuousAction = targetMapping.getName().equals("key.attack") || targetMapping.getName().equals("key.use");

                // Instead of a one-shot task, ensure continuous actions like attacking/eating
                // persist for at least 5 ticks if the physical key isn't held (Tap Action).
                if (isContinuousAction && !isPhysicalDown && minDuration < 5) {
                    minDuration = 5;
                }

                if (!targetMapping.isDown()) {
                    if (targetMapping instanceof KeyMappingAccessor accessor) {
                        accessor.intent$setClickCount(accessor.intent$getClickCount() + 1);
                    }
                }

                // Use the Redirect system for everything.
                // This allows Mining/Eating to persist as long as the user holds the button (or minDuration passes).
                activeRedirects.put(physicalKey, new RedirectState(targetMapping, minDuration));
                targetMapping.setDown(true);
            }
        } finally {
            processingActions.remove(entry.actionId());
        }
    }

    private static List<IntentProfile.IntentEntry> findMatches(List<IntentProfile.IntentEntry> stack, LocalPlayer player) {
        List<IntentProfile.IntentEntry> matches = new ArrayList<>();
        List<IntentProfile.IntentEntry> sorted = stack.stream()
                .sorted(Comparator.comparingInt(IntentProfile.IntentEntry::priority).reversed())
                .toList();
        int highestPriorityFound = Integer.MIN_VALUE;
        for (IntentProfile.IntentEntry entry : sorted) {
            if (entry.context().test(player)) {
                if (entry.priority() > highestPriorityFound) {
                    matches.clear();
                    highestPriorityFound = entry.priority();
                }
                if (entry.priority() == highestPriorityFound) {
                    matches.add(entry);
                }
            }
        }
        return matches;
    }
}