package com.github.kuramastone.fightOrFlight.entity.goals;

import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import com.github.kuramastone.fightOrFlight.utils.PokeUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Causes Pokemon to occasionally attack a nearby pokemon
 */
public class ExtraAggressionGoal extends TargetGoal {
    private final PokemonEntity pokemonEntity;

    private WrappedPokemon wrappedPokemon;
    private int unseenTicks;
    private int aggressionCounter = 0;
    private Random random;
    private boolean isDisabled = false;

    public ExtraAggressionGoal(PokemonEntity pokemonEntity) {
        super(pokemonEntity, false);
        this.pokemonEntity = pokemonEntity;
        this.wrappedPokemon = FightOrFlightMod.instance.getAPI().getWrappedPokemon(pokemonEntity);
        random = new Random();
        resetAggression();
        // apply a random factor to the aggression on its initial creation
        aggressionCounter = (int) ((0.5 + random.nextDouble() * 0.5) * aggressionCounter);

        FOFApi api = FightOrFlightMod.instance.getAPI();
        // dont allow disabled species
        if (PokeUtils.doesAnySpeciesMatch(api.getConfigOptions().aggressionDisabledSpecies, pokemonEntity.getPokemon().getSpecies())) {
            isDisabled = true;
        }
        // dont allow disabled aspects
        if (PokeUtils.doAnyAspectsMatch(api.getConfigOptions().aggressionDisabledAspects, pokemonEntity)) {
            isDisabled = true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (pokemonEntity.getBattleId() != null) {
            return false;
        }

        if (pokemonEntity.isBusy()) {
            return false;
        }

        LivingEntity livingEntity = this.targetMob;

        // stop if target has changed.
        if (livingEntity != wrappedPokemon.getTarget()) {
            return false;
        }

        if (livingEntity == null) {
            return false;
        } else if (!this.mob.canAttack(livingEntity)) {
            return false;
        } else if (livingEntity.isDeadOrDying()) {
            return false;
        } else {
            Team team = this.mob.getTeam();
            Team team2 = livingEntity.getTeam();
            if (team != null && team2 == team) {
                return false;
            } else {
                double d = this.getFollowDistance();
                if (this.mob.distanceToSqr(livingEntity) > d * d) {
                    return false;
                } else {
                    if (this.mustSee) {
                        if (this.mob.getSensing().hasLineOfSight(livingEntity)) {
                            this.unseenTicks = 0;
                        } else if (++this.unseenTicks > reducedTickDelay(this.unseenMemoryTicks)) {
                            return false;
                        }
                    }

                    this.wrappedPokemon.setTarget(livingEntity);

                    return true;
                }
            }
        }
    }

    private @Nullable LivingEntity getMobToTarget() {
        List<LivingEntity> nearby = pokemonEntity.level().getEntitiesOfClass(LivingEntity.class, pokemonEntity.getBoundingBox().inflate(16));
        nearby.removeIf(it -> it.getUUID() == pokemonEntity.getOwnerUUID()); // remove owner
        nearby.removeIf(it -> it == pokemonEntity); // remove self
        nearby.removeIf(it -> {  // dont target creative/spectators
            if (it instanceof ServerPlayer player)
                return !(player.gameMode.isSurvival() || player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE);
            return false;
        });
        nearby.removeIf(it -> !wrappedPokemon.isAllowedToAttackTarget(it));

        nearby.removeIf(it -> {
            if (it instanceof PokemonEntity queryPoke)
                // remove pokemon with similar owners
                if (queryPoke.getOwnerUUID() != null && queryPoke.getOwnerUUID().equals(this.pokemonEntity.getOwnerUUID()))
                    return true;
                    // remove pokemon that are battling or being caught
                else if (queryPoke.isBusy()) {
                    return true;
                }
            return false;
        });

        nearby.removeIf(it -> !wrappedPokemon.isAllowedToAttackTarget(it));


        LivingEntity nearest = null;
        double nearestDist = 0;
        for (LivingEntity livingEntity : nearby) {
            double d = livingEntity.distanceToSqr(pokemonEntity);
            if (nearest == null || d < nearestDist) {
                nearest = livingEntity;
                nearestDist = d;
            }
        }

        return nearest;
    }

    @Override
    public boolean canUse() {
        FOFApi api = FightOrFlightMod.instance.getAPI();
        // owned pokemon cant be extra aggressive if this is toggled
        if (api.getConfigOptions().ownedPokemonAggressionDisabled) {
            if (pokemonEntity.getOwner() != null) {
                return false;
            }
            if(pokemonEntity.getTethering() != null) {
                return false;
            }
        }

        if(isDisabled) {
            return false;
        }

        // disable if default time is set to ignore.
        if (getDefaultAggression() < 0) {
            return false;
        }

        // if pokemon already has target, dont use
        if (wrappedPokemon.getTarget() != null) {
            return false;
        }

        // if pokemon is in a battle, dont use
        if (pokemonEntity.getBattleId() != null) {
            return false;
        }

        // dont use if busy
        if (pokemonEntity.isBusy()) {
            return false;
        }

        // only trigger periodically
        if (aggressionCounter-- <= 0) {
            resetAggression();
            LivingEntity target = getMobToTarget();
            if (target != null) {
                if (this.canAttack(target, TargetingConditions.DEFAULT)) {
                    this.targetMob = target;
                    wrappedPokemon.setTarget(target);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean canAttack(@Nullable LivingEntity livingEntity, TargetingConditions targetingConditions) {
        if (livingEntity == null)
            return false;
        else if (!pokemonEntity.getSensing().hasLineOfSight(livingEntity))
            return false;
        else if (livingEntity instanceof PokemonEntity pokemonEntity) {
            if(FightOrFlightMod.instance.getAPI().isPokemonProtected(pokemonEntity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
        super.start();
        // spawn angry particles
        if (targetMob != null) {
            ((ServerLevel) pokemonEntity.level()).sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    pokemonEntity.getEyePosition().x, pokemonEntity.getEyePosition().y, pokemonEntity.getEyePosition().z,
                    10,
                    1, 0.25, 1,
                    0);
        }
    }

    @Override
    public void stop() {
        super.stop();
        resetAggression();
    }

    private void resetAggression() {
        aggressionCounter = (int) (getDefaultAggression() * 100.0 / this.wrappedPokemon.getHigherAttackStat().getRight());
    }

    private int getDefaultAggression() {
        // 3 minutes
        return FightOrFlightMod.instance.getAPI().getConfigOptions().baseAggressionTimer;
    }

}
