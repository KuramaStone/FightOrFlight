package com.github.kuramastone.fightOrFlight.attacks.types;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.attacks.AttackInstance;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import com.github.kuramastone.fightOrFlight.utils.ReflectionUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SteelAttack extends PokeAttack {

    public SteelAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.STEEL, isRanged);
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
        int attackSpeed = 10;

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
        private final int maxAttacks = 1;
        private final float power;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed, float power) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, isSpecial ? Math.max(attackSpeed, 10) : 1);
            this.power = power;

            particleMain = loadParticle(target.level(), "dust{color:[0.0,255.0,0.0],scale:1.0}");
            particleAlt = loadParticle(target.level(), "dust{color:[255.0,0.0,255.0],scale:1.0}");
        }

        // fallingBlock doesnt drop an item and won't hurt entities
        FallingBlockEntity fallingBlock;
        Vec3 start;

        @Override
        protected void start() {
            if (isSpecial) {
                start = target.position().add(0, 4, 0);
                fallingBlock = new FallingBlockEntity(EntityType.FALLING_BLOCK, target.level());
                try {
                    ReflectionUtils.setFallingBlockEntityBlockState(fallingBlock, Blocks.ANVIL.defaultBlockState());
                    ReflectionUtils.setFallingBlockEntityCancelDrop(fallingBlock, true);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                fallingBlock.setPos(start);
                fallingBlock.setNoGravity(true);
                fallingBlock.setDeltaMovement(0, -4.0 / maxTicks, 0);
                fallingBlock.dropItem = false;

                target.level().addFreshEntity(fallingBlock);
            }
        }

        @Override
        protected void end() {
            if (fallingBlock != null) {
                if (!fallingBlock.isRemoved())
                    fallingBlock.remove(Entity.RemovalReason.DISCARDED);
            }
            target.level().playSeededSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.HOSTILE,
                    1.0f, 1.0f, random.nextLong());
            calculateDamage(1.0 / maxAttacks, isSpecial, getElementalType(), pokemonEntity, target);
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);

            if (fallingBlock != null && !fallingBlock.isRemoved()) {
                // drop anvil on entity
                Vec3 newPos = interpolate(start, target.position(), (float) currentTick / maxTicks);
                FightOrFlightMod.logger.info("{}: {}", (float)  currentTick / maxTicks, newPos.y);
                fallingBlock.setPos(newPos);
            }

        }
    }

}


















