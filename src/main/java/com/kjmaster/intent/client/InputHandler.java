package com.kjmaster.intent.client;

import com.kjmaster.intent.Intent;
import com.kjmaster.intent.client.gui.RadialMenuScreen;
import com.kjmaster.intent.client.gui.editor.IntentEditorScreen;
import com.kjmaster.intent.data.IntentProfile;
import com.kjmaster.intent.mixin.MinecraftAccessor;
import com.kjmaster.intent.util.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
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

    // A list of actions waiting to fire (e.g., "Attack in 2 ticks")
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


    // --- EVENTS ---

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        InputConstants.Key physicalKey = InputConstants.getKey(event.getKey(), event.getScanCode());

        if (event.getAction() == GLFW.GLFW_RELEASE) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {

            if (KeyInit.OPEN_EDITOR.get().matches(event.getKey(), event.getScanCode())) {
                mc.setScreen(new IntentEditorScreen());
                return;
            }

            if (activeRedirects.containsKey(physicalKey)) return;
            if (mc.screen instanceof RadialMenuScreen) return;

            IntentProfile profile = Intent.DATA_MANAGER.getMasterProfile();

            for (IntentProfile.Binding binding : profile.bindings()) {
                if (binding.getInput().equals(physicalKey)) {
                    List<IntentProfile.IntentEntry> matches = findMatches(binding.stack(), mc.player);

                    if (matches.isEmpty()) continue;
                    else if (matches.size() == 1) performAction(matches.getFirst(), physicalKey, 0);
                    else mc.setScreen(new RadialMenuScreen(matches, physicalKey));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Convert Mouse ID to unified InputConstants.Key
        // Mouse codes: 0=Left, 1=Right, 2=Middle, 3+=Side Buttons
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(event.getButton());

        // Ignore Release (handled by tick)
        if (event.getAction() == GLFW.GLFW_RELEASE) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {

            // Safety: If Left/Right click is NOT bound in our system, do NOT touch it.
            // We only intervene if the user explicitly created a profile for "key.mouse.0" etc.
            boolean isBound = false;
            for (IntentProfile profile : Intent.DATA_MANAGER.getProfiles().values()) {
                for (IntentProfile.Binding binding : profile.bindings()) {
                    if (binding.getInput().equals(mouseKey)) {
                        isBound = true;
                        break;
                    }
                }
            }
            if (!isBound) return; // Let vanilla handle normal clicks

            // If we are here, the user wants to override this mouse button.
            if (activeRedirects.containsKey(mouseKey)) return;
            if (mc.screen instanceof RadialMenuScreen) return;

            for (IntentProfile profile : Intent.DATA_MANAGER.getProfiles().values()) {
                for (IntentProfile.Binding binding : profile.bindings()) {
                    if (binding.getInput().equals(mouseKey)) {
                        List<IntentProfile.IntentEntry> matches = findMatches(binding.stack(), mc.player);

                        if (matches.isEmpty()) continue;
                        else if (matches.size() == 1) performAction(matches.get(0), mouseKey, 0);
                        else mc.setScreen(new RadialMenuScreen(matches, mouseKey));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 1. PROCESS TASK QUEUE (Attacks/Clicks)
        if (!taskQueue.isEmpty()) {
            Iterator<DelayedTask> it = taskQueue.iterator();
            while (it.hasNext()) {
                DelayedTask task = it.next();
                task.ticksWait--;
                if (task.ticksWait <= 0) {
                    // Time's up! Run the action.
                    task.action.run();
                    it.remove();
                }
            }
        }

        // 2. PROCESS HELD KEYS (Jump/Mine)
        long window = mc.getWindow().getWindow();
        List<InputConstants.Key> toRemove = new ArrayList<>();

        for (Map.Entry<InputConstants.Key, RedirectState> entry : activeRedirects.entrySet()) {
            InputConstants.Key physicalKey = entry.getKey();
            RedirectState state = entry.getValue();

            state.target.setDown(true);

            if (state.ticksLeft > 0) {
                state.ticksLeft--;
            } else {
                boolean isPhysicalDown = InputConstants.isKeyDown(window, physicalKey.getValue());
                if (!isPhysicalDown) {
                    state.target.setDown(false);
                    toRemove.add(physicalKey);
                }
            }
        }
        for (InputConstants.Key key : toRemove) activeRedirects.remove(key);
    }

    // --- LOGIC ---

    public static void performAction(IntentProfile.IntentEntry entry, InputConstants.Key physicalKey, int minDuration) {
        KeyMapping targetMapping = KeyMappingHelper.getMapping(entry.actionId());

        if (targetMapping != null) {
            Intent.LOGGER.info(">>> ACTIVATING: [{}]", entry.actionId());

            // SPECIAL HANDLING: ATTACK
            if (targetMapping.getName().equals("key.attack")) {
                // Schedule the attack for 2 ticks later.
                // This gives the GUI time to close and the mouse to re-lock.
                taskQueue.add(new DelayedTask(2, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc instanceof MinecraftAccessor accessor) {
                        // 1. Force the engine to attack
                        boolean success = accessor.intent$startAttack();
                        Intent.LOGGER.info("Triggered startAttack. Result: {}", success);

                        // 2. VISUAL FEEDBACK: Force the arm to swing immediately.
                        // Even if startAttack fails (e.g. cooldown), this proves the code ran.
                        if (mc.player != null) {
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                }));
                return;
            }

            // SPECIAL HANDLING: USE ITEM
            if (targetMapping.getName().equals("key.use")) {
                taskQueue.add(new DelayedTask(2, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc instanceof MinecraftAccessor accessor) {
                        accessor.intent$startUseItem();
                    }
                }));
                return;
            }

            // DEFAULT HANDLING (Jump, Mine, etc.)
            activeRedirects.put(physicalKey, new RedirectState(targetMapping, minDuration));
            targetMapping.setDown(true);
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