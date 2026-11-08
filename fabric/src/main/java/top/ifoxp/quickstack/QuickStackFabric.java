package top.ifoxp.quickstack;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import top.ifoxp.quickstack.network.Network;
import top.ifoxp.quickstack.server.LockManager;
import top.ifoxp.quickstack.server.ServerEvents;

public class QuickStackFabric implements ModInitializer {

    /** Locked inventory slots, saved with the player and kept on death. */
    public static final AttachmentType<Long> LOCKS = AttachmentRegistry.create(QuickStack.id("locks"),
            builder -> builder.persistent(Codec.LONG).copyOnDeath().initializer(() -> LockManager.DEFAULT_MASK));

    /** Set on a container block entity that quick stack to nearby containers must skip. */
    public static final AttachmentType<Boolean> EXCLUDED = AttachmentRegistry.create(QuickStack.id("excluded"),
            builder -> builder.persistent(Codec.BOOL));

    @Override
    public void onInitialize() {
        QuickStack.init();
        registerPayloads();

        ServerLifecycleEvents.SERVER_STARTING.register(ServerEvents::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerEvents::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(ServerEvents::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerEvents.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerEvents.onPlayerLeave(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> ServerEvents.onPlayerRespawn(newPlayer));
    }

    private static void registerPayloads() {
        for (Network.ServerBound<?> entry : Network.SERVER_BOUND) {
            registerServerBound(entry);
        }
        for (Network.ClientBound<?> entry : Network.CLIENT_BOUND) {
            registerClientBound(entry);
        }
    }

    private static <T extends CustomPacketPayload> void registerClientBound(Network.ClientBound<T> entry) {
        PayloadTypeRegistry.clientboundPlay().register(entry.type(), entry.codec());
    }

    private static <T extends CustomPacketPayload> void registerServerBound(Network.ServerBound<T> entry) {
        PayloadTypeRegistry.serverboundPlay().register(entry.type(), entry.codec());
        ServerPlayNetworking.registerGlobalReceiver(entry.type(),
                (payload, context) -> entry.handler().handle(payload, context.player()));
    }
}
