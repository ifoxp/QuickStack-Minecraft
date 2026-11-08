package top.ifoxp.quickstack;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
            // The category (a translation key in 1.21.x) needs no registration; NeoForge sorts unknown ones by name.
            for (KeyMapping key : QuickStackClient.KEYS) {
                event.register(key);
            }
        });

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> QuickStackClient.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> QuickStackClient.onDisconnect());
    }

    /** Server-to-client payloads (registered with their handler in QuickStackNeoForge.registerPayloads). */
    static void handle(CustomPacketPayload payload) {
        switch (payload) {
            case Payloads.Flights flights -> QuickStackClient.onFlights(flights);
            case Payloads.LockSync lockSync -> QuickStackClient.onLockSync(lockSync);
            case Payloads.NearbyItems nearbyItems -> QuickStackClient.onNearbyItems(nearbyItems);
            case Payloads.Highlight highlight -> QuickStackClient.onHighlight(highlight);
            case Payloads.StorageSummary summary -> QuickStackClient.onStorageSummary(summary);
            default -> {
            }
        }
    }
}
