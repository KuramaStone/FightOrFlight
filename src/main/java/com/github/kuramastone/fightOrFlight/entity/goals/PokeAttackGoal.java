package com.github.kuramastone.fightOrFlight.entity.goals;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.entity.AttackState;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.pathfinder.Path;

import java.time.Duration;
import java.util.*;

public class PokeAttackGoal extends Goal {

    private final FOFApi api;
    private final WrappedPokemon wrappedPokemon;
    private final PokemonEntity pokemonEntity;

    protected SplittableRandom random = new SplittableRandom();
    private long timeOfLastAttack;
    private long timeOfAttackStart;

    private LivingEntity targetMob;
    private boolean setTargetToNullAtEnd;
    private Path path;

    public PokeAttackGoal(FOFApi api, PokemonEntity pokemonEntity) {
        Objects.requireNonNull(pokemonEntity, "Pokemon cannot be null for goal!");
        this.api = api;
        this.pokemonEntity = pokemonEntity;
        this.wrappedPokemon = api.getWrappedPokemon(pokemonEntity);

        getFlags().addAll(List.of(Flag.MOVE, Flag.LOOK, Flag.JUMP, Flag.TARGET));
    }

    @Override
    public void start() {
        try {
            super.start();
            targetMob = wrappedPokemon.getTarget();
            if (targetMob == null)
                return;
            if (!pokemonEntity.getSensing().hasLineOfSight(targetMob))
                createPathToTarget();
            timeOfAttackStart = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (setTargetToNullAtEnd)
            wrappedPokemon.setTarget(null);
        targetMob = null;
        timeOfLastAttack = -1;
        if (path != null)
            pokemonEntity.getNavigation().stop();
        path = null;
    }

    @Override
    public void tick() {
        try {
            super.tick();

            // only move until we have line of sight
            if (pokemonEntity.getSensing().hasLineOfSight(targetMob)) {
                pokemonEntity.getNavigation().stop();
                path = null;
            }
            else {
                createPathToTarget();
            }

            if (shouldAttemptAttack()) {
                if (!pokemonEntity.getSensing().hasLineOfSight(targetMob) && (path == null || !path.canReach() || path.isDone())) {
                    createPathToTarget();
                }

                // perform special attack
                PokeAttack attack = api.getAttackManager().getAttack(wrappedPokemon);
                if (attack != null) {
                    //start ranged attack.
                    if (attack.isRanged()) {
                        pokemonEntity.getNavigation().stop();
                        attack.perform(pokemonEntity, targetMob);
                        setTargetToNullAtEnd = true;
                    }
                    // set to perform melee. Melee goal will take over
                    else {
                        wrappedPokemon.setAttackState(AttackState.MELEE);
                        setTargetToNullAtEnd = false;
                    }
                    timeOfLastAttack = System.currentTimeMillis();
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createPathToTarget() {
        path = pokemonEntity.getNavigation().createPath(targetMob, 1);
        pokemonEntity.getNavigation().moveTo(path, calculateMovementSpeed(pokemonEntity));
    }

    private static double calculateMovementSpeed(PokemonEntity pokemonEntity) {
        return Math.max(1.0, pokemonEntity.getSpeed() / 150.0);
    }

    private boolean shouldAttemptAttack() {
        if (targetMob == null || targetMob.isDeadOrDying())
            return false;

        if (System.currentTimeMillis() < timeOfLastAttack + 5000) {
            return false;
        }

        if (!pokemonEntity.getSensing().hasLineOfSight(targetMob))
            return false;

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // if target has changed, stop trying to attack.
        if(wrappedPokemon.getTarget() != targetMob) {
            setTargetToNullAtEnd = false;
            return false;
        }

        // stop trying to attack if the time of last attack was 30 seonds ago
        if(System.currentTimeMillis() > timeOfAttackStart + 30000) {
            setTargetToNullAtEnd = true;
            return false;
        }

        return !pokemonEntity.isDeadOrDying() && (targetMob != null && !targetMob.isDeadOrDying() && isTargetInRange());
    }

    private boolean isTargetInRange() {
        return targetMob.distanceTo(pokemonEntity) < 24.0f;
    }

    @Override
    public boolean canUse() {
        return wrappedPokemon.getTarget() != null && !wrappedPokemon.getTarget().isDeadOrDying();
    }
}


//Move move = getRandomMove(pokemonEntity.getPokemon().getMoveSet());
//            FightOrFlightMod.logger.info("Activating attack: {}", move.getName());
//MoLangRuntime runtime = new MoLangRuntime();
//List<Object> providers = new ArrayList<>();
//            providers.add(new UsersProvider(target));
//
//        runtime.getEnvironment().query.addFunction("move", (_a) -> move.getStruct());
//        runtime.getEnvironment().query.addFunction("instruction_id", (_a) -> new StringValue(cobblemonResource("move").toString()));
//
//        MoLangFunctions.INSTANCE.addStandardFunctions(runtime.getEnvironment().query);
//
//ActionEffectTimeline actionEffectTimeline = move.getTemplate().getActionEffect();
//            move.getTemplate().getActionEffect().run(new ActionEffectContext(
//        actionEffectTimeline, new HashSet<>(), providers,
//runtime, false, false, new ArrayList<>(),
//        pokemonEntity.level()
//            ));