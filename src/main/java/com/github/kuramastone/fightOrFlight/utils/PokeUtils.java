package com.github.kuramastone.fightOrFlight.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.tags.CobblemonItemTags;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.ai.strongBattleAI.AIUtility;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.requirements.LevelRequirement;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public class PokeUtils {

    public static double calculatePokeAttackDamage(Pokemon attacker, Pokemon defender, ElementalType moveType, double power, boolean isSpecial, boolean useTypeEffectiveness) {
        int level = attacker.getLevel();
        double attack = isSpecial ? attacker.getSpecialAttack() : attacker.getAttack();
        double defense = isSpecial ? defender.getSpecialDefence() : defender.getDefence();

        // Base damage calculation
        double baseDamage = ((((((double) (2 * level) / 5) + 2) * power * (attack / defense)) / 50) + 2);

        // Modifier calculation
        double modifier = calculateModifier(attacker, defender, moveType, useTypeEffectiveness, isSpecial);

        // Final damage
        return baseDamage * modifier;
    }

    public static boolean doAnyAspectsMatch(List<String> targetAspects, PokemonEntity pe) {
        if (targetAspects == null) {
            return true;
        }
        Set<String> peAspects = pe.getPokemon().getAspects();

        if (targetAspects.contains("shiny") && pe.getPokemon().getShiny()) {
            return true;
        }

        for (String peAspect : peAspects) {
            if (targetAspects.contains(peAspect)) {
                return true;
            }
        }

        return false;
    }

    private static double calculateModifier(Pokemon attacker, Pokemon defender, ElementalType moveType, boolean useTypeEffectiveness, boolean isSpecial) {
        double modifier = 1.0;

        // STAB
        if (attacker.getAbility().getName().equalsIgnoreCase("adaptability") ||
                attacker.getPrimaryType() == moveType || attacker.getSecondaryType() == moveType)
            modifier *= 1.5;

        // Type effectiveness
        if (useTypeEffectiveness)
            modifier *= getTypeEffectiveness(moveType, defender.getPrimaryType(), defender.getSecondaryType());

        // Random factor
        modifier *= getRandomFactor();

        // Additional factors (abilities, items, weather, etc.) can be added here.
        ItemStack heldItem = attacker.getHeldItem$common();
        if (isSpecial) {
            if (heldItem.getItem() == CobblemonItems.CHOICE_SPECS) {
                modifier *= 1.5;
            }
        }
        else {
            if (heldItem.getItem() == CobblemonItems.CHOICE_BAND) {
                modifier *= 1.5;
            }
        }

        if(heldItem.getItem() == CobblemonItems.LIFE_ORB) {
            modifier *= 1.3;
        }

        return modifier;
    }

    private static double getTypeEffectiveness(ElementalType type, ElementalType primaryType, ElementalType secondaryType) {

        double modifier = AIUtility.INSTANCE.getDamageMultiplier(type, primaryType);
        if (secondaryType != null) {
            modifier *= AIUtility.INSTANCE.getDamageMultiplier(type, secondaryType);
        }

        return modifier;
    }

    // Placeholder methods for type effectiveness, critical hit chance, and random factor

    private static double getRandomFactor() {
        // Return a random value between 0.85 and 1.0
        return 0.85 + (Math.random() * 0.15);
    }

    public static int getBaseStatTotal(Pokemon pokemon) {
        int sum = 0;

        for (Stat stat : new Stat[]{Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED}) {
            sum += pokemon.getStat(stat);
        }

        return sum;
    }

    public static int calculateExperience(Pokemon victorPokemon, Pokemon opponentPokemon, double participationMultiplier) {
        double baseExp = opponentPokemon.getForm().getBaseExperienceYield();
        int opponentLevel = opponentPokemon.getLevel();
        double term1 = (baseExp * opponentLevel) / 5.0;

        // Handling the division behavior of participationMultiplier
        double term2 = 1 * participationMultiplier;

        int victorLevel = victorPokemon.getLevel();
        double term3 = Math.pow(((2.0 * opponentLevel) + 10) / (opponentLevel + victorLevel + 10), 2.5);

        // Checking if the Pokémon is the original trainer's
        double nonOtBonus = 1.0;
        if (victorPokemon.getOriginalTrainer() != null && victorPokemon.getOwnerUUID() != null) {
            if (!victorPokemon.getOriginalTrainer().equals(victorPokemon.getOwnerUUID().toString())) {
                nonOtBonus = 1.5;
            }
        }

        // Checking if the Pokémon is holding a Lucky Egg
        double luckyEggMultiplier = (victorPokemon.heldItem().is(CobblemonItemTags.LUCKY_EGG)) ?
                Cobblemon.config.getLuckyEggMultiplier() : 1.0;

        // Evolution multiplier check
        boolean canEvolve = victorPokemon.getEvolutionProxy().server().stream().anyMatch(evolution ->
                evolution.getRequirements().stream().anyMatch(req -> req instanceof LevelRequirement) &&
                        evolution.getRequirements().stream().allMatch(req -> req.check(victorPokemon))
        );
        double evolutionMultiplier = canEvolve ? 1.2 : 1.0;

        // Affection multiplier
        double affectionMultiplier = (victorPokemon.getFriendship() >= 220) ? 1.2 : 1.0;

        // Global experience multiplier
        double gimmickBoost = Cobblemon.config.getExperienceMultiplier();

        // Final calculation
        double term4 = term1 * term2 * term3 + 1;
        return (int) Math.round(term4 * nonOtBonus * luckyEggMultiplier * evolutionMultiplier * affectionMultiplier * gimmickBoost);
    }

    public static boolean doesAnySpeciesMatch(List<String> speciesList, Species species) {
        return speciesList.stream().anyMatch(it -> it.equalsIgnoreCase(species.getName()));
    }
}
