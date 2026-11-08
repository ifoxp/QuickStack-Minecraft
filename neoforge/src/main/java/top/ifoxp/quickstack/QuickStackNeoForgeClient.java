package top.ifoxp.quickstack;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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
            for (KeyMapping key : QuickStackClient.KEYS) {
                event.register(key);
            }
        });
        QuickStackNeoForge.clientHandler = QuickStackNeoForgeClient::handle;

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> QuickStackClient.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> QuickStackClient.onDisconnect());
    }

    private static void handle(CustomPacketPayload payload) {
        switch (payload) {
            case Payloads.Flights flights -> QuickStackClient.onFlights(flights);
            case Payloads.LockSync sync -> QuickStackClient.onLockSync(sync);
            case Payloads.NearbyItems items -> QuickStackClient.onNearbyItems(items);
            case Payloads.Highlight highlight -> QuickStackClient.onHighlight(highlight);
            case Payloads.StorageSummary summary -> QuickStackClient.onStorageSummary(summary);
            default -> {
            }
        }
    }
}
