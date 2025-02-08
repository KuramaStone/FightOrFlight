package com.github.kuramastone.fightOrFlight;

import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.attacks.types.*;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;

public class AttackManager {

    private final Map<ElementalType, PokeAttack> rangedAttacks;
    private final Map<ElementalType, PokeAttack> physicalAttacks;

    private SplittableRandom random;

    public AttackManager() {
        rangedAttacks = new HashMap<>();
        physicalAttacks = new HashMap<>();
        random = new SplittableRandom();

        registerRangedAttack(FireAttack.class);
        registerRangedAttack(IceAttack.class);
        registerDynamicAttack(PoisonAttack.class);
        registerRangedAttack(PsychicAttack.class);
        registerRangedAttack(FairyAttack.class);
        registerPhysicalAttack(FightingAttack.class);
        registerDynamicAttack(SteelAttack.class);
        registerDynamicAttack(GroundAttack.class);
        registerDynamicAttack(RockAttack.class);
        registerRangedAttack(GhostAttack.class);
        registerRangedAttack(DarkAttack.class);
        registerDynamicAttack(ElectricAttack.class);
        registerDynamicAttack(BugAttack.class);
        registerDynamicAttack(GrassAttack.class);
        registerRangedAttack(WaterAttack.class);
        registerDynamicAttack(FlyingAttack.class);
        registerDynamicAttack(DragonAttack.class);
        registerDynamicAttack(NormalAttack.class);
    }

    /**
     * Registers an attack as both melee and physical
     */
    private void registerDynamicAttack(Class<? extends PokeAttack> clazz) {
        registerPhysicalAttack(clazz);
        registerRangedAttack(clazz);
    }

    private void registerRangedAttack(Class<? extends PokeAttack> clazz) {
        FOFApi api = FightOrFlightMod.instance.getAPI();
        PokeAttack pokeAttack = null;
        try {
            pokeAttack = clazz.getDeclaredConstructor(FOFApi.class, boolean.class).newInstance(api, true);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        rangedAttacks.put(pokeAttack.getElementalType(), pokeAttack);
    }

    private void registerPhysicalAttack(Class<? extends PokeAttack> clazz) {
        FOFApi api = FightOrFlightMod.instance.getAPI();
        PokeAttack pokeAttack = null;
        try {
            pokeAttack = clazz.getDeclaredConstructor(FOFApi.class, boolean.class).newInstance(api, false);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        physicalAttacks.put(pokeAttack.getElementalType(), pokeAttack);
    }

    public @Nullable PokeAttack getAttack(WrappedPokemon wrappedPokemon) {
        return getAttack(wrappedPokemon, null);
    }

    public @Nullable PokeAttack getAttack(WrappedPokemon wrappedPokemon, @Nullable Predicate<PokeAttack> filter) {
        PokeAttack special = getAttack(this.rangedAttacks, filter, wrappedPokemon);
        PokeAttack physical = getAttack(this.physicalAttacks, filter, wrappedPokemon);

        boolean isSpecial = wrappedPokemon.shouldUseSpecialAttack();
        if (isSpecial && special != null)
            return special; // it's a special user and there is a special move, so return special
        else if (!isSpecial && physical != null)
            return physical; // its a physical user and there is a physical move, so return physical
        else if (special != null)
            return special; // even though it isn't a special attacker, return the special move. It will still use their higher attack stat
        else if (physical != null)
            return physical; // even though it is a special attacker, return the physical move. it will still use their higher attack stat
        else
            return null; // return null if nothing could possibly work
    }

    public @Nullable PokeAttack getAttack(Map<ElementalType, PokeAttack> map, @Nullable Predicate<PokeAttack> filter, WrappedPokemon wrappedPokemon) {
        // get only types with an attack
        List<ElementalType> types = new ArrayList<>();
        for (ElementalType elementalType : new ElementalType[]{wrappedPokemon.getPokemon().getPrimaryType(), wrappedPokemon.getPokemon().getSecondaryType()}) {
            if (map.containsKey(elementalType))
                types.add(elementalType);
        }

        // check elemental types
        if (types.isEmpty())
            return null;

        Map<ElementalType, PokeAttack> filteredMap = new HashMap<>();
        for (Map.Entry<ElementalType, PokeAttack> pair : map.entrySet()) {
            if (filter == null || filter.test(pair.getValue()))
                filteredMap.put(pair.getKey(), pair.getValue());
        }

        if (filteredMap.isEmpty())
            return null;

        //return a random valid type
        return filteredMap.get(types.get(random.nextInt(types.size())));
    }

}
