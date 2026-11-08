package top.ifoxp.quickstack.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.network.Payloads;

/**
 * Shows quick-stacked items flying: they burst out of the player, spread in a ring three blocks in front of the
 * camera, then fly one by one in an arc into their containers. The items are client-only item entities (never on
 * the server, negative ids so they cannot clash with real entities) moved every tick; the vanilla item renderer
 * draws them, with interpolation between ticks.
 */
public final class FlightAnimator {

    private static final int BURST_TICKS = 6;
    private static final double HOVER_DISTANCE = 3.0;

    private static final List<Flying> ACTIVE = new ArrayList<>();
    private static int nextId = -1_000_000;

    private FlightAnimator() {
    }

    private static final class Flying {
        final ItemEntity entity;
        final Vec3 start;
        final Vec3 hover;
        final Vec3 target;
        final int launch;
        final int arrive;
        int age;

        Flying(ItemEntity entity, Vec3 start, Vec3 hover, Vec3 target, int launch, int arrive) {
            this.entity = entity;
            this.start = start;
            this.hover = hover;
            this.target = target;
            this.launch = launch;
            this.arrive = arrive;
        }

        Vec3 positionAt(int tick) {
            if (tick < BURST_TICKS) {
                double t = (double) tick / BURST_TICKS;
                double eased = 1 - (1 - t) * (1 - t);
                return start.lerp(hover, eased);
            }
            if (tick < launch) {
                double bob = Math.sin((tick + entity.bobOffs * 10) * 0.25) * 0.04;
                return hover.add(0, bob, 0);
            }
            double t = Mth.clamp((double) (tick - launch) / Math.max(1, arrive - launch), 0, 1);
            double eased = t * t * (3 - 2 * t);
            Vec3 control = hover.lerp(target, 0.5).add(0, 0.8 + hover.distanceTo(target) * 0.12, 0);
            Vec3 a = hover.lerp(control, eased);
            Vec3 b = control.lerp(target, eased);
            return a.lerp(b, eased);
        }
    }

    public static void start(Minecraft minecraft, List<Payloads.Flight> flights, boolean cancelPrevious) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        if (cancelPrevious) {
            clear();
        }
        if (flights.isEmpty() || !ClientConfig.get().animation) {
            return;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getViewVector(1.0F);
        Vec3 center = eye.add(look.scale(HOVER_DISTANCE));
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 start = eye.add(0, -0.45, 0).add(look.scale(0.3));

        int count = flights.size();
        double radius = count == 1 ? 0 : 0.35 + 0.09 * Math.sqrt(count);
        for (int i = 0; i < count; i++) {
            Payloads.Flight flight = flights.get(i);
            double angle = Math.PI * 2 * i / count;
            // Several rings for big batches so the items do not overlap too much.
            double ringRadius = radius * (1.0 - 0.35 * (i % 3) / 2.0);
            Vec3 hover = center.add(right.scale(Math.cos(angle) * ringRadius)).add(up.scale(Math.sin(angle) * ringRadius));
            ItemEntity entity = createEntity(level, flight, start);
            ACTIVE.add(new Flying(entity, start, hover, flight.target(), flight.launchTick(), flight.arriveTick()));
        }
    }

    private static ItemEntity createEntity(ClientLevel level, Payloads.Flight flight, Vec3 start) {
        ItemEntity entity = new ItemEntity(EntityTypes.ITEM, level) {
            @Override
            public void tick() {
                // Positions come from FlightAnimator; commonTick() already keeps the previous position for interpolation.
            }

            @Override
            public boolean isPickable() {
                return false;
            }
        };
        entity.setItem(flight.stack());
        entity.setId(nextId--);
        if (nextId > -1000) {
            nextId = -1_000_000;
        }
        entity.setNoGravity(true);
        entity.noPhysics = true;
        entity.setPos(start.x, start.y, start.z);
        entity.setOldPosAndRot();
        level.addEntity(entity);
        return entity;
    }

    public static void tick(Minecraft minecraft) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        ClientLevel level = minecraft.level;
        for (Iterator<Flying> it = ACTIVE.iterator(); it.hasNext(); ) {
            Flying flying = it.next();
            flying.age++;
            if (flying.entity.level() != level || flying.entity.isRemoved()) {
                it.remove();
                continue;
            }
            if (flying.age >= flying.arrive) {
                arrive(level, flying);
                it.remove();
                continue;
            }
            Vec3 pos = flying.positionAt(flying.age);
            flying.entity.setPos(pos.x, pos.y, pos.z);
        }
    }

    private static void arrive(ClientLevel level, Flying flying) {
        Vec3 target = flying.target;
        level.removeEntity(flying.entity.getId(), Entity.RemovalReason.DISCARDED);
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.POOF,
                    target.x + (level.getRandom().nextDouble() - 0.5) * 0.4,
                    target.y + 0.5,
                    target.z + (level.getRandom().nextDouble() - 0.5) * 0.4,
                    0, 0.02, 0);
        }
        level.playLocalSound(target.x, target.y, target.z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.25F, 1.2F + level.getRandom().nextFloat() * 0.6F, false);
        if (ClientConfig.get().highlightTargets) {
            Highlighter.add(List.of(BlockPos.containing(target)), 30, false);
        }
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (Flying flying : ACTIVE) {
            if (minecraft.level != null && flying.entity.level() == minecraft.level) {
                minecraft.level.removeEntity(flying.entity.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
        ACTIVE.clear();
    }
}
