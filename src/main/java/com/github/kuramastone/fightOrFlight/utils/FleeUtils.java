package com.github.kuramastone.fightOrFlight.utils;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.world.phys.AABB;

public class FleeUtils {


    public static int getAbilityFleeScore(Pokemon pokemon) {
        AbilityTemplate ability = pokemon.getAbility().getTemplate();

        if (ability == Abilities.INSTANCE.get("intimidate")) {
            return -1;
        }
        if (ability == Abilities.INSTANCE.get("defiant")) {
            return -1;
        }
        if (ability == Abilities.INSTANCE.get("run_away")) {
            return 2;
        }

        return 0;
    }

    public static int getFleeingNatureScore(Pokemon pokemon) {
        Nature nature = pokemon.getNature();
        int sum = 0;

        sum += nature.getIncreasedStat() == Stats.SPEED ? 1 : 0;
        sum += nature.getDecreasedStat() == Stats.ATTACK ? 1 : 0;
        sum += nature.getDecreasedStat() == Stats.SPECIAL_ATTACK ? 1 : 0;

        return sum;
    }

    public static double getVolume(AABB bb) {
        return bb.getXsize() * bb.getYsize() * bb.getZsize();
    }

    public static int getBST(Pokemon pokemon) {
        return pokemon.getSpecies().getBaseStats().values().stream().mapToInt(it->it).sum();
    }
}
