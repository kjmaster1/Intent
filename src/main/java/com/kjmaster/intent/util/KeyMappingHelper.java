package com.kjmaster.intent.util;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public class KeyMappingHelper {
    private static final Map<String, KeyMapping> KEY_CACHE = new HashMap<>();

    /**
     * Finds a vanilla/mod KeyMapping by its translation key (e.g., "key.jump").
     */
    public static KeyMapping getMapping(String descriptionId) {
        if (KEY_CACHE.containsKey(descriptionId)) {
            return KEY_CACHE.get(descriptionId);
        }

        // Slow scan (only happens once per key)
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.getName().equals(descriptionId)) {
                KEY_CACHE.put(descriptionId, mapping);
                return mapping;
            }
        }
        return null;
    }
}