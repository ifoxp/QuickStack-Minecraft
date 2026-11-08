package top.ifoxp.quickstack.platform;

import java.nio.file.Path;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import top.ifoxp.quickstack.QuickStackFabric;
import top.ifoxp.quickstack.platform.services.IPlatformHelper;
import top.ifoxp.quickstack.server.LockManager;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public long getLockMask(Player player) {
        return player.getAttachedOrElse(QuickStackFabric.LOCKS, LockManager.DEFAULT_MASK);
    }

    @Override
    public void setLockMask(Player player, long mask) {
        player.setAttached(QuickStackFabric.LOCKS, mask);
    }

    @Override
    public boolean isExcluded(BlockEntity blockEntity) {
        return Boolean.TRUE.equals(blockEntity.getAttached(QuickStackFabric.EXCLUDED));
    }

    @Override
    public void setExcluded(BlockEntity blockEntity, boolean excluded) {
        blockEntity.setAttached(QuickStackFabric.EXCLUDED, excluded ? Boolean.TRUE : null);
    }
}
