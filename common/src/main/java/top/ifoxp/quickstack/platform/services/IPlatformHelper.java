package top.ifoxp.quickstack.platform.services;

import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Everything the common code needs from the mod loader. Implemented once per loader and found with
 * {@link java.util.ServiceLoader}; client-only calls live in {@link IClientPlatformHelper}.
 */
public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    Path getConfigDir();

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Slots of the player's main inventory (0-35) that quick stack and sorting must leave alone, as a bit mask. */
    long getLockMask(Player player);

    void setLockMask(Player player, long mask);

    /** Whether the player excluded this container from quick stacking to nearby chests. */
    boolean isExcluded(BlockEntity blockEntity);

    void setExcluded(BlockEntity blockEntity, boolean excluded);
}
