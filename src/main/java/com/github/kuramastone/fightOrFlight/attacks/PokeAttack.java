package com.github.kuramastone.fightOrFlight.attacks;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.tags.CobblemonItemTags;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.utils.*;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class PokeAttack {

    protected final FOFApi api;
    private final ElementalType elementalType;
    private final boolean isRanged;
    protected SplittableRandom random = new SplittableRandom();

    public PokeAttack(FOFApi api, ElementalType elementalType, boolean isRanged) {
        this.api = api;
        this.elementalType = elementalType;
        this.isRanged = isRanged;
    }

    public static void calculateDamage(double multiplier, boolean isSpecial, ElementalType moveType, PokemonEntity attacker, LivingEntity defender) {
        resetInvulnerabilityTicks(defender);

        double damage;
        if (defender instanceof PokemonEntity pokeDefender) {
            damage = calculateDamage(multiplier, isSpecial, moveType, attacker, pokeDefender);

            int oldHealth = pokeDefender.getPokemon().getCurrentHealth();
            int newHealth = oldHealth - (int) Math.ceil(damage);
            newHealth = Math.max(0, newHealth);
            pokeDefender.getPokemon().setCurrentHealth(newHealth);
            if (damage != 0) {
                pokeDefender.hurt(attacker.damageSources().mobAttack(attacker), 0.0f);
            }
            pokeDefender.setTarget(attacker);
            pokeDefender.setLastHurtByMob(attacker);

            // receive rewards if ko'd
            if (newHealth == 0) {
                if (oldHealth > 0)
                    if (!FightOrFlightMod.instance.getAPI().getConfigOptions().disableRewardsOutsideBattle)
                        provideKnockOutRewards(attacker, pokeDefender);
            }


            PlayPosableAnimationPacket pkt = new PlayPosableAnimationPacket(pokeDefender.getId(),
                    Set.of("recoil"),
                    Collections.emptyList());
            pkt.sendToPlayersAround(
                    pokeDefender.getX(), pokeDefender.getY(), pokeDefender.getZ(),
                    32, pokeDefender.level().dimension(), (p) -> false
            );
        } else {
            // When you're just a minecraft mob, deal damage as though it were a lv 50 dudunsparse while ignoring type effectiveness
            double highestDamagingMoveOfType = getHighestDamagingMovePowerOfType(attacker, moveType, 50);
            Pokemon equivalent = FightOrFlightMod.instance.getAPI().getConfigOptions().getPokemonEquivalent(defender.getType());
            damage = multiplier * PokeUtils.calculatePokeAttackDamage(attacker.getPokemon(),
                    equivalent,
                    moveType, highestDamagingMoveOfType, isSpecial, false);

            // get damage to pokemon equivalent as a percent
            float pokeHealthFraction = (float) damage / equivalent.getMaxHealth();
            // apply that percent the the mob's entity hp
            float mobHealthFraction = pokeHealthFraction * defender.getMaxHealth();

            defender.hurt(attacker.damageSources().mobAttack(attacker), mobHealthFraction);
        }
    }

    private static void provideKnockOutRewards(PokemonEntity attacker, PokemonEntity pokeDefender) {

        // provide evs
        provideEVRewards(attacker, pokeDefender);

        // provide exp
        provideExpRewards(attacker, pokeDefender);
    }

    private static void provideEVRewards(PokemonEntity attacker, PokemonEntity pokeDefender) {

        if (attacker.getOwner() instanceof ServerPlayer serverPlayer) {
            Map<Stat, Integer> evs = pokeDefender.getPokemon().getForm().getEvYield();

            // provide exp rewards to any party members with exp share
            for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(serverPlayer)) {
                if (!pokemon.getUuid().equals(attacker.getPokemon().getUuid())) {
                    if (attacker.getPokemon().getHeldItem$common().is(CobblemonItemTags.EXPERIENCE_SHARE)) {
                        evs.forEach((stat, amount) -> pokemon.getEvs().add(stat, amount));
                    }
                }
            }
            evs.forEach((stat, amount) -> attacker.getPokemon().getEvs().add(stat, amount));


        } else {
            // wild pokemon dont get evs
        }
    }

    private static void provideExpRewards(PokemonEntity attacker, PokemonEntity pokeDefender) {
        if (attacker.getOwner() instanceof ServerPlayer serverPlayer) {
            // provide exp for main pokemon

            // provide exp rewards to any party members with exp share. exclude attacker
            for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(serverPlayer)) {
                if (!pokemon.getUuid().equals(attacker.getPokemon().getUuid())) {
                    if (pokemon.getHeldItem$common().is(CobblemonItemTags.EXPERIENCE_SHARE)) {
                        double expMultiplier = Cobblemon.config.getExperienceShareMultiplier();
                        int experience = PokeUtils.calculateExperience(attacker.getPokemon(), pokeDefender.getPokemon(), expMultiplier);
                        pokemon.addExperienceWithPlayer(serverPlayer, new SidemodExperienceSource(FightOrFlightMod.MODID), experience);
                    }
                }
            }

            double expMultiplier = 1.0;
            if (attacker.getPokemon().getHeldItem$common().is(CobblemonItemTags.EXPERIENCE_SHARE))
                expMultiplier = Cobblemon.config.getExperienceShareMultiplier();
            int experience = PokeUtils.calculateExperience(attacker.getPokemon(), pokeDefender.getPokemon(), expMultiplier);
            attacker.getPokemon().addExperienceWithPlayer(serverPlayer, new SidemodExperienceSource(FightOrFlightMod.MODID), experience);
        } else {
            int experience = PokeUtils.calculateExperience(attacker.getPokemon(), pokeDefender.getPokemon(), 1.0);
            attacker.getPokemon().addExperience(new SidemodExperienceSource(FightOrFlightMod.MODID), experience);
        }
    }


    private static void resetInvulnerabilityTicks(LivingEntity defender) {
        try {
            ReflectionUtils.setEntityInvulnerableTime(defender, 0);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static double calculateDamage(double multiplier, boolean isSpecial, ElementalType moveType, PokemonEntity attacker, PokemonEntity defender) {
        double highestDamagingMoveOfType = getHighestDamagingMovePowerOfType(attacker, moveType, 50);

        return multiplier * PokeUtils.calculatePokeAttackDamage(attacker.getPokemon(), defender.getPokemon(), moveType, highestDamagingMoveOfType, isSpecial, true);
    }

    private static double getHighestDamagingMovePowerOfType(PokemonEntity attacker, ElementalType type, double min) {
        double highestDamagingMoveOfType = min;
        for (Move move : attacker.getPokemon().getMoveSet().getMoves())
            if (move.getType() == type)
                if (highestDamagingMoveOfType < move.getPower())
                    highestDamagingMoveOfType = move.getPower();
        return highestDamagingMoveOfType;
    }

    public static Move getHighestDamagingMoveOfType(PokemonEntity attacker, ElementalType type) {
        double highestDamagingMoveOfType = 0;
        Move bigMove = null;
        for (Move move : attacker.getPokemon().getMoveSet().getMoves())
            if (move.getType() == type)
                if (highestDamagingMoveOfType < move.getPower()) {
                    highestDamagingMoveOfType = move.getPower();
                    bigMove = move;
                }
        return bigMove;
    }

    public abstract CompletableFuture<Boolean> perform(PokemonEntity entity, LivingEntity target);

    protected Move getRandomMove(MoveSet moveSet) {
        return moveSet.get(random.nextInt(moveSet.getMoves().size()));
    }

    public ElementalType getElementalType() {
        return elementalType;
    }

    public boolean isRanged() {
        return isRanged;
    }

}
