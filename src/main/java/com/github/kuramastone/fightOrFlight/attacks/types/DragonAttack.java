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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.github.kuramastone.fightOrFlight.utils.Utils.style;

public class DragonAttack extends PokeAttack {

    public static Map<DragonFireball, FireAttack.FireballAttackData> fireballsLaunched = new HashMap<>();

    public DragonAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.INSTANCE.getDRAGON(), isRanged);
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
        private final ParticleOptions particleAlt;
        private int maxAttacks = 1;
        private final float power;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed, float power) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, Math.min(20, attackSpeed));
            this.power = power;
            if(isSpecial)
                maxAttacks = Math.max(1, Math.round(power));

            particleMain = ParticleTypes.SMALL_GUST;
            particleAlt = loadParticle(target.level(), "dust{color:[0.0,0.0,255.0],scale:1.0}");
        }

        @Override
        protected void start() {
            if (!isSpecial) {
                calculateDamage(1.0, isSpecial, getElementalType(), pokemonEntity, target);
                spawnParticleAt(particleMain, target.position(), 3, 1, 1, 1, 0);
                target.level().playSeededSound(null, pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
                        SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.0f, 0.5f, random.nextLong());
            }
        }

        @Override
        protected void end() {

        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);

            if (hasPassedSectionsOf(maxAttacks)) {
                if (isSpecial) {
                    Vec3 direction = target.getBoundingBox().getCenter().subtract(pokemonEntity.getEyePosition()).normalize();
                    float attackSpeed = 10.0f / maxTicks;
                    DragonFireball fireball = new DragonFireball(this.pokemonEntity.level(), this.pokemonEntity,
                            direction.normalize().multiply(attackSpeed, attackSpeed, attackSpeed));
                    fireball.setPos(fireball.getX(), this.pokemonEntity.getY(0.5) + 0.5, fireball.getZ());
                    fireballsLaunched.put(fireball, new FireAttack.FireballAttackData(pokemonEntity, maxAttacks, isSpecial, target));
                    EntityUtils.entitiesToNotSave.add(fireball.getUUID());
                    // schedule removing this fireball in 10 seconds
                    TickScheduler.scheduleLater(200L, () -> fireball.remove(Entity.RemovalReason.DISCARDED));
                    this.pokemonEntity.level().addFreshEntity(fireball);
                    target.level().playSeededSound(null, pokemonEntity.getX(), pokemonEntity.getY(), pokemonEntity.getZ(),
                            SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 1.0f, 0.5f, random.nextLong());
                }
            }

        }
    }

}


















