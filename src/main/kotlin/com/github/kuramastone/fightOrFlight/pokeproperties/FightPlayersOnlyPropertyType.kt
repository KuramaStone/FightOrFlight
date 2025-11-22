package com.github.kuramastone.fightOrFlight.pokeproperties

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.properties.FlagProperty

class FightPlayersOnlyPropertyType : CustomPokemonPropertyType<FlagProperty> {
    override val keys = setOf(KEY)
    override val needsKey = true

    override fun examples() = listOf("true", "false")

    override fun fromString(value: String?): FlagProperty? {
        return FlagProperty(KEY, false)
    }

    companion object {
        private val KEY = "fof-players-only"

        @JvmStatic
        fun matches(pokemon: Pokemon): Boolean {
            return FlagProperty(KEY, false).matches(pokemon)
        }
    }
}
