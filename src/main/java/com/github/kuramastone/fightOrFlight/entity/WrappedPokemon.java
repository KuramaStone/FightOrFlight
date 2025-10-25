package com.github.kuramastone.fightOrFlight.entity;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.event.FOFEvents;
import com.github.kuramastone.fightOrFlight.event.FightFleeChanceCalculation;
import com.github.kuramastone.fightOrFlight.pokeproperties.AggressionBiasProperty;
import com.github.kuramastone.fightOrFlight.pokeproperties.FightPlayersOnlyPropertyType;
import com.github.kuramastone.fightOrFlight.utils.FleeUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.SplittableRandom;

public class WrappedPokemon {

    private PokemonEntity pokemonEntity;
    private AttackState attackState = AttackState.NONE;
    private SplittableRandom random;

    private LivingEntity target;
    private boolean neverFleeTarget = false;

    // remember last decision about fleeing from this source
    private DamageSource cachedDamageSource;
    private boolean willFleeFromCachedSource;

    public WrappedPokemon(PokemonEntity pokemonEntity) {
        this.pokemonEntity = pokemonEntity;
        random = new SplittableRandom();
    }

    /**
     * Consider different values of this pokemon and decide if they would flee from this target
     */
    public boolean shouldFlee(DamageSource lastDamageSource, LivingEntity target) {
        if(target == this.target && neverFleeTarget)
            return false;
        if (lastDamageSource == cachedDamageSource)
            return willFleeFromCachedSource;
        if (target == null)
            return false;

        // dont flee when defending the owner
        if (pokemonEntity.getOwner() != null) {
            if (pokemonEntity.getOwner().getLastHurtByMob() == target)
                return false;
        }

        // a positive score is more likely to flee

        // does this nature encourage fleeing?
        int natureScore = FleeUtils.getFleeingNatureScore(pokemonEntity.getPokemon());
        // does this ability affect fleeing?
        int abilityScore = FleeUtils.getAbilityFleeScore(pokemonEntity.getPokemon());
        // is self bigger than target?
        double relativeSize = 0.2 * (FleeUtils.getVolume(target.getBoundingBox()) - FleeUtils.getVolume(pokemonEntity.getBoundingBox()));
        // is a higher level?
        double levelScore;
        // is stronger otherwise
        double baseStatScore;

        Pokemon targetPokemon = (target instanceof PokemonEntity) ?
                ((PokemonEntity) target).getPokemon()
                : FightOrFlightMod.instance.getAPI().getConfigOptions().getPokemonEquivalent(target.getType());
        levelScore = 0.1 * (targetPokemon.getLevel() - pokemonEntity.getPokemon().getLevel());
        baseStatScore = Math.tanh((FleeUtils.getBST(targetPokemon) - FleeUtils.getBST(pokemonEntity.getPokemon()))) * 2;

        // add random variation
        double rndVariation = 5;

        double fleeScore = FightOrFlightMod.instance.getAPI().getConfigOptions().fleeBiasValue + -2 + natureScore + abilityScore + relativeSize + levelScore + baseStatScore;
        fleeScore += getAggressionBias();

        double minValue = fleeScore - rndVariation;
        double maxValue = fleeScore + rndVariation;
        double fleeLikelihood;
        if (maxValue <= 0)
            fleeLikelihood = 0.0; // guaranteed to not flee if max value is still below 0
        else if (minValue >= 0)
            fleeLikelihood = 1.0; // guaranteed to flee if min is still above 0
        else
            fleeLikelihood = (maxValue - Math.max(0, minValue)) / (maxValue - minValue);

        FightFleeChanceCalculation event = new FightFleeChanceCalculation(this, target, fleeLikelihood);
        FOFEvents.FLIGHT_FLEE_CHANCE_CALCULATION.emit(event);
        fleeLikelihood = event.getFleeLikelihood();

        boolean willFlee = random.nextDouble() < fleeLikelihood;
//        Utils.broadcast("==============================");
//        Utils.broadcast("nature=%s, ability=%s, size=%.2f, level=%s, bst=%.2f".formatted(natureScore, abilityScore, relativeSize, levelScore, baseStatScore));
//        Utils.broadcast("willFlee=%s, chanceToFlee=%s%%, range=[%.1f <%.1f> %.1f]".formatted(willFlee, (int) (fleeLikelihood * 100), minValue, fleeScore, maxValue));



        cachedDamageSource = lastDamageSource;
        willFleeFromCachedSource = willFlee;

        return willFlee;
    }

    // returns the aggression bias of this pokemon if they have an AggressionBiasProperty
    private double getAggressionBias() {
        @Nullable AggressionBiasProperty aggressionBiasProperty = (AggressionBiasProperty) pokemonEntity.getPokemon().getCustomProperties()
                .stream()
                .filter(it -> it instanceof AggressionBiasProperty)
                .findFirst()
                .orElse(null);

        if(aggressionBiasProperty != null) {
            return aggressionBiasProperty.getBias();
        }

        return 0.0;
    }

    /**
     * Use special if the special stat is higher. Random if the stats are equal
     */
    public boolean shouldUseSpecialAttack() {
        int spa = pokemonEntity.getPokemon().getSpecialAttack();
        int att = pokemonEntity.getPokemon().getAttack();

        if (spa > att)
            return true;
        else if (att > spa)
            return false;
        else
            return random.nextBoolean();
    }

    public PokemonEntity getPokemonEntity() {
        return pokemonEntity;
    }

    public Pokemon getPokemon() {
        return pokemonEntity.getPokemon();
    }

    public void setAttackState(AttackState attackState) {
        this.attackState = attackState;
    }

    public AttackState getAttackState() {
        return attackState;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        setTarget(target, false);
    }

    public void setTarget(LivingEntity target, boolean neverFlee) {
        this.target = target;
        this.neverFleeTarget = neverFlee;
    }

    public Pair<Stat, Integer> getHigherAttackStat() {
        int spa = pokemonEntity.getPokemon().getSpecialAttack();
        int att = pokemonEntity.getPokemon().getAttack();

        if (spa > att)
            return Pair.of(Stats.SPECIAL_ATTACK, spa);
        else if (att > spa)
            return Pair.of(Stats.ATTACK, att);
        else
            return random.nextBoolean() ? Pair.of(Stats.SPECIAL_ATTACK, spa) : Pair.of(Stats.ATTACK, att);
    }

    public boolean isAllowedToAttackTarget() {
        return isAllowedToAttackTarget(getTarget());
    }

    public boolean isAllowedToAttackTarget(LivingEntity target) {
        if(pokemonEntity.isBusy())
            return false;
        if(target instanceof PokemonEntity pokeTarget) {
            if(pokeTarget.isBusy()) {
                return false;
            }
        }

        if(pokemonEntity.getOwner() != null && target != null) {
            if(pokemonEntity.getOwner() instanceof Player playerOwner) {
                if (!PokeAttack.canAttack(playerOwner, target)) {
                    return false;
                }
            }
        }

        // if they have this tag, then ONLY target players.
        if(new FightPlayersOnlyPropertyType.FightPlayersOnlyProperty().matches(pokemonEntity.getPokemon())) {
            if(!(target instanceof ServerPlayer)) {
                return false;
            }
        }

        return true;
    }
}
