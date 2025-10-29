package com.github.kuramastone.fightOrFlight.pokeproperties

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.pokemon.Pokemon

class FightPlayersOnlyPropertyType : CustomPokemonPropertyType<FightPlayersOnlyProperty> {
    override val keys = setOf("fof-players-only")
    override val needsKey = true

    override fun examples() = listOf("true", "false")

    override fun fromString(value: String?): FightPlayersOnlyProperty? {
        if(value != null && (value.equals("true", true) || value.equals("yes", true))) {
            return FightPlayersOnlyProperty()
        }
        return null
    }
}

class FightPlayersOnlyProperty() : CustomPokemonProperty {

    override fun apply(pokemon: Pokemon) {
        pokemon.customProperties.add(this)
    }

    override fun asString() = "fof-players-only"

    override fun matches(pokemon: Pokemon): Boolean {
        return pokemon.customProperties.any { it.asString() == this.asString() }
    }

}