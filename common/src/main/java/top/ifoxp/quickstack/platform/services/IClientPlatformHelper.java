package top.ifoxp.quickstack.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Loader calls that only exist on the client. */
public interface IClientPlatformHelper {

    void sendToServer(CustomPacketPayload payload);

    /** True when the server we are connected to runs QuickStack (it can receive our payloads). */
    boolean serverHasMod();
}
