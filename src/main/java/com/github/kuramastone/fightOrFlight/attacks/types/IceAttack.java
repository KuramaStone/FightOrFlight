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
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class IceAttack extends PokeAttack {

    public IceAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.ICE, isRanged);
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
        float power = pokemonEntity.getPokemon().getSpecialAttack() / 100.0f;

        // send a line of particles towards the target over time
        WrappedPokemon wrappedPokemon = api.getWrappedPokemon(pokemonEntity);
        boolean isSpecial = wrappedPokemon.shouldUseSpecialAttack();
        new PrivateAttackInstance(this, pokemonEntity, target, future, isSpecial, attackSpeed, power).schedule();

    }

    /**
     * Shoot fireballs while a beam of particles travels to the target
     */
    private class PrivateAttackInstance extends AttackInstance {

        private final ParticleOptions beamParticleMain;

        private final int maxAttacks = 4;
        private final float power;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed, float power) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, Math.max(attackSpeed, 10));
            this.power = power;

            beamParticleMain = ParticleTypes.ITEM_SNOWBALL;
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);
            Vec3 start = pokemonEntity.getEyePosition();
            Vec3 end = target.getBoundingBox().getCenter();

            // spawn snowflakes in area
            AABB aabb = new AABB(start, end).inflate(2.0f);
            for (int i = 0; i < 10 * power; i++) {
                spawnRandomlyInsideAABB(()->ParticleTypes.SNOWFLAKE, aabb);
            }

            // launch freezing beam
            if (hasPassedSectionsOf(maxAttacks)) {
                calculateDamage(1.0 / maxAttacks, isSpecial, getElementalType(), pokemonEntity, target);

                spawnImmediateLineOfParticles(start, end, () -> beamParticleMain, 4, 1, 0, 0, 0, 0);
                int frozen = (int) Math.ceil(Math.max(60, 140 * power));
                target.setTicksFrozen(frozen);
                target.level().playSeededSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE,
                        1.0f, 1.0f, random.nextLong());
                target.hurt(pokemonEntity.damageSources().mobAttack(pokemonEntity.getOwner() == null ? pokemonEntity : pokemonEntity.getOwner()), 0.0f);
            }
        }
    }

}


















