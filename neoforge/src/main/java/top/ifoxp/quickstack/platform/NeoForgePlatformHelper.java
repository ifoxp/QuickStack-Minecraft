package top.ifoxp.quickstack.platform;

import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import top.ifoxp.quickstack.QuickStackNeoForge;
import top.ifoxp.quickstack.platform.services.IPlatformHelper;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public long getLockMask(Player player) {
        return player.getData(QuickStackNeoForge.LOCKS);
    }

    @Override
    public void setLockMask(Player player, long mask) {
        player.setData(QuickStackNeoForge.LOCKS, mask);
    }

    @Override
    public boolean isExcluded(BlockEntity blockEntity) {
        return blockEntity.hasData(QuickStackNeoForge.EXCLUDED) && blockEntity.getData(QuickStackNeoForge.EXCLUDED);
    }

    @Override
    public void setExcluded(BlockEntity blockEntity, boolean excluded) {
        if (excluded) {
            blockEntity.setData(QuickStackNeoForge.EXCLUDED, Boolean.TRUE);
        } else {
            blockEntity.removeData(QuickStackNeoForge.EXCLUDED);
        }
    }
}
