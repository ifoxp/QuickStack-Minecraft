package top.ifoxp.quickstack;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.client.ui.ConfigScreen;
import top.ifoxp.quickstack.network.Payloads;

/** Client wiring; only loaded on the physical client. */
final class QuickStackNeoForgeClient {

    private QuickStackNeoForgeClient() {
    }

    static void init(IEventBus modBus, ModContainer container) {
        QuickStack.initClient();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(QuickStackClient.CATEGORY);
            for (KeyMapping key : QuickStackClient.KEYS) {
                event.register(key);
            }
        });
        modBus.addListener((RegisterClientPayloadHandlersEvent event) -> {
            event.register(Payloads.Flights.TYPE, (payload, context) -> QuickStackClient.onFlights(payload));
            event.register(Payloads.LockSync.TYPE, (payload, context) -> QuickStackClient.onLockSync(payload));
            event.register(Payloads.NearbyItems.TYPE, (payload, context) -> QuickStackClient.onNearbyItems(payload));
            event.register(Payloads.Highlight.TYPE, (payload, context) -> QuickStackClient.onHighlight(payload));
            event.register(Payloads.StorageSummary.TYPE, (payload, context) -> QuickStackClient.onStorageSummary(payload));
        });

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> QuickStackClient.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> QuickStackClient.onDisconnect());
    }
}
