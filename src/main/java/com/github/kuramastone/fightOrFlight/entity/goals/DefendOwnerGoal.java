package com.github.kuramastone.fightOrFlight.entity.goals;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.scores.Team;

public class DefendOwnerGoal extends TargetGoal {
    private final PokemonEntity pokemonEntity;
    private LivingEntity ownerLastHurt;
    private int timestamp;

    private WrappedPokemon wrappedPokemon;
    private int unseenTicks;

    public DefendOwnerGoal(PokemonEntity pokemonEntity) {
        super(pokemonEntity, false);
        this.pokemonEntity = pokemonEntity;
        this.wrappedPokemon = FightOrFlightMod.instance.getAPI().getWrappedPokemon(pokemonEntity);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity livingEntity = pokemonEntity.getOwner().getLastHurtByMob();
        if (livingEntity == null) {
            livingEntity = this.targetMob;
        }

        if (livingEntity == null) {
            return false;
        } else if (!this.mob.canAttack(livingEntity)) {
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

    @Override
    public boolean canUse() {
        if(pokemonEntity.getOwner() == null)
            return false;

        LivingEntity livingEntity = this.pokemonEntity.getOwner();
        if (livingEntity == null) {
            return false;
        } else {
            this.ownerLastHurt = livingEntity.getLastHurtByMob();
            int i = livingEntity.getLastHurtByMobTimestamp();
            return i != this.timestamp
                    && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT)
                    && this.pokemonEntity.wantsToAttack(this.ownerLastHurt, livingEntity);
        }
    }

    @Override
    public void start() {
        wrappedPokemon.setTarget(this.ownerLastHurt);
        LivingEntity livingEntity = this.pokemonEntity.getOwner();
        if (livingEntity != null) {
            this.timestamp = livingEntity.getLastHurtMobTimestamp();
        }

        super.start();
    }

}
