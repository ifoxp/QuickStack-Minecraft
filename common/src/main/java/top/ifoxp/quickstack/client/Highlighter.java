package top.ifoxp.quickstack.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Makes containers sparkle: green for containers that received quick-stacked items, white rods for search
 * results. Particles instead of outline rendering keep this independent of the renderer, which changes a lot
 * between Minecraft versions.
 */
public final class Highlighter {

    private static final Map<BlockPos, Entry> ACTIVE = new HashMap<>();

    private record Entry(int ticksLeft, boolean search) {
    }

    private Highlighter() {
    }

    public static void add(List<BlockPos> positions, int ticks, boolean search) {
        ClientLevel level = Minecraft.getInstance().level;
        for (BlockPos pos : positions) {
            put(pos.immutable(), ticks, search);
            // A double chest arrives as one position; light up its other half too.
            if (level != null) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                        && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                    put(pos.relative(ChestBlock.getConnectedDirection(state)), ticks, search);
                }
            }
        }
    }

    private static void put(BlockPos pos, int ticks, boolean search) {
        ACTIVE.merge(pos, new Entry(ticks, search),
                (old, fresh) -> new Entry(Math.max(old.ticksLeft, fresh.ticksLeft), old.search || fresh.search));
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (ACTIVE.isEmpty() || level == null) {
            return;
        }
        RandomSource random = level.getRandom();
        ACTIVE.replaceAll((pos, entry) -> new Entry(entry.ticksLeft - 1, entry.search));
        ACTIVE.entrySet().removeIf(e -> e.getValue().ticksLeft <= 0);
        ACTIVE.forEach((pos, entry) -> {
            if (entry.search && entry.ticksLeft % 2 == 0) {
                // A rising column above search results, visible over walls and from a distance.
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 0.01, 0.18, (random.nextDouble() - 0.5) * 0.01);
            }
            if (entry.ticksLeft % 3 != 0) {
                return;
            }
            ParticleOptions particle = entry.search ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER;
            int count = entry.search ? 3 : 2;
            for (int i = 0; i < count; i++) {
                // A random point on the block's surface.
                double x = random.nextDouble();
                double y = random.nextDouble();
                double z = random.nextDouble();
                switch (random.nextInt(3)) {
                    case 0 -> x = random.nextBoolean() ? -0.05 : 1.05;
                    case 1 -> y = random.nextBoolean() ? -0.05 : 1.05;
                    default -> z = random.nextBoolean() ? -0.05 : 1.05;
                }
                level.addParticle(particle, pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0, entry.search ? 0.01 : 0, 0);
            }
        });
    }

    public static boolean isHighlighted(BlockPos pos) {
        return ACTIVE.containsKey(pos);
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
