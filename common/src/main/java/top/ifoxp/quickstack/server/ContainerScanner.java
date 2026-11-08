package top.ifoxp.quickstack.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import top.ifoxp.quickstack.config.ServerConfig;
import top.ifoxp.quickstack.platform.Services;

/**
 * Finds storage containers around a player. It walks the block entity maps of the already loaded chunks in
 * range (never loads chunks and never scans blocks), so a radius of 8 touches a handful of chunks.
 */
public final class ContainerScanner {

    private ContainerScanner() {
    }

    public enum Purpose {
        /** Quick stack to nearby: honours the per-container exclusion. */
        QUICK_STACK,
        /** Crafting and item search: every usable container counts. */
        READ
    }

    /** Containers within {@code radius} blocks of the player's eyes, nearest first. */
    public static List<ContainerTarget> find(ServerPlayer player, int radius, Purpose purpose) {
        List<ContainerTarget> result = new ArrayList<>();
        if (player.isSpectator()) {
            return result;
        }
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        double maxDistSqr = (radius + 0.5) * (radius + 0.5);
        int minChunkX = SectionPos.blockToSectionCoord(eye.x - radius);
        int maxChunkX = SectionPos.blockToSectionCoord(eye.x + radius);
        int minChunkZ = SectionPos.blockToSectionCoord(eye.z - radius);
        int maxChunkZ = SectionPos.blockToSectionCoord(eye.z + radius);
        Set<BlockPos> seen = new HashSet<>();

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (seen.contains(pos) || Vec3.atCenterOf(pos).distanceToSqr(eye) > maxDistSqr) {
                        continue;
                    }
                    ContainerTarget target = resolve(level, blockEntity);
                    if (target == null) {
                        continue;
                    }
                    for (BlockEntity part : target.parts()) {
                        seen.add(part.getBlockPos());
                    }
                    if (usableBy(target, player, purpose)) {
                        result.add(target);
                    }
                }
            }
        }
        result.sort(Comparator.comparingDouble(t -> t.center().distanceToSqr(eye)));
        return result;
    }

    /** The container at {@code pos}, or null when there is no longer an acceptable one. */
    public static ContainerTarget resolve(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? null : resolve(level, blockEntity);
    }

    private static ContainerTarget resolve(ServerLevel level, BlockEntity blockEntity) {
        if (blockEntity.isRemoved() || !(blockEntity instanceof Container) || !accepts(blockEntity)) {
            return null;
        }
        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = blockEntity.getBlockState();
        if (blockEntity instanceof ChestBlockEntity && state.getBlock() instanceof ChestBlock chestBlock
                && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
            BlockEntity other = level.getBlockEntity(otherPos);
            Container combined = ChestBlock.getContainer(chestBlock, state, level, pos, true);
            if (other instanceof ChestBlockEntity && combined != null) {
                // Order halves the way the combined container does (first = the one at pos when it is the
                // "left" half), but key the target by the smaller position so both halves resolve to one key.
                BlockPos key = pos.compareTo(otherPos) < 0 ? pos : otherPos;
                BlockEntity first = key.equals(pos) ? blockEntity : other;
                BlockEntity second = key.equals(pos) ? other : blockEntity;
                Vec3 center = Vec3.atCenterOf(pos).add(Vec3.atCenterOf(otherPos)).scale(0.5);
                return new ContainerTarget(key, combined, List.of(first, second), center);
            }
        }
        return new ContainerTarget(pos, (Container) blockEntity, List.of(blockEntity), Vec3.atCenterOf(pos));
    }

    private static boolean accepts(BlockEntity blockEntity) {
        ServerConfig config = ServerConfig.get();
        if (!config.blockedContainerTypes.isEmpty()) {
            Identifier typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            if (typeId != null && config.blockedContainerTypes.contains(typeId.toString())) {
                return false;
            }
        }
        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity) {
            return true;
        }
        // Modded chests usually extend BaseContainerBlockEntity; the slot threshold keeps hoppers, droppers,
        // furnaces and machines out.
        return config.allowModdedContainers && blockEntity instanceof BaseContainerBlockEntity container
                && container.getContainerSize() >= config.moddedContainerMinSlots;
    }

    private static boolean usableBy(ContainerTarget target, ServerPlayer player, Purpose purpose) {
        for (BlockEntity part : target.parts()) {
            // Never touch loot chests nobody has opened yet: that would generate (and spoil) their loot.
            if (part instanceof RandomizableContainer randomizable && randomizable.getLootTable() != null) {
                return false;
            }
            if (part instanceof BaseContainerBlockEntity base && !base.canOpen(player)) {
                return false;
            }
            if (purpose == Purpose.QUICK_STACK && Services.PLATFORM.isExcluded(part)) {
                return false;
            }
        }
        return true;
    }
}
