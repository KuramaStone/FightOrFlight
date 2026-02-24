package com.github.kuramastone.fightOrFlight.pokeproperties

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.pokemon.Pokemon
import java.util.*

class AggressionRatePropertyType : CustomPokemonPropertyType<AggressionRateProperty> {
    override val keys = setOf("aggression-rate")
    override val needsKey = true

    override fun examples() = listOf("3.0", "0.33")

    override fun fromString(value: String?) = value?.toDoubleOrNull()?.let { AggressionRateProperty(it) }

}

class AggressionRateProperty(val multiplier: Double) : CustomPokemonProperty {

    override fun apply(pokemon: Pokemon) {
        if (!pokemon.customProperties.any { it.asString().startsWith("aggression-rate=") })
            pokemon.customProperties.add(this)
    }

    override fun asString() = "aggression-rate=$multiplier"

    override fun matches(pokemon: Pokemon): Boolean {
        return pokemon.customProperties.firstOrNull { it is AggressionRateProperty && it.multiplier == this.multiplier } != null
    }

    companion object {

        // Get the Pokemon's aggression modifier if present
        @JvmStatic
        fun getAggressionRateModifier(pokemon: Pokemon): Double {
            val property: AggressionRateProperty =
                pokemon.customProperties.filterIsInstance<AggressionRateProperty>().firstOrNull()
                    ?: return 1.0

            return property.multiplier
        }
    }

}