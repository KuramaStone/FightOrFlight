package com.github.kuramastone.fightOrFlight.entity.goals;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.AttackState;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import com.github.kuramastone.fightOrFlight.utils.ReflectionUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

public class MeleePokeAttackGoal extends net.minecraft.world.entity.ai.goal.MeleeAttackGoal {

    private final FOFApi api;
    private final PokemonEntity pokemonEntity;
    private final WrappedPokemon wrappedPokemon;
    private long lastCanUseCheck;
    private Field pathField = ReflectionUtils.getMeleeAttackGoalPathField();

    public MeleePokeAttackGoal(FOFApi api, PokemonEntity pokemonEntity) {
        super(pokemonEntity, calculateMovementSpeed(pokemonEntity), true);
        this.api = api;
        this.pokemonEntity = pokemonEntity;
        this.wrappedPokemon = api.getWrappedPokemon(pokemonEntity);
    }

    private static double calculateMovementSpeed(PokemonEntity pokemonEntity) {
        return Math.max(1.0, (double) pokemonEntity.getPokemon().getSpeed() / 150);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity livingEntity) {
        try {
            if (this.canPerformAttack(livingEntity)) {
                this.resetAttackCooldown();
                this.mob.swing(InteractionHand.MAIN_HAND);

                PokeAttack attack = api.getAttackManager().getAttack(wrappedPokemon, it -> !it.isRanged());
                if (attack != null) {
                    attack.perform(pokemonEntity, livingEntity);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean canPerformAttack(LivingEntity livingEntity) {
        boolean isTime = this.isTimeToAttack();
        boolean inRange = isWithinMeleeAttackRange(mob, livingEntity);
        boolean hasLOS = this.mob.getSensing().hasLineOfSight(livingEntity);
        return isTime && inRange && hasLOS;
    }

    /**
     * Sometimes entity get stuck slightly out of range to hit but close enough to not move anymore. This expands the box if they stop moving
     */
    private boolean isWithinMeleeAttackRange(PathfinderMob mob, LivingEntity livingEntity) {
        if (new Vec3(mob.getDeltaMovement().x, 0, mob.getDeltaMovement().z).length() <= 0.00001)
            return mob.getBoundingBox().inflate(1.5, 1.5, 1.5).intersects(livingEntity.getBoundingBox());
        else
            return mob.isWithinMeleeAttackRange(livingEntity);
    }

    @Override
    public void start() {
        super.start();
        pokemonEntity.setTarget(wrappedPokemon.getTarget());
    }

    @Override
    public void stop() {
        super.stop();
        wrappedPokemon.setAttackState(AttackState.NONE);
    }

    private boolean isTargetInRange() {
        if (wrappedPokemon.getTarget() == null)
            return false;
        return wrappedPokemon.getTarget().distanceTo(pokemonEntity) < 24.0f;
    }

    @Override
    public boolean canContinueToUse() {
        boolean notNull = wrappedPokemon.getTarget() != null;
        boolean inRange = notNull && isTargetInRange();
        boolean inMelee = notNull && wrappedPokemon.getAttackState() == AttackState.MELEE;
        boolean notDead = !mob.isDeadOrDying();
        boolean superCheck = super.canContinueToUse();
        return superCheck
                && inRange
                && inMelee
                && notDead;
    }

    @Override
    public boolean canUse() {
        if(wrappedPokemon.getAttackState() != AttackState.MELEE)
            return false;

        long l = this.mob.level().getGameTime();
        if (l - this.lastCanUseCheck < 20L) {
            return false;
        } else {
            this.lastCanUseCheck = l;
            LivingEntity livingEntity = this.wrappedPokemon.getTarget();
            if (livingEntity == null) {
                return false;
            } else if (!livingEntity.isAlive()) {
                return false;
            } else {
                Path path2 = this.mob.getNavigation().createPath(livingEntity, 0);
                try {
                    pathField.set(this, path2);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                return path2 != null ? true : this.mob.isWithinMeleeAttackRange(livingEntity);
            }
        }
    }
}
