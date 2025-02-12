package com.github.kuramastone.fightOrFlight.attacks.types;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.attacks.AttackInstance;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import com.github.kuramastone.fightOrFlight.utils.EntityUtils;
import com.github.kuramastone.fightOrFlight.utils.ReflectionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RockAttack extends PokeAttack {

    public RockAttack(FOFApi api, boolean isRanged) {
        super(api, ElementalTypes.INSTANCE.getROCK(), isRanged);
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
        private final int maxAttacks = 4;
        private final float power;

        public PrivateAttackInstance(PokeAttack pokeAttack, PokemonEntity pokemonEntity, LivingEntity target, CompletableFuture<Boolean> future, boolean isSpecial, int attackSpeed, float power) {
            super(pokeAttack, pokemonEntity, target, future, isSpecial, Math.max(attackSpeed, 10));
            this.power = power;

            particleMain = loadParticle(target.level(), "block{block_state:{Name:cobblestone}}");
        }

        List<List<BlockPos>> blocksToPlace = new ArrayList<>();
        List<Display.BlockDisplay> displayEntities = new ArrayList<>();

        @Override
        protected void start() {
            FightOrFlightMod.debug("Starting rock attack {}: ", uid);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, maxTicks, 255, false, false, false));

            AABB aabb = target.getBoundingBox().inflate(0.1, 0, 0.1);

            FightOrFlightMod.debug("{}: aabb: {}", uid, aabb);

            for (int y = (int) aabb.minY; y <= (int) aabb.maxY; y++) {
                List<BlockPos> layer = new ArrayList<>();
                for (int x = (int) aabb.minX; x <= (int) aabb.maxX; x++) {
                    for (int z = (int) aabb.minZ; z <= (int) aabb.maxZ; z++) {
                        int x2 = x;
                        int z2 = z;
                        if (x2 < 0)
                            x2 -= 1;
                        if (z2 < 0)
                            z2 -= 1;

                        // only place edges, not the insides. just a slight performance boost.
//                        if (x == (int) aabb.minX || x == (int) aabb.maxX
//                                || y == (int) aabb.minY || y == (int) aabb.maxY
//                                || z == (int) aabb.minZ || z == (int) aabb.maxZ)
                        FightOrFlightMod.debug("{}: blocksToPlace size: {}, layer size: {}, at x:{}/{} y:{}/{} z:{}/{}",
                                uid, blocksToPlace.size(), layer.size(),
                                x/ (int) aabb.maxX,
                                y/ (int) aabb.maxY,
                                z/ (int) aabb.maxZ);
                        layer.add(new BlockPos(x2, y, z2));
                    }
                }
                blocksToPlace.add(layer);
                if(blocksToPlace.size() > 50) {
                    break;
                }
            }

            placeBlockLayer();
        }

        @Override
        protected void end() {
            displayEntities.forEach(it -> {
                it.remove(Entity.RemovalReason.DISCARDED);
                EntityUtils.entitiesToNotSave.remove(it.getUUID());

                spawnParticleAt(particleMain, it.position(), (int) (5 * Math.max(1, power)), 1, 1, 1, 1);
            });
            displayEntities.clear();
            blocksToPlace.clear();
        }

        @Override
        protected void tick() {
            pokemonEntity.getLookControl().setLookAt(target);

            if (!blocksToPlace.isEmpty() && hasPassedSectionsOf(blocksToPlace.size())) {
                placeBlockLayer();
                target.level().playSeededSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.DEEPSLATE_BRICKS_FALL, SoundSource.HOSTILE,
                        0.5f, 1.0f, random.nextLong());
            }

            if (hasPassedSectionsOf(maxAttacks)) {
                calculateDamage(1.0 / maxAttacks, isSpecial, getElementalType(), pokemonEntity, target);
                target.setDeltaMovement(0, -0.1, 0);
            }

        }

        private void placeBlockLayer() {
            List<BlockPos> layer = blocksToPlace.removeFirst();
            for (BlockPos blockPos : layer) {
                Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, target.level());
                display.setPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                try {
                    ReflectionUtils.displayBlockMethod_setBlockState(display, Blocks.COBBLESTONE.defaultBlockState());
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                target.level().addFreshEntity(display);
                displayEntities.add(display);
                EntityUtils.entitiesToNotSave.add(display.getUUID()); // guarantees that it wont wrongly save
            }
        }
    }

}


















