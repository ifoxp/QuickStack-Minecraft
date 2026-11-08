package top.ifoxp.quickstack.network;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ifoxp.quickstack.QuickStack;

/** Every packet QuickStack sends. C2S = client to server, S2C = server to client. */
public final class Payloads {

    /** An item by registry id ({@code Item.STREAM_CODEC} from 1.21.5). */
    private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> ITEM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);
    /** Three doubles ({@code Vec3.STREAM_CODEC} from 1.21.2). */
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    private Payloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
        return new CustomPacketPayload.Type<>(QuickStack.id(name));
    }

    // ---- client -> server ----

    /**
     * Quick stack the unlocked inventory into nearby containers that already hold the same items. Without
     * {@code animate} the items land at once and only the containers sparkle.
     */
    public record QuickStackNearby(boolean animate) implements CustomPacketPayload {
        public static final Type<QuickStackNearby> TYPE = typeOf("quick_stack_nearby");
        public static final StreamCodec<RegistryFriendlyByteBuf, QuickStackNearby> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, QuickStackNearby::animate, QuickStackNearby::new);

        @Override
        public Type<QuickStackNearby> type() {
            return TYPE;
        }
    }

    /** One of the {@link MenuAction}s on the menu the player has open. */
    public record MenuActionRequest(MenuAction action, int keepInContainer, int sortMode) implements CustomPacketPayload {
        public static final Type<MenuActionRequest> TYPE = typeOf("menu_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionRequest> CODEC = StreamCodec.composite(
                MenuAction.STREAM_CODEC, MenuActionRequest::action,
                ByteBufCodecs.VAR_INT, MenuActionRequest::keepInContainer,
                ByteBufCodecs.VAR_INT, MenuActionRequest::sortMode,
                MenuActionRequest::new);

        @Override
        public Type<MenuActionRequest> type() {
            return TYPE;
        }
    }

    /** Toggle the lock on one main-inventory slot (0-35); {@link #RESET_DEFAULT} and {@link #UNLOCK_ALL} act on all. */
    public record ToggleLock(int slot) implements CustomPacketPayload {
        public static final int RESET_DEFAULT = -1;
        public static final int UNLOCK_ALL = -2;
        public static final Type<ToggleLock> TYPE = typeOf("toggle_lock");
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleLock> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ToggleLock::slot, ToggleLock::new);

        @Override
        public Type<ToggleLock> type() {
            return TYPE;
        }
    }

    /**
     * Ask for the summary of items in containers around the player (for the recipe book). {@code enabled} carries
     * the player's preference: when false the server stops pulling crafting ingredients from containers for them.
     */
    public record RequestNearbyItems(boolean enabled) implements CustomPacketPayload {
        public static final Type<RequestNearbyItems> TYPE = typeOf("request_nearby_items");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestNearbyItems> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, RequestNearbyItems::enabled, RequestNearbyItems::new);

        @Override
        public Type<RequestNearbyItems> type() {
            return TYPE;
        }
    }

    /** Highlight the nearby containers holding any of these items. */
    public record FindItems(List<Holder<Item>> items) implements CustomPacketPayload {
        public static final int MAX = 512;
        public static final Type<FindItems> TYPE = typeOf("find_items");
        public static final StreamCodec<RegistryFriendlyByteBuf, FindItems> CODEC = StreamCodec.composite(
                ITEM_CODEC.apply(ByteBufCodecs.list(MAX)), FindItems::items, FindItems::new);

        @Override
        public Type<FindItems> type() {
            return TYPE;
        }
    }

    /** Ask what the containers within the search radius hold (for the search screen). */
    public record RequestStorage() implements CustomPacketPayload {
        public static final Type<RequestStorage> TYPE = typeOf("request_storage");
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestStorage> CODEC = StreamCodec.unit(new RequestStorage());

        @Override
        public Type<RequestStorage> type() {
            return TYPE;
        }
    }

    /** Move {@code amount} items from a slot of the open menu onto the cursor (mouse wheel up, one per notch). */
    public record TakeFromSlot(int containerId, int slot, int amount) implements CustomPacketPayload {
        public static final Type<TakeFromSlot> TYPE = typeOf("take_from_slot");
        public static final StreamCodec<RegistryFriendlyByteBuf, TakeFromSlot> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TakeFromSlot::containerId,
                ByteBufCodecs.VAR_INT, TakeFromSlot::slot,
                ByteBufCodecs.VAR_INT, TakeFromSlot::amount,
                TakeFromSlot::new);

        @Override
        public Type<TakeFromSlot> type() {
            return TYPE;
        }
    }

    /** Undo the last quick stack to nearby containers. */
    public record Undo() implements CustomPacketPayload {
        public static final Type<Undo> TYPE = typeOf("undo");
        public static final StreamCodec<RegistryFriendlyByteBuf, Undo> CODEC = StreamCodec.unit(new Undo());

        @Override
        public Type<Undo> type() {
            return TYPE;
        }
    }

    // ---- server -> client ----

    /**
     * One item flying from the player to a container. Ticks count from the moment the client receives the packet:
     * the item leaves the player, hovers in front of the camera and flies off at {@code launchTick}.
     */
    public record Flight(ItemStack stack, Vec3 target, int launchTick, int arriveTick) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Flight> CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Flight::stack,
                VEC3_CODEC, Flight::target,
                ByteBufCodecs.VAR_INT, Flight::launchTick,
                ByteBufCodecs.VAR_INT, Flight::arriveTick,
                Flight::new);
    }

    public record Flights(List<Flight> flights, boolean cancelPrevious) implements CustomPacketPayload {
        public static final Type<Flights> TYPE = typeOf("flights");
        public static final StreamCodec<RegistryFriendlyByteBuf, Flights> CODEC = StreamCodec.composite(
                Flight.CODEC.apply(ByteBufCodecs.list(512)), Flights::flights,
                ByteBufCodecs.BOOL, Flights::cancelPrevious,
                Flights::new);

        @Override
        public Type<Flights> type() {
            return TYPE;
        }
    }

    public record LockSync(long mask) implements CustomPacketPayload {
        public static final Type<LockSync> TYPE = typeOf("lock_sync");
        public static final StreamCodec<RegistryFriendlyByteBuf, LockSync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, LockSync::mask, LockSync::new);

        @Override
        public Type<LockSync> type() {
            return TYPE;
        }
    }

    public record NearbyCount(Holder<Item> item, int count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, NearbyCount> CODEC = StreamCodec.composite(
                ITEM_CODEC, NearbyCount::item,
                ByteBufCodecs.VAR_INT, NearbyCount::count,
                NearbyCount::new);
    }

    /** Items usable for crafting in containers around the player. */
    public record NearbyItems(List<NearbyCount> items) implements CustomPacketPayload {
        public static final Type<NearbyItems> TYPE = typeOf("nearby_items");
        public static final StreamCodec<RegistryFriendlyByteBuf, NearbyItems> CODEC = StreamCodec.composite(
                NearbyCount.CODEC.apply(ByteBufCodecs.list(4096)), NearbyItems::items, NearbyItems::new);

        @Override
        public Type<NearbyItems> type() {
            return TYPE;
        }
    }

    /** Every item kind in the containers within {@code radius} blocks, with totals (for the search screen). */
    public record StorageSummary(List<NearbyCount> items, int radius) implements CustomPacketPayload {
        public static final Type<StorageSummary> TYPE = typeOf("storage_summary");
        public static final StreamCodec<RegistryFriendlyByteBuf, StorageSummary> CODEC = StreamCodec.composite(
                NearbyCount.CODEC.apply(ByteBufCodecs.list(8192)), StorageSummary::items,
                ByteBufCodecs.VAR_INT, StorageSummary::radius,
                StorageSummary::new);

        @Override
        public Type<StorageSummary> type() {
            return TYPE;
        }
    }

    /** Make these containers sparkle for a while (targets of a quick stack, or search results). */
    public record Highlight(List<BlockPos> positions, int ticks, boolean search) implements CustomPacketPayload {
        public static final Type<Highlight> TYPE = typeOf("highlight");
        public static final StreamCodec<RegistryFriendlyByteBuf, Highlight> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(1024)), Highlight::positions,
                ByteBufCodecs.VAR_INT, Highlight::ticks,
                ByteBufCodecs.BOOL, Highlight::search,
                Highlight::new);

        @Override
        public Type<Highlight> type() {
            return TYPE;
        }
    }
}
