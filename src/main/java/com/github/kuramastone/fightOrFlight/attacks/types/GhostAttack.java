package com.github.kuramastone.fightOrFlight.attacks.types;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.attacks.AttackInstance;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import com.github.kuramastone.fightOrFlight.utils.EntityUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GhostAttack extends PokeAttack {

    public static List<UUID> EVOKER_FANGS = new ArrayList<>();

    public GhostAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.INSTANCE.getGHOST(), isRanged);
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
        private final int maxAttacks = 1;
        private final float power;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed, float power) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, Math.max(attackSpeed, 10));
            this.power = power;

            particleMain = loadParticle(target.level(), "block{block_state:{Name:cobblestone}}");
        }

        List<Vec3> blocksToPlaceFangs = new ArrayList<>();

        @Override
        protected void start() {
            AABB aabb = target.getBoundingBox().inflate(0.1, 0.0, 0.1);

            for (double x = aabb.minX; x <= aabb.maxX; x += 0.9) {
                for (double z = aabb.minZ; z <= aabb.maxZ; z += 0.9) {
                    blocksToPlaceFangs.add(new Vec3(x, aabb.minY, z));
                }
            }

            if (!blocksToPlaceFangs.isEmpty() && hasPassedSectionsOf(blocksToPlaceFangs.size())) {
                for (Vec3 pos : blocksToPlaceFangs) {
                    EvokerFangs fangs = new EvokerFangs(target.level(), pos.x(), pos.y(), pos.z(), 0, maxTicks-8,
                            pokemonEntity.getOwner() == null ? pokemonEntity : pokemonEntity.getOwner());
                    EVOKER_FANGS.add(fangs.getUUID());
                    target.level().addFreshEntity(fangs);
                    EntityUtils.entitiesToNotSave.add(fangs.getUUID()); // guarantees that it wont wrongly save
                }
            }

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, maxTicks, 255, false, false, false));
        }

        @Override
        protected void end() {
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);

            target.setDeltaMovement(0, -0.1, 0);
            if (hasPassedSectionsOf(maxAttacks)) {
                calculateDamage(1.0 / maxAttacks, isSpecial, getElementalType(), pokemonEntity, target);
                target.setDeltaMovement(0, -0.1, 0);
            }

        }
    }

}


















