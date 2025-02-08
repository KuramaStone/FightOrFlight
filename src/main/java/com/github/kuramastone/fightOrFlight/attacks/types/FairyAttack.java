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
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class FairyAttack extends PokeAttack {

    public FairyAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.INSTANCE.getFAIRY(), isRanged);
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
        int attackSpeed = (int) (20 * Math.min(2, 200.0 / pokemonEntity.getPokemon().getSpeed()));

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
        private final int maxAttacks = 8;
        private final float power;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed, float power) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, Math.max(attackSpeed, 10));
            this.power = power;

            particleMain = ParticleTypes.CHERRY_LEAVES;
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);

            // spawn cherry blossoms around face
            float bodyRadius = target.getBbWidth();
            Vec3 faceCenter = target.getEyePosition().add(target.getLookAngle().normalize().multiply(bodyRadius, bodyRadius, bodyRadius));
            spawnRandomlyInsideAABB(()->particleMain, target.getBoundingBox().inflate(0.2));
            for(int i = 0; i < 8 * Math.max(1, power); i++) {
                float circleRadius = 0.2f;
                Vec3 pos = faceCenter.add(
                        random.nextDouble() * circleRadius, random.nextDouble() * circleRadius, random.nextDouble() * circleRadius
                );

                Vec3 delta = pos.subtract(faceCenter);

                spawnParticleAt(particleMain, pos, 1, delta.x, delta.y, delta.z, 0.01);
            }

            if (hasPassedSectionsOf(maxAttacks)) {
                calculateDamage(1.0 / maxAttacks, isSpecial, getElementalType(), pokemonEntity, target);
                target.level().playSeededSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.HOSTILE,
                        1.0f, 1.0f, random.nextLong());
            }

        }
    }

}


















