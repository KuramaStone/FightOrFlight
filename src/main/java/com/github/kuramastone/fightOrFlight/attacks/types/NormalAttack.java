package com.github.kuramastone.fightOrFlight.attacks.types;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.attacks.AttackInstance;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class NormalAttack extends PokeAttack {

    public NormalAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.INSTANCE.getNORMAL(), isRanged);
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
        private int maxAttacks = 1;

        private final ParticleOptions beamParticleMain;
        private final ParticleOptions beamParticleAlt;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, isSpecial ? Math.max(attackSpeed, 10) : 1);
            if(!isSpecial) {
                maxAttacks = 1;
            }

            beamParticleMain = loadParticle(target.level(), "dust{color:[255.0,255.0,255.0],scale:0.5}");
            beamParticleAlt = loadParticle(target.level(), "dust{color:[255.0,255.0,0.0],scale:0.5}");
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);
            Vec3 start = pokemonEntity.getEyePosition();
            Vec3 end = target.getBoundingBox().getCenter();

            if (isSpecial) {
                float dist = (float) start.distanceTo(end);
                spawnGradualSpiralParticles(start, end, () -> beamParticleMain, 4, Math.round(dist * 2), 0.5f, 0);
                spawnGradualSpiralParticles(start, end, () -> beamParticleMain, 4, Math.round(dist * 2), 0.5f, 3.141f);
                spawnImmediateLineOfParticles(start, end, () -> beamParticleMain, 8,3, 0f, 0f, 0f, 0);
            }

            // launch fireball periodically. Copied from Blaze
            if (hasPassedSectionsOf(maxAttacks)) {
                calculateDamage(1.0 / maxAttacks, isSpecial, getElementalType(), pokemonEntity, target);
                target.level().playSeededSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.0f, 1.0f, random.nextLong());
            }
        }
    }

    public static class FireballAttackData {
        public PokemonEntity pokemonEntity;
        public int fireballsSent;

        public FireballAttackData(PokemonEntity pokemonEntity, int fireballsSent) {
            this.pokemonEntity = pokemonEntity;
            this.fireballsSent = fireballsSent;
        }
    }

}


















