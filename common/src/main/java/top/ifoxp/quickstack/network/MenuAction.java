package top.ifoxp.quickstack.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Buttons that act on the menu the player has open. */
public enum MenuAction {
    /** Put unlocked items into the open container when it already holds the same item. */
    DEPOSIT_MATCHING,
    /** Take items the player already carries from the open container, leaving a few behind. */
    RESTOCK,
    /** Put every unlocked item into the open container. */
    DEPOSIT_ALL,
    /** Take everything from the open container. */
    LOOT_ALL,
    SORT_CONTAINER,
    SORT_INVENTORY,
    /** Exclude the open container from (or include it again in) quick stack to nearby containers. */
    TOGGLE_EXCLUDED;

    private static final MenuAction[] VALUES = values();

    public static final StreamCodec<ByteBuf, MenuAction> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            i -> VALUES[Math.clamp(i, 0, VALUES.length - 1)], MenuAction::ordinal);
}
