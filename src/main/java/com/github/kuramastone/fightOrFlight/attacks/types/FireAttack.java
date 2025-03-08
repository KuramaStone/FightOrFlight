package com.github.kuramastone.fightOrFlight.attacks.types;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.attacks.AttackInstance;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import com.github.kuramastone.fightOrFlight.utils.EntityUtils;
import com.github.kuramastone.fightOrFlight.utils.TickScheduler;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FireAttack extends PokeAttack {

    public static Map<SmallFireball, FireballAttackData> fireballsLaunched = new HashMap<>();

    public FireAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.INSTANCE.getFIRE(), isRanged);
    }

    @Override
    public CompletableFuture<Boolean> perform(PokemonEntity pokemonEntity, LivingEntity target) {
        Objects.requireNonNull(pokemonEntity, "Pokemon cannot be null!");
        Objects.requireNonNull(target, "Target cannot be null!");

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        beginAttack(pokemonEntity, target, future);


        return future;
    }

    private void beginAttack(PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future) {

        // spe:100 = 2 seconds. spe:400 = 0.5 seconds
        int attackSpeed = (int) (20 * Math.min(2, 200.0 / pokemonEntity.getPokemon().getSpeed()));

        // send a line of particles towards the target over time
        WrappedPokemon wrappedPokemon = api.getWrappedPokemon(pokemonEntity);
        boolean isSpecial = wrappedPokemon.shouldUseSpecialAttack();
        new PrivateAttackInstance(this, pokemonEntity, target, future, isSpecial, attackSpeed).schedule();

    }

    /**
     * Shoot fireballs while a beam of particles travels to the target
     */
    private class PrivateAttackInstance extends AttackInstance {

        private static final int spiralCount = 10;
        private final int maxAttacks = 4;

        private final ParticleOptions beamParticleMain;
        private final ParticleOptions beamParticleAlt;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, Math.max(attackSpeed, 10));

            beamParticleMain = loadParticle(target.level(), "dust{color:[255.0,0.0,0.0],scale:0.5}");
            beamParticleAlt = loadParticle(target.level(), "dust{color:[255.0,255.0,0.0],scale:0.5}");
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);
            Vec3 start = pokemonEntity.getEyePosition();
            Vec3 end = target.getBoundingBox().getCenter().add(target.getDeltaMovement());

            spawnGradualSpiralParticles(start, end, () -> beamParticleMain, 4, spiralCount, 0.25f, 0.0f);
            spawnGradualLineOfParticles(start, end, () -> beamParticleAlt, 2);
            spawnGradualSpiralParticles(start, end, () -> beamParticleMain, 4, spiralCount, 0.4f, 3.141f);


            // launch fireball periodically. Copied from Blaze
            if (hasPassedSectionsOf(maxAttacks)) {
                Vec3 direction = end.subtract(start).normalize();
                float attackSpeed = 10.0f / maxTicks;
                SmallFireball smallFireball = new SmallFireball(this.pokemonEntity.level(), this.pokemonEntity, direction.normalize().multiply(attackSpeed, attackSpeed, attackSpeed));
                smallFireball.setPos(smallFireball.getX(), this.pokemonEntity.getY(0.5) + 0.5, smallFireball.getZ());
                fireballsLaunched.put(smallFireball, new FireballAttackData(pokemonEntity, maxAttacks, isSpecial, target));
                // schedule removing this fireball in 10 seconds
                TickScheduler.scheduleLater(200L, () -> smallFireball.remove(Entity.RemovalReason.DISCARDED));
                EntityUtils.entitiesToNotSave.add(smallFireball.getUUID());
                this.pokemonEntity.level().addFreshEntity(smallFireball);
                target.level().playSeededSound(null, smallFireball.getX(), smallFireball.getY(), smallFireball.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0f, 1.0f, random.nextLong());
            }
        }
    }

    public static class FireballAttackData {
        public PokemonEntity pokemonEntity;
        public int fireballsSent;
        public boolean isSpecial;
        public LivingEntity targetEntity;

        public FireballAttackData(PokemonEntity pokemonEntity, int fireballsSent, boolean isSpecial, LivingEntity targetEntity) {
            this.pokemonEntity = pokemonEntity;
            this.fireballsSent = fireballsSent;
            this.isSpecial = isSpecial;
            this.targetEntity = targetEntity;
        }
    }

}


















