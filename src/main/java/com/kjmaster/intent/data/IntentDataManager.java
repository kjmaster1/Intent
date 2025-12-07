package com.kjmaster.intent.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.intent.Intent;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntentDataManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // CHANGED: We now expose a single "Master Profile" that represents the final merged state
    private final Map<ResourceLocation, IntentProfile> rawProfiles = new HashMap<>();
    private IntentProfile masterProfile = new IntentProfile(new ArrayList<>());

    public IntentDataManager() {
        super(GSON, "intent_profiles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        rawProfiles.clear();
        LOGGER.info("Loading Intent Profiles...");

        // 1. Load Defaults from Assets
        object.forEach((location, json) -> {
            try {
                IntentProfile.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> LOGGER.error("Failed to parse profile {}: {}", location, error))
                        .ifPresent(profile -> rawProfiles.put(location, profile));
            } catch (Exception e) {
                LOGGER.error("Exception loading profile {}", location, e);
            }
        });

        // 2. Load User Config (Overrides)
        loadUserConfig();

        // 3. Flatten into Master Profile
        rebuildMasterProfile();

        LOGGER.info("Intent System Ready. Loaded {} profiles merged into {} active bindings.", rawProfiles.size(), masterProfile.bindings().size());
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
                            .ifPresent(profile -> {
                                ResourceLocation configId = ResourceLocation.fromNamespaceAndPath(Intent.MODID, "user_config");
                                rawProfiles.put(configId, profile);
                            });
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load user config", e);
        }
    }

    // Logic to Merge Defaults + User Config
    private void rebuildMasterProfile() {
        Map<String, IntentProfile.Binding> mergedBindings = new HashMap<>();

        // 1. Apply Defaults First
        for (Map.Entry<ResourceLocation, IntentProfile> entry : rawProfiles.entrySet()) {
            // Skip the user config for a moment
            if (entry.getKey().getPath().equals("user_config")) continue;

            for (IntentProfile.Binding binding : entry.getValue().bindings()) {
                mergedBindings.put(binding.triggerKey(), binding);
            }
        }

        // 2. Apply User Config (Overwriting Defaults)
        ResourceLocation configId = ResourceLocation.fromNamespaceAndPath(Intent.MODID, "user_config");
        if (rawProfiles.containsKey(configId)) {
            for (IntentProfile.Binding binding : rawProfiles.get(configId).bindings()) {
                // This PUT replaces any existing binding for the same key
                mergedBindings.put(binding.triggerKey(), binding);
            }
        }

        this.masterProfile = new IntentProfile(new ArrayList<>(mergedBindings.values()));
    }

    // Changing this to return the single master profile simplifies the InputHandler logic too!
    public IntentProfile getMasterProfile() {
        return masterProfile;
    }

    // Kept for compatibility if you need raw access, but prefer getMasterProfile()
    public Map<ResourceLocation, IntentProfile> getProfiles() {
        return rawProfiles;
    }

    public void saveToDisk() {
        try {
            Path configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("intent");
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path file = configDir.resolve("user_profile.json");

            // We save the MASTER profile (which represents the current state of the editor)
            // This effectively "forks" the defaults into the user config.
            JsonElement json = IntentProfile.CODEC.encodeStart(JsonOps.INSTANCE, masterProfile)
                    .getOrThrow(IllegalStateException::new);

            String jsonString = GSON.toJson(json);
            Files.writeString(file, jsonString);
            LOGGER.info("Saved master profile to {}", file);

            // Reload from disk to ensure state is synced
            loadUserConfig();
            rebuildMasterProfile();

        } catch (Exception e) {
            LOGGER.error("Failed to save profile", e);
        }
    }

    // Helper for Editor to update the Master Profile directly
    public void updateBinding(IntentProfile.Binding newBinding) {
        // Update the in-memory master profile immediately so UI updates
        List<IntentProfile.Binding> mutable = new ArrayList<>(masterProfile.bindings());

        // Remove old if exists
        mutable.removeIf(b -> b.triggerKey().equals(newBinding.triggerKey()));
        mutable.add(newBinding);

        this.masterProfile = new IntentProfile(mutable);
    }
}