package com.github.kuramastone.fightOrFlight.attacks.types;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.attacks.AttackInstance;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class WaterAttack extends PokeAttack {

    public WaterAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.WATER, isRanged);
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
        WrappedPokemon wrappedPokemon = api.getWrappedPokemon(pokemonEntity);

        // spe:100 = 2 seconds. spe:400 = 0.5 seconds
        int attackSpeed = (int) (20 * Math.min(2, 100.0 / pokemonEntity.getPokemon().getSpeed()));

        boolean isSpecial = wrappedPokemon.shouldUseSpecialAttack();
        float power = (isSpecial ? pokemonEntity.getPokemon().getSpecialAttack() : pokemonEntity.getPokemon().getAttack()) / 100.0f;

        // send a line of particles towards the target over time
        new PrivateAttackInstance(this, pokemonEntity, target, future, isSpecial, attackSpeed, power).schedule();

    }

    /**
     * Shoot fireballs while a beam of particles travels to the target
     */
    private class PrivateAttackInstance extends AttackInstance {

        private final ParticleOptions particleMain;
        private final ParticleOptions particleAlt;
        private int maxAttacks = 1;
        private final float power;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed, float power) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, Math.max(attackSpeed, 10));
            this.power = power;

            particleMain = ParticleTypes.BUBBLE;
            particleAlt = ParticleTypes.BUBBLE_POP;
        }

        @Override
        protected void start() {
        }

        @Override
        protected void end() {
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);

            spawnGradualLineOfParticles(pokemonEntity.getEyePosition(), target.getBoundingBox().getCenter(),
                    ()->random.nextDouble() < 0.8 ? particleMain : particleAlt,
                    5, 4, 0.15f * power / 2, 0.15f * power / 2, 0.15f * power / 2, 0
            );

            if (hasPassedSectionsOf(maxAttacks)) {
                calculateDamage(1.0 / maxAttacks, isSpecial, getElementalType(), pokemonEntity, target);

                AABB aabb = target.getBoundingBox().inflate(0.5, 0.5, 0.5);
                for(int i = 0; i < 30 * Math.max(1, power); i++)
                    spawnRandomlyInsideAABB(()->random.nextDouble() < 0.8 ? particleMain : particleAlt, aabb);

                target.level().playSeededSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE,
                        1.0f, 1.0f, random.nextLong());
            }

        }
    }

}


















