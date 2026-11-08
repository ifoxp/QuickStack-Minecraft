package top.ifoxp.quickstack.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.services.IClientPlatformHelper;

public class NeoForgeClientPlatformHelper implements IClientPlatformHelper {

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public boolean serverHasMod() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(Payloads.QuickStackNearby.TYPE);
    }
}
