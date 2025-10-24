package com.github.kuramastone.fightOrFlight.pokeproperties

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.pokemon.Pokemon

class AggressionBiasPropertyType : CustomPokemonPropertyType<AggressionBiasProperty> {
    override val keys = setOf("aggression")
    override val needsKey = true

    override fun examples() = listOf("5.0", "-5.0")

    override fun fromString(value: String?) = AggressionBiasProperty(value?.toDoubleOrNull() ?: 0.0)
}

class AggressionBiasProperty(val bias: Double) : CustomPokemonProperty {

    override fun apply(pokemon: Pokemon) {
        pokemon.customProperties.add(this)
    }

    override fun asString() = "aggression-bias"

    override fun matches(pokemon: Pokemon): Boolean {
        return pokemon.customProperties.firstOrNull { it is AggressionBiasProperty && it.bias == this.bias } != null
    }

}