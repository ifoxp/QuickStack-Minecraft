package top.ifoxp.quickstack.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Opens a container's lid while items fly in and closes it afterwards, the way a player opening it would look,
 * unless a real player has it open (then the vanilla opener count owns the lid).
 */
record LidEvent(ServerLevel level, BlockPos pos, long tick, boolean open) {

    void apply() {
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockState state = level.getBlockState(pos);
        if (blockEntity instanceof ChestBlockEntity chest && state.getBlock() instanceof ChestBlock chestBlock) {
            if (!chest.getEntitiesWithContainerOpen().isEmpty()) {
                return;
            }
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            sound(open ? chestBlock.getOpenChestSound() : chestBlock.getCloseChestSound());
        } else if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
            if (!shulker.getEntitiesWithContainerOpen().isEmpty()) {
                return;
            }
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            sound(open ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE);
        } else if (blockEntity instanceof BarrelBlockEntity barrel && state.hasProperty(BarrelBlock.OPEN)) {
            if (!barrel.getEntitiesWithContainerOpen().isEmpty() || state.getValue(BarrelBlock.OPEN) == open) {
                return;
            }
            level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), 3);
            sound(open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE);
        }
    }

    private void sound(SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
