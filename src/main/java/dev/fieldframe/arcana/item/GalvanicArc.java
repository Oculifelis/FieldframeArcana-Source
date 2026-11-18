package dev.fieldframe.arcana.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The arc the Galvanic sword and bow throw from a struck mob to the nearest hostile one beside it. */
public final class GalvanicArc {
    private GalvanicArc() {
    }

    /** The nearest living hostile mob within {@code range} of {@code target}, other than it and the attacker. */
    public static @Nullable LivingEntity nearest(ServerLevel level, LivingEntity target, @Nullable LivingEntity attacker, double range) {
        LivingEntity next = null;
        double best = range * range;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(range),
                e -> e != target && e != attacker && e.isAlive() && e instanceof Enemy)) {
            double d = e.distanceToSqr(target);
            if (d < best) {
                best = d;
                next = e;
            }
        }
        return next;
    }

    /** A spark line from {@code target} to {@code next}, then {@code damage} as magic to it. */
    public static void zap(ServerLevel level, LivingEntity target, LivingEntity next, @Nullable LivingEntity attacker, float damage) {
        Vec3 from = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 to = next.position().add(0, next.getBbHeight() * 0.5, 0);
        for (int i = 0; i <= 8; i++) {
            Vec3 p = from.lerp(to, i / 8.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.02);
        }
        next.hurtServer(level, level.damageSources().indirectMagic(attacker, attacker), damage);
        level.playSound(null, next.getX(), next.getY(), next.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    /** A burst of sparks on the struck mob. */
    public static void burst(ServerLevel level, LivingEntity target) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                6, 0.2, 0.3, 0.2, 0.1);
    }
}
