package top.ifoxp.quickstack.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import top.ifoxp.quickstack.server.ItemFinder;
import top.ifoxp.quickstack.server.LockManager;
import top.ifoxp.quickstack.server.MenuActions;
import top.ifoxp.quickstack.server.NearbyCrafting;
import top.ifoxp.quickstack.server.QuickStackManager;

/**
 * The payload table both loaders register from. Server handlers live here; client handlers are registered
 * separately by the client entrypoints so a dedicated server never loads client classes.
 */
public final class Network {

    /** Bump when a payload changes shape, so mismatched jars are refused instead of misreading packets. */
    public static final String PROTOCOL = "1";

    @FunctionalInterface
    public interface ServerHandler<T> {
        void handle(T payload, ServerPlayer player);
    }

    public record ServerBound<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            ServerHandler<T> handler) {
    }

    public record ClientBound<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
    }

    public static final List<ServerBound<?>> SERVER_BOUND = List.of(
            new ServerBound<>(Payloads.QuickStackNearby.TYPE, Payloads.QuickStackNearby.CODEC,
                    (payload, player) -> QuickStackManager.requestQuickStack(player, payload.animate())),
            new ServerBound<>(Payloads.MenuActionRequest.TYPE, Payloads.MenuActionRequest.CODEC, MenuActions::handle),
            new ServerBound<>(Payloads.ToggleLock.TYPE, Payloads.ToggleLock.CODEC,
                    (payload, player) -> LockManager.toggle(player, payload.slot())),
            new ServerBound<>(Payloads.RequestNearbyItems.TYPE, Payloads.RequestNearbyItems.CODEC,
                    (payload, player) -> NearbyCrafting.request(player, payload.enabled())),
            new ServerBound<>(Payloads.FindItems.TYPE, Payloads.FindItems.CODEC,
                    (payload, player) -> ItemFinder.find(player, payload.items())),
            new ServerBound<>(Payloads.Undo.TYPE, Payloads.Undo.CODEC,
                    (payload, player) -> QuickStackManager.undo(player)));

    public static final List<ClientBound<?>> CLIENT_BOUND = List.of(
            new ClientBound<>(Payloads.Flights.TYPE, Payloads.Flights.CODEC),
            new ClientBound<>(Payloads.LockSync.TYPE, Payloads.LockSync.CODEC),
            new ClientBound<>(Payloads.NearbyItems.TYPE, Payloads.NearbyItems.CODEC),
            new ClientBound<>(Payloads.Highlight.TYPE, Payloads.Highlight.CODEC));

    private Network() {
    }
}
