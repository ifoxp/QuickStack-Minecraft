package top.ifoxp.quickstack.server;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A container in the world. A double chest is one target: {@code container} spans both halves, {@code pos} is
 * the half with the smaller position (stable key) and {@code center} sits between them.
 */
public record ContainerTarget(BlockPos pos, Container container, List<BlockEntity> parts, Vec3 center) {

    public BlockEntity primary() {
        return parts.getFirst();
    }

    /** Shulker boxes refuse items that cannot go inside container items (other shulker boxes). */
    public boolean isShulkerBox() {
        return primary() instanceof ShulkerBoxBlockEntity;
    }
}
