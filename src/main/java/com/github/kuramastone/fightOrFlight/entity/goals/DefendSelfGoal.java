package com.github.kuramastone.fightOrFlight.entity.goals;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class DefendSelfGoal extends TargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();
    private static final int ALERT_RANGE_Y = 10;
    private boolean alertSameType;
    private int timestamp;
    private final Class<?>[] toIgnoreDamage;
    @Nullable
    private Class<?>[] toIgnoreAlert;

    private WrappedPokemon wrappedPokemon;
    private int unseenTicks;

    public DefendSelfGoal(PokemonEntity pathfinderMob, Class<?>... classs) {
        super(pathfinderMob, true);
        this.toIgnoreDamage = classs;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));

        this.wrappedPokemon = FightOrFlightMod.instance.getAPI().getWrappedPokemon(pathfinderMob);
    }

    @Override
    public void stop() {
        this.mob.setTarget(null);
        this.targetMob = null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity livingEntity = this.mob.getTarget();
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

                    this.mob.setTarget(livingEntity);
                    this.wrappedPokemon.setTarget(livingEntity);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean canUse() {
        if(FightOrFlightMod.instance.getAPI().isDisabledInWorld(this.mob.level())) {
            return false;
        }
        int i = this.mob.getLastHurtByMobTimestamp();
        LivingEntity livingEntity = this.mob.getLastHurtByMob();
        if (i != this.timestamp && livingEntity != null) {
            if (livingEntity.getType() == EntityType.PLAYER && this.mob.level().getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                return false;
            } else {
                for (Class<?> class_ : this.toIgnoreDamage) {
                    if (class_.isAssignableFrom(livingEntity.getClass())) {
                        return false;
                    }
                }

                return this.canAttack(livingEntity, HURT_BY_TARGETING);
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.mob.setTarget(this.mob.getLastHurtByMob());
        this.targetMob = this.mob.getTarget();
        this.wrappedPokemon.setTarget(this.targetMob);
        this.timestamp = this.mob.getLastHurtByMobTimestamp();
        this.unseenMemoryTicks = 300;
        if (this.alertSameType) {
            this.alertOthers();
        }

        super.start();
    }

    protected void alertOthers() {
        double d = this.getFollowDistance();
        AABB aABB = AABB.unitCubeFromLowerCorner(this.mob.position()).inflate(d, 10.0, d);
        List<? extends Mob> list = this.mob.level().getEntitiesOfClass(this.mob.getClass(), aABB, EntitySelector.NO_SPECTATORS);
        Iterator var5 = list.iterator();

        while (true) {
            Mob mob;
            while (true) {
                if (!var5.hasNext()) {
                    return;
                }

                mob = (Mob)var5.next();
                if (this.mob != mob
                        && mob.getTarget() == null
                        && (!(this.mob instanceof TamableAnimal) || ((TamableAnimal)this.mob).getOwner() == ((TamableAnimal)mob).getOwner())
                        && !mob.isAlliedTo(this.mob.getLastHurtByMob())) {
                    if (this.toIgnoreAlert == null) {
                        break;
                    }

                    boolean bl = false;

                    for (Class<?> class_ : this.toIgnoreAlert) {
                        if (mob.getClass() == class_) {
                            bl = true;
                            break;
                        }
                    }

                    if (!bl) {
                        break;
                    }
                }
            }

            this.alertOther(mob, this.mob.getLastHurtByMob());
        }
    }

    protected void alertOther(Mob mob, LivingEntity livingEntity) {
        mob.setTarget(livingEntity);
    }

    @Override
    protected boolean canAttack(@Nullable LivingEntity livingEntity, TargetingConditions targetingConditions) {
        if (livingEntity instanceof PokemonEntity pokemonEntity) {
            if(FightOrFlightMod.instance.getAPI().isPokemonProtected(pokemonEntity)) {
                return false;
            }
        }
        // dont defend self from owner
        if(this.wrappedPokemon.getPokemonEntity().getOwnerUUID() != null) {
            if(this.wrappedPokemon.getPokemonEntity().getOwnerUUID().equals(livingEntity.getUUID())) {
                return false;
            }
        }
        return super.canAttack(livingEntity, targetingConditions);
    }
}