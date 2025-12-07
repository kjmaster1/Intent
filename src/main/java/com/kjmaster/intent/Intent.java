package com.kjmaster.intent;

import com.kjmaster.intent.client.gui.IntentDebugOverlay;
import com.kjmaster.intent.data.IntentDataManager;
import com.kjmaster.intent.impl.ContextTypes;
import com.kjmaster.intent.registry.IntentRegistries;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@Mod(value = Intent.MODID, dist = Dist.CLIENT)
public class Intent {

    public static final String MODID = "intent";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final IntentDataManager DATA_MANAGER = new IntentDataManager();

    public Intent(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        IntentRegistries.register(modEventBus);
        ContextTypes.register();

        modEventBus.addListener(this::onRegisterReloadListeners);
        modEventBus.addListener(this::registerOverlays);
    }

    private void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(DATA_MANAGER);
    }

    private void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Intent.MODID, "debug_hud"),
                IntentDebugOverlay::render
        );
    }

}
