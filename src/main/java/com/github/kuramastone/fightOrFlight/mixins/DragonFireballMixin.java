package com.github.kuramastone.fightOrFlight.mixins;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.attacks.types.DragonAttack;
import com.github.kuramastone.fightOrFlight.attacks.types.FireAttack;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

@Mixin(DragonFireball.class)
public class DragonFireballMixin {


    /**
     * Inject the custom interaction on fireball hit.
     */
    @Inject(method = "onHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
            shift = At.Shift.AFTER),
            cancellable = true)
    private void onHit(HitResult hitResult, CallbackInfo ci) {
        Map<DragonFireball, FireAttack.FireballAttackData> dataMap = DragonAttack.fireballsLaunched;
        if (dataMap.containsKey(this)) {
            // override result

            FireAttack.FireballAttackData data = dataMap.get(this);

            Entity defender = null;
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                defender = ((EntityHitResult) hitResult).getEntity();

                if (defender instanceof LivingEntity livingDefender) {
                    PokeAttack.calculateDamage(1.0 / data.fireballsSent, data.isSpecial, ElementalTypes.INSTANCE.getDRAGON(), data.pokemonEntity, livingDefender);
                }
            }

            DragonFireball fireball = ((DragonFireball) (Object) this);
            fireball.level().playSeededSound(null, fireball.getX(), fireball.getY(), fireball.getZ(),
                    SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE,
                    1.0f, 1.0f, (long) (Math.random() * 100000L));

            Vec3 delta = new Vec3(0, 0, 0);
            if(defender != null)
                delta = defender.getBoundingBox().getCenter().subtract(fireball.position());
            AABB aabb = fireball.getBoundingBox().inflate(0.25);
            for (int i = 0; i < 20; i++) {
                spawnRandomlyInsideAABB((ServerLevel) fireball.level(), () -> ParticleTypes.DRAGON_BREATH, aabb, 1,
                        (float) delta.x, (float) delta.y, (float) delta.z, 0.5f);
            }

            fireball.remove(Entity.RemovalReason.DISCARDED);
            dataMap.remove(this);
            ci.cancel();
        }

    }

    /**
     * Spawns a particle inside the aabb
     */
    protected void spawnRandomlyInsideAABB(ServerLevel level, Supplier<ParticleOptions> particleOptions, AABB aabb, int count, float dx, float dy, float dz, float speed) {
        Random random = new Random();
        Vec3 pos = aabb.getMinPosition().add(
                random.nextDouble() * aabb.getXsize(),
                random.nextDouble() * aabb.getYsize(),
                random.nextDouble() * aabb.getZsize()
        );

        spawnParticleAt(
                level,
                particleOptions.get(),
                pos,
                count,
                dx, dy, dz,
                speed
        );
    }

    protected void spawnParticleAt(ServerLevel level, ParticleOptions particleOptions, Vec3 vec, int count, double dx, double dy, double dz, double speed) {
        level.sendParticles(particleOptions, vec.x, vec.y, vec.z, count, dx, dy, dz, speed);
    }

}
