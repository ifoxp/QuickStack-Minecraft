package top.ifoxp.quickstack.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
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
        if (blockEntity instanceof ChestBlockEntity && state.getBlock() instanceof ChestBlock) {
            if (ChestBlockEntity.getOpenCount(level, pos) > 0) {
                return;
            }
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            sound(open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE);
        } else if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
            if (openByPlayer(shulker)) {
                return;
            }
            level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
            sound(open ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE);
        } else if (blockEntity instanceof BarrelBlockEntity barrel && state.hasProperty(BarrelBlock.OPEN)) {
            if (openByPlayer(barrel) || state.getValue(BarrelBlock.OPEN) == open) {
                return;
            }
            level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), 3);
            sound(open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE);
        }
    }

    /** Whether a player in this level has the container open (its opener counter is private before 1.21.5). */
    private boolean openByPlayer(Container container) {
        for (ServerPlayer player : level.players()) {
            if (player.containerMenu == player.inventoryMenu) {
                continue;
            }
            for (Slot slot : player.containerMenu.slots) {
                if (slot.container == container
                        || (slot.container instanceof CompoundContainer compound && compound.contains(container))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sound(SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
