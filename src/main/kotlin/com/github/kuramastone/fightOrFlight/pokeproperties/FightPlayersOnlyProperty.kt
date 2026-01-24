package com.github.kuramastone.fightOrFlight.pokeproperties

import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.properties.FlagProperty

class FightPlayersOnlyPropertyType : CustomPokemonPropertyType<FlagProperty> {
    override val keys = KEYS
    override val needsKey = true

    override fun examples() = listOf("true", "false")

    override fun fromString(value: String?): FlagProperty? {
        if(value == null) return null
        if (value in keys)
            return FlagProperty(value, false)
        return null
    }

    companion object {

        val KEYS = setOf("fof-players-only", "fof-player-only")

        @JvmStatic
        fun matches(pokemon: Pokemon): Boolean {
            return KEYS.any { FlagProperty(it, false).matches(pokemon) }
        }
    }
}
