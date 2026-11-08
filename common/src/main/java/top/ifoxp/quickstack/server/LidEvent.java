package top.ifoxp.quickstack.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
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
        if (blockEntity instanceof ChestBlockEntity chest && state.getBlock() instanceof ChestBlock) {
            if (hasViewer(chest)) {
                return;
            }
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            sound(open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE);
        } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            sound(open ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE);
        } else if (blockEntity instanceof BarrelBlockEntity barrel && state.hasProperty(BarrelBlock.OPEN)) {
            if (hasViewer(barrel) || state.getValue(BarrelBlock.OPEN) == open) {
                return;
            }
            level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), 3);
            sound(open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE);
        }
    }

    /** A player has this chest or barrel open (the same test as the block entity's opener counter). */
    private boolean hasViewer(Container container) {
        return level.players().stream().anyMatch(player -> player.containerMenu instanceof ChestMenu menu
                && (menu.getContainer() == container
                        || menu.getContainer() instanceof CompoundContainer compound && compound.contains(container)));
    }

    private void sound(SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
