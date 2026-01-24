package com.github.kuramastone.fightOrFlight.pokeproperties

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.pokemon.Pokemon

class FofDamagePropertyType : CustomPokemonPropertyType<FofDamageProperty> {
    override val keys = setOf("fof-damage")
    override val needsKey = true

    override fun examples() = listOf("4.0", "0.25")

    override fun fromString(value: String?) = value?.toDoubleOrNull()?.let { FofDamageProperty(it) }

    companion object {
        @JvmStatic
        fun getMultiplier(pokemon: Pokemon): Double {
            val prop = pokemon.customProperties
                .filterIsInstance<FofDamageProperty>()
                .firstOrNull()
            return prop?.multiplier ?: 1.0
        }
    }

}

class FofDamageProperty(val multiplier: Double) : CustomPokemonProperty {

    override fun apply(pokemon: Pokemon) {
        if (!pokemon.customProperties.any { it.asString().startsWith("fof-damage=") })
            pokemon.customProperties.add(this)
    }

    override fun asString() = "fof-damage=$multiplier"

    override fun matches(pokemon: Pokemon): Boolean {
        return pokemon.customProperties.firstOrNull { it is FofDamageProperty && this.multiplier == it.multiplier } != null
    }

}