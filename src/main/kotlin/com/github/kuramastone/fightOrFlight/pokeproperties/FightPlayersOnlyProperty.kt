package com.github.kuramastone.fightOrFlight.pokeproperties

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.properties.FlagProperty

class FightPlayersOnlyPropertyType : CustomPokemonPropertyType<FlagProperty> {
    override val keys = KEYS
    override val needsKey = true

    override fun examples() = listOf("true", "false")

    override fun fromString(value: String?): FlagProperty? {
        return if (value == null || value.lowercase() in listOf("true", "yes"))
            FlagProperty(KEYS.first(), false)
        else if (value.lowercase() in listOf("false", "no"))
            FlagProperty(KEYS.first(), true)
        else
            null
    }

    companion object {

        val KEYS = setOf("fof-players-only", "fof-player-only")

        @JvmStatic
        fun matches(pokemon: Pokemon): Boolean {
            return PokemonProperties.parse(KEYS.first()).matches(pokemon)
        }
    }
}
