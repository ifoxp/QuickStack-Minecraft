package top.ifoxp.quickstack;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.ifoxp.quickstack.network.Network;
import top.ifoxp.quickstack.server.LockManager;
import top.ifoxp.quickstack.server.ServerEvents;

@Mod(QuickStack.MOD_ID)
public class QuickStackNeoForge {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, QuickStack.MOD_ID);

    /** Locked inventory slots, saved with the player and kept on death. */
    public static final Supplier<AttachmentType<Long>> LOCKS = ATTACHMENTS.register("locks",
            () -> AttachmentType.builder(() -> LockManager.DEFAULT_MASK).serialize(Codec.LONG.fieldOf("mask")).copyOnDeath().build());

    /** Set on a container block entity that quick stack to nearby containers must skip. */
    public static final Supplier<AttachmentType<Boolean>> EXCLUDED = ATTACHMENTS.register("excluded",
            () -> AttachmentType.builder(() -> Boolean.FALSE).serialize(Codec.BOOL.fieldOf("excluded")).build());

    public QuickStackNeoForge(IEventBus modBus, ModContainer container, Dist dist) {
        QuickStack.init();
        ATTACHMENTS.register(modBus);
        modBus.addListener(QuickStackNeoForge::registerPayloads);

        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> ServerEvents.onServerStarting(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> ServerEvents.onServerStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> ServerEvents.onServerTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerEvents.onPlayerJoin(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerEvents.onPlayerLeave(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ServerEvents.onPlayerRespawn(player);
            }
        });

        if (dist.isClient()) {
            QuickStackNeoForgeClient.init(modBus, container);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Network.PROTOCOL);
        for (Network.ServerBound<?> entry : Network.SERVER_BOUND) {
            registerServerBound(registrar, entry);
        }
        for (Network.ClientBound<?> entry : Network.CLIENT_BOUND) {
            registerClientBound(registrar, entry);
        }
    }

    /** Client handlers are added in RegisterClientPayloadHandlersEvent, so servers never load client classes. */
    private static <T extends CustomPacketPayload> void registerClientBound(PayloadRegistrar registrar, Network.ClientBound<T> entry) {
        registrar.playToClient(entry.type(), entry.codec());
    }

    private static <T extends CustomPacketPayload> void registerServerBound(PayloadRegistrar registrar, Network.ServerBound<T> entry) {
        registrar.playToServer(entry.type(), entry.codec(), (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                entry.handler().handle(payload, player);
            }
        });
    }
}
