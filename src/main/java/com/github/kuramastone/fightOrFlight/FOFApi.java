package com.github.kuramastone.fightOrFlight;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import com.github.kuramastone.fightOrFlight.utils.ConfigOptions;
import com.github.kuramastone.fightOrFlight.utils.PokeUtils;
import com.github.kuramastone.fightOrFlight.utils.TickScheduler;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FOFApi {

    private Map<PokemonEntity, WrappedPokemon> wrappedPokemonEntityList;

    private AttackManager attackManager;
    private WandManager wandManager;

    private ConfigOptions configOptions;

    public void init() {
        wrappedPokemonEntityList = Collections.synchronizedMap(new HashMap<>());
        attackManager = new AttackManager();
        wandManager = new WandManager();

        configOptions = new ConfigOptions();
        TickScheduler.scheduleRepeating(this::checkWrappedList, 0L, 200L);
    }

    private void checkWrappedList() {
        for (WrappedPokemon wp : new ArrayList<>(wrappedPokemonEntityList.values())) {
            if(wp.getPokemonEntity().isRemoved()) {
                removeWrappedPokemon(wp.getPokemonEntity());
            }
        }
    }

    public WrappedPokemon getWrappedPokemon(PokemonEntity entity) {
        if(wrappedPokemonEntityList.containsKey(entity)) {
            return wrappedPokemonEntityList.get(entity);
        }
        return newWrappedPokemon(entity);
    }

    public void removeWrappedPokemon(PokemonEntity entity) {
        wrappedPokemonEntityList.remove(entity);
    }

    public AttackManager getAttackManager() {
        return attackManager;
    }

    public WandManager getWandManager() {
        return wandManager;
    }

    public ConfigOptions getConfigOptions() {
        return configOptions;
    }

    public WrappedPokemon newWrappedPokemon(PokemonEntity pokemonEntity) {
        WrappedPokemon wp = new WrappedPokemon(pokemonEntity);
        this.wrappedPokemonEntityList.put(pokemonEntity, wp);
        return wp;
    }

    /**
     * @return True if this pokemon cannot be targetted
     */
    public boolean isPokemonProtected(PokemonEntity pokemonEntity) {
        return !pokemonEntity.getPokemon().getAspects().contains("forced-targetting") && pokemonEntity.getOwnerUUID() == null
                && PokeUtils.doAnyAspectsMatch(configOptions.aggressionDisabledAspects, pokemonEntity);
    }

    public boolean isDisabledInWorld(Level level) {
        String world_name = level.dimension().location().toString();
        return configOptions.disabledWorlds.contains(world_name);
    }

}
