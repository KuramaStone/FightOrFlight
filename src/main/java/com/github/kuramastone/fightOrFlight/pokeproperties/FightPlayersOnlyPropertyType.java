package com.github.kuramastone.fightOrFlight.pokeproperties;

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

// Check CustomPokemonPropertyType to see more about these methods.
public class FightPlayersOnlyPropertyType implements CustomPokemonPropertyType<FightPlayersOnlyPropertyType.FightPlayersOnlyProperty> {

    @Override
    public @NotNull Iterable<String> getKeys() {
        return List.of("fof-players-only");
    }

    @Override
    public boolean getNeedsKey() {
        return false;
    }

    @Override
    public @Nullable FightPlayersOnlyPropertyType.FightPlayersOnlyProperty fromString(@Nullable String args) {
        return new FightPlayersOnlyProperty();
    }

    @Override
    public @NotNull Collection<String> examples() {
        return List.of();
    }

    public static class FightPlayersOnlyProperty implements CustomPokemonProperty {

        @Override
        public @NotNull String asString() {
            return "fof-players-only";
        }

        @Override
        public void apply(@NotNull Pokemon pokemon) {
            pokemon.getCustomProperties().add(this);
        }

        @Override
        public void apply(@NotNull PokemonEntity pokemonEntity) {
            apply(pokemonEntity.getPokemon());
        }

        @Override
        public boolean matches(@NotNull Pokemon pokemon) {
            return pokemon.getCustomProperties().stream().anyMatch(it -> it.asString().equals(this.asString()));
        }

        @Override
        public boolean matches(@NotNull PokemonEntity pokemonEntity) {
            return matches(pokemonEntity.getPokemon());
        }
    }

}
