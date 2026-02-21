package com.github.kuramastone.fightOrFlight.pokeproperties

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.pokemon.Pokemon

class FofResistancePropertyType : CustomPokemonPropertyType<FofResistanceProperty> {
    override val keys = setOf("fof-resistance")
    override val needsKey = true

    override fun examples() = listOf("0.5", "0.1")

    override fun fromString(value: String?) = value?.toDoubleOrNull()?.let { FofResistanceProperty(it) }

    companion object {
        @JvmStatic
        fun getMultiplier(pokemon: Pokemon): Double {
            val prop = pokemon.customProperties
                .filterIsInstance<FofResistanceProperty>()
                .firstOrNull()
            return prop?.multiplier ?: 1.0
        }
    }

}

class FofResistanceProperty(val multiplier: Double) : CustomPokemonProperty {

    override fun apply(pokemon: Pokemon) {
        if (!pokemon.customProperties.any { it.asString().startsWith("fof-resistance=") })
            pokemon.customProperties.add(this)
    }

    override fun asString() = "fof-resistance=$multiplier"

    override fun matches(pokemon: Pokemon): Boolean {
        return pokemon.customProperties.firstOrNull { it is FofDamageProperty && this.multiplier == it.multiplier } != null
    }

}