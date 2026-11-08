package top.ifoxp.quickstack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.network.Payloads;

public class QuickStackFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        QuickStack.initClient();

        KeyMapping.Category.register(QuickStackClient.CATEGORY.id());
        for (KeyMapping key : QuickStackClient.KEYS) {
            KeyMappingHelper.registerKeyMapping(key);
        }

        ClientPlayNetworking.registerGlobalReceiver(Payloads.Flights.TYPE, (payload, context) -> QuickStackClient.onFlights(payload));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.LockSync.TYPE, (payload, context) -> QuickStackClient.onLockSync(payload));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.NearbyItems.TYPE, (payload, context) -> QuickStackClient.onNearbyItems(payload));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.Highlight.TYPE, (payload, context) -> QuickStackClient.onHighlight(payload));

        ClientTickEvents.END_CLIENT_TICK.register(QuickStackClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> QuickStackClient.onDisconnect());
    }
}
