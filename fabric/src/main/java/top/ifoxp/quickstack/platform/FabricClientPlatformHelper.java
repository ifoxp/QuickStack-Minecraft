package top.ifoxp.quickstack.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.services.IClientPlatformHelper;

public class FabricClientPlatformHelper implements IClientPlatformHelper {

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Override
    public boolean serverHasMod() {
        return ClientPlayNetworking.canSend(Payloads.QuickStackNearby.TYPE);
    }
}
