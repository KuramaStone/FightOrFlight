package com.github.kuramastone.fightOrFlight.event

import com.cobblemon.mod.common.api.events.Cancelable
import com.cobblemon.mod.common.api.reactive.CancelableObservable
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import java.time.Duration

object FOFEvents {

    @JvmField
    val POKEWAND_DAMAGE_EVENT = CancelableObservable<PokeWandDamageEvent>()

    @JvmField
    val FLIGHT_FLEE_CHANCE_CALCULATION = SimpleObservable<FightFleeChanceCalculation>()

    @JvmField
    val AGGRESSION_RESET = SimpleObservable<AggressionResetEvent>()

    @JvmField
    val WAND_COMMAND_COOLDOWN = SimpleObservable<WandReceiveEvent>()

}

data class PokeWandDamageEvent(
    val attacker: PokemonEntity,
    val defender: PokemonEntity,
    var damage: Int,
    val multiplier: Double,
    val isSpecial: Boolean,
    val moveType: ElementalType
) : Cancelable()

data class FightFleeChanceCalculation(
    val wrappedPokemon: WrappedPokemon,
    val target: LivingEntity,
    var fleeLikelihood: Double
)

data class AggressionResetEvent(
    val wrappedPokemon: WrappedPokemon,
    var aggressionTimer: Int
)

data class WandReceiveEvent(
    val player: ServerPlayer,
    var cooldownLength: Duration
)