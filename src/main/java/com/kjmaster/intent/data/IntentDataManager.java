package com.kjmaster.intent.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.intent.Intent;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IntentDataManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // Holds purely default profiles loaded from assets.
    private final Map<ResourceLocation, IntentProfile> defaultProfiles = new HashMap<>();

    // Holds ONLY the user's custom changes loaded from user_profile.json.
    private IntentProfile userProfile = new IntentProfile(new ArrayList<>());

    // The calculated active profile used by the game (Defaults + User Overrides).
    private IntentProfile masterProfile = new IntentProfile(new ArrayList<>());

    private final Map<InputConstants.Key, List<IntentProfile.Binding>> keyCache = new HashMap<>();

    public IntentDataManager() {
        super(GSON, "intent_profiles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        defaultProfiles.clear();
        LOGGER.info("Loading Intent Profiles...");

        // 1. Load Defaults from Assets
        object.forEach((location, json) -> {
            try {
                // Safety check: ignore if user_config somehow appears in data packs
                if (location.getPath().endsWith("user_config")) return;

                IntentProfile.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> LOGGER.error("Failed to parse profile {}: {}", location, error))
                        .ifPresent(profile -> defaultProfiles.put(location, profile));
            } catch (Exception e) {
                LOGGER.error("Exception loading profile {}", location, e);
            }
        });

        // 2. Load User Config (Overrides)
        loadUserConfig();

        // 3. Flatten into Master Profile & Rebuild Cache
        rebuildMasterProfile();

        LOGGER.info("Intent System Ready. Loaded {} default profiles. Master profile contains {} active bindings.", defaultProfiles.size(), masterProfile.bindings().size());
    }

    private void loadUserConfig() {
        try {
            Path configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("intent");
            Path userFile = configDir.resolve("user_profile.json");

            if (Files.exists(userFile)) {
                try (BufferedReader reader = Files.newBufferedReader(userFile)) {
                    JsonElement json = GSON.fromJson(reader, JsonElement.class);
                    IntentProfile.CODEC.parse(JsonOps.INSTANCE, json)
                            .resultOrPartial(error -> LOGGER.error("Failed to parse user config: {}", error))
                            .ifPresent(profile -> this.userProfile = profile);
                }
            } else {
                // Ensure empty profile if file doesn't exist
                this.userProfile = new IntentProfile(new ArrayList<>());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load user config", e);
            this.userProfile = new IntentProfile(new ArrayList<>());
        }
    }

    private void rebuildMasterProfile() {
        // We accumulate bindings by Key.
        Map<String, List<IntentProfile.IntentEntry>> stackedBindings = new HashMap<>();

        // 1. Accumulate Defaults (Append strategy for multiple mods adding to same key)
        for (IntentProfile profile : defaultProfiles.values()) {
            for (IntentProfile.Binding binding : profile.bindings()) {
                stackedBindings.computeIfAbsent(binding.triggerKey(), k -> new ArrayList<>())
                        .addAll(binding.stack());
            }
        }

        // 2. Apply User Overrides
        // Logic: If the USER defines a key, it completely OVERRIDES the defaults for that key.
        for (IntentProfile.Binding userBinding : userProfile.bindings()) {
            // Replace the stack entirely with the user's version
            stackedBindings.put(userBinding.triggerKey(), new ArrayList<>(userBinding.stack()));
        }

        // 3. Build Final List
        List<IntentProfile.Binding> finalBindings = new ArrayList<>();
        for (Map.Entry<String, List<IntentProfile.IntentEntry>> entry : stackedBindings.entrySet()) {
            finalBindings.add(new IntentProfile.Binding(entry.getKey(), entry.getValue()));
        }

        this.masterProfile = new IntentProfile(finalBindings);
        refreshCache();
    }

    private void refreshCache() {
        keyCache.clear();
        for (IntentProfile.Binding binding : masterProfile.bindings()) {
            InputConstants.Key key = binding.getInput();
            keyCache.computeIfAbsent(key, k -> new ArrayList<>()).add(binding);
        }
    }

    public List<IntentProfile.Binding> getBindings(InputConstants.Key key) {
        return keyCache.getOrDefault(key, Collections.emptyList());
    }

    public IntentProfile getMasterProfile() {
        return masterProfile;
    }

    public void saveToDisk() {
        try {
            Path configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("intent");
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path file = configDir.resolve("user_profile.json");

            // Only save the userProfile, NEVER the masterProfile.
            JsonElement json = IntentProfile.CODEC.encodeStart(JsonOps.INSTANCE, userProfile)
                    .getOrThrow(IllegalStateException::new);

            String jsonString = GSON.toJson(json);
            Files.writeString(file, jsonString);
            LOGGER.info("Saved user profile to {}", file);

            // Reloading is not strictly necessary as memory state is up to date,
            // but we can reload to ensure consistency if needed.
            // For now, in-memory state is authority.

        } catch (Exception e) {
            LOGGER.error("Failed to save profile", e);
        }
    }

    public void updateBinding(IntentProfile.Binding newBinding) {
        // Update the User Profile
        List<IntentProfile.Binding> mutable = new ArrayList<>(userProfile.bindings());

        // Remove existing user definition for this key (if any)
        mutable.removeIf(b -> b.triggerKey().equals(newBinding.triggerKey()));

        // Add the new definition
        mutable.add(newBinding);

        this.userProfile = new IntentProfile(mutable);

        // Rebuild Master to reflect changes in-game immediately
        rebuildMasterProfile();
    }

    public void removeBinding(String triggerKey) {
        // Remove from User Profile
        // This effectively "Reverts to Default" if a default exists,
        // or "Deletes" it if no default exists.
        List<IntentProfile.Binding> mutable = new ArrayList<>(userProfile.bindings());
        mutable.removeIf(b -> b.triggerKey().equals(triggerKey));
        this.userProfile = new IntentProfile(mutable);

        rebuildMasterProfile();
    }
}