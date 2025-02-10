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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class PsychicAttack extends PokeAttack {

    public PsychicAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.INSTANCE.getPSYCHIC(), isRanged);
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

        int attackSpeed = (int) (60 * Math.min(2, 200.0 / pokemonEntity.getPokemon().getSpeed()));
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

        private final ParticleOptions particleMain;
        private final ParticleOptions particleAlt;

        private int maxAttacks = 4;
        private final float power;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed, float power) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, Math.max(attackSpeed, 10));
            this.power = power;

            particleMain = loadParticle(pokemonEntity.level(), "dust{color:[255.0,0.0,255.0],scale:1.0}");
            particleAlt = loadParticle(pokemonEntity.level(), "dust{color:[255.0,0.0,255.0],scale:0.5}");
        }

        @Override
        protected void start() {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, (int) Math.min(30, (0.5 * maxTicks * Math.min(1.25, power))), 1, false, false, false));
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);

            boolean dealtDamage = false;

            if (hasPassedSectionsOf(maxAttacks)) {
                calculateDamage(1.0 / maxAttacks, isSpecial, getElementalType(), pokemonEntity, target);
                target.level().playSeededSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.WOLF_SHAKE, SoundSource.HOSTILE,
                        4.0f, 1.0f, random.nextLong());
                target.level().playSeededSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.WOLF_SHAKE, SoundSource.HOSTILE,
                        0.25f, 1.0f, random.nextLong());
                dealtDamage = true;
            }

            // create aura around target
            float radius = target.getBbWidth();
            int particlesPerBlock = 3;
            double perimeter = 2 * Math.PI * radius;
            int totalParticles = (int) Math.ceil(particlesPerBlock * perimeter);

            float distanceBetweenWaves = 2.0f;
            float sinFrequency = (float) ((2.0f * Math.PI * radius) / (distanceBetweenWaves));// total waves around perimeter
            float sinAmplitude = 0.25f;
            float height = target.getBbHeight();
            for (int particleIndex = 0; particleIndex < totalParticles; particleIndex++) {
                double theta = particleIndex * (Math.PI * 2.0) / totalParticles;
                Vec3 pos = target.position().add(Math.cos(theta) * radius, 0, Math.sin(theta) * radius);

                int maxRings = (int) Math.ceil(Math.max(1, power));
                float heightPerRing = height / (1.0f + maxRings);
                for (int rings = 0; rings < maxRings; rings++) {
                    float offY =
                            sinAmplitude + heightPerRing * rings // base height
                                    + sharpenSin((float) Math.sin(theta * sinFrequency)) * sinAmplitude; // height of sin
                    Vec3 pos2 = pos.add(0, offY, 0);
                    spawnParticleAt(dealtDamage ? particleMain : particleAlt, pos2, 1, 0, 0, 0, 0);
                }

            }

        }

        private float sharpenSin(double sin) {
            return (float) (sin * Math.sin(sin));
        }
    }

}


















