package com.github.kuramastone.fightOrFlight.attacks;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.cobblemon.mod.common.api.pokemon.stats.SidemodEvSource;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.tags.CobblemonItemTags;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.fightOrFlight.FOFApi;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.event.FOFEvents;
import com.github.kuramastone.fightOrFlight.event.PokeWandDamageEvent;
import com.github.kuramastone.fightOrFlight.event.PokeWandDeathEvent;
import com.github.kuramastone.fightOrFlight.utils.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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

    public static boolean canAttack(Player owner, LivingEntity target) {
        // always allow attack if it has this aspect
        if (target instanceof PokemonEntity pokemonEntity) {
            if (pokemonEntity.getAspects().contains("forced-targetting")) {
                return true;
            }
        }
        // don't attack if the entity requires persistence
        if (target instanceof Mob mob && mob.isPersistenceRequired()) return false;
        InteractionResult result = AttackEntityCallback.EVENT.invoker()
                .interact(owner, owner.level(), InteractionHand.MAIN_HAND, target, null);
        return result == InteractionResult.PASS;
    }

    public static void calculateDamage(double multiplier, boolean isSpecial, ElementalType moveType, PokemonEntity attacker, LivingEntity defender) {
        resetInvulnerabilityTicks(defender);

        double damage;
        if (defender instanceof PokemonEntity pokeDefender) {
            damage = calculateDamage(multiplier, isSpecial, moveType, attacker.getPokemon(), pokeDefender.getPokemon());

            int actualDamage = (int) Math.ceil(damage);
            PokeWandDamageEvent damageEvent = new PokeWandDamageEvent(attacker, pokeDefender, actualDamage, multiplier, isSpecial, moveType);
            FOFEvents.POKEWAND_DAMAGE_EVENT.emit(damageEvent);
            if (!damageEvent.isCanceled()) {
                actualDamage = damageEvent.getDamage();
                int oldHealth = pokeDefender.getPokemon().getCurrentHealth();
                int newHealth = Math.max(oldHealth - actualDamage, 0);
                pokeDefender.getPokemon().setCurrentHealth(newHealth);
                if (damage != 0) {
                    pokeDefender.hurt(attacker.damageSources().mobAttack(attacker), 0.0f);

                    // deal recoil move to the user
                    Move chosenMove = getHighestDamagingMoveOfType(attacker.getPokemon(), moveType, isSpecial);
                    double recoil = getRecoilOf(chosenMove) * FightOrFlightMod.instance.getAPI().getConfigOptions().recoilPercentage;
                    if (0.0 < recoil) {
                        if (!hasRecoilResistAbility(attacker.getPokemon())) {
                            double recoilDamage = damage * recoil;
                            int attackeroldHealth = attacker.getPokemon().getCurrentHealth();
                            int attackernewHealth = (int) Math.max(attackeroldHealth - recoilDamage, 0);
                            attacker.getPokemon().setCurrentHealth(attackernewHealth);
                        }
                    }
                }
                pokeDefender.setTarget(attacker);
                pokeDefender.setLastHurtByMob(attacker);

                // receive rewards if ko'd
                if (newHealth == 0) {
                    if (oldHealth > 0) {
                        if (!FightOrFlightMod.instance.getAPI().getConfigOptions().disableRewardsOutsideBattle)
                            provideKnockOutRewards(attacker, pokeDefender);
                        FOFEvents.POKEWAND_DEATH_EVENT.emit(new PokeWandDeathEvent(attacker, defender, damageEvent));
                    }
                }


                PlayPosableAnimationPacket pkt = new PlayPosableAnimationPacket(pokeDefender.getId(),
                        Set.of("recoil"),
                        Collections.emptyList());
                pkt.sendToPlayersAround(
                        pokeDefender.getX(), pokeDefender.getY(), pokeDefender.getZ(),
                        32, pokeDefender.level().dimension(), (p) -> false
                );
            }
        }
        else {
            // When you're just a minecraft mob, deal damage as though it were a lv 50 dudunsparse while ignoring type effectiveness
            Move move = getHighestDamagingMoveOfType(attacker.getPokemon(), moveType, isSpecial);
            double highestDamagingMoveOfType = move == null ? 50 : Math.max(move.getPower(), 50);
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

    private static boolean hasRecoilResistAbility(Pokemon pokemon) {
        String ability = pokemon.getAbility().getTemplate().getName();
        return !ability.equalsIgnoreCase("rockhead") && !ability.equalsIgnoreCase("magicguard");
    }

    private static double getRecoilOf(Move move) {
        if (move == null)
            return 0.0;

        String name = move.getTemplate().getName().toLowerCase();
        if (name.equals("bravebird")) {
            return 0.33;
        }
        if (name.equals("doubleedge")) {
            return 0.33;
        }
        if (name.equals("flareblitz")) {
            return 0.33;
        }
        if (name.equals("headcharge")) {
            return 0.25;
        }
        if (name.equals("headsmash")) {
            return 0.5;
        }
        if (name.equals("lightofruin")) {
            return 0.5;
        }
        if (name.equals("selfdestruct")) {
            return 0.8;
        }
        if (name.equals("explosion")) {
            return 0.8;
        }
        if (name.equals("shadowend")) {
            return 0.5;
        }
        if (name.equals("shadowrush")) {
            return 1.0 / 16;
        }
        if (name.equals("submission")) {
            return 0.25;
        }
        if (name.equals("takedown")) {
            return 0.25;
        }
        if (name.equals("volttackle")) {
            return 0.33;
        }
        if (name.equals("wavecrash")) {
            return 0.33;
        }
        if (name.equals("wildcharge")) {
            return 0.25;
        }
        if (name.equals("woodhammer")) {
            return 0.33;
        }

        return 0.0;
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
                        evs.forEach((stat, amount) -> pokemon.getEvs().add(stat, amount, new SidemodEvSource(FightOrFlightMod.MODID, pokemon)));
                    }
                }
            }
            ItemStack heldItem = attacker.getPokemon().getHeldItem$common();
            SidemodEvSource sideMod = new SidemodEvSource(FightOrFlightMod.MODID, attacker.getPokemon());
            evs.forEach((stat, amount) -> attacker.getPokemon().getEvs().add(stat, amount, sideMod));

            if (heldItem.getItem() == CobblemonItems.POWER_WEIGHT) {
                attacker.getPokemon().getEvs().add(Stats.HP, 8, sideMod);
            }
            else if (heldItem.getItem() == CobblemonItems.POWER_BRACER) {
                attacker.getPokemon().getEvs().add(Stats.ATTACK, 8, sideMod);
            }
            else if (heldItem.getItem() == CobblemonItems.POWER_BELT) {
                attacker.getPokemon().getEvs().add(Stats.DEFENCE, 8, sideMod);
            }
            else if (heldItem.getItem() == CobblemonItems.POWER_LENS) {
                attacker.getPokemon().getEvs().add(Stats.SPECIAL_ATTACK, 8, sideMod);
            }
            else if (heldItem.getItem() == CobblemonItems.POWER_BAND) {
                attacker.getPokemon().getEvs().add(Stats.SPECIAL_DEFENCE, 8, sideMod);
            }
            else if (heldItem.getItem() == CobblemonItems.POWER_ANKLET) {
                attacker.getPokemon().getEvs().add(Stats.SPEED, 8, sideMod);
            }


        }
        else {
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
                        double expMultiplier = Cobblemon.config.getExperienceShareMultiplier() * FightOrFlightMod.instance.getAPI().getConfigOptions().worldExpMultiplier;
                        int experience = PokeUtils.calculateExperience(attacker.getPokemon(), pokeDefender.getPokemon(), expMultiplier);
                        pokemon.addExperienceWithPlayer(serverPlayer, new SidemodExperienceSource(FightOrFlightMod.MODID), experience);
                    }
                }
            }

            double expMultiplier = FightOrFlightMod.instance.getAPI().getConfigOptions().worldExpMultiplier;
            if (attacker.getPokemon().getHeldItem$common().is(CobblemonItemTags.EXPERIENCE_SHARE))
                expMultiplier = Cobblemon.config.getExperienceShareMultiplier();
            int experience = PokeUtils.calculateExperience(attacker.getPokemon(), pokeDefender.getPokemon(), expMultiplier);
            attacker.getPokemon().addExperienceWithPlayer(serverPlayer, new SidemodExperienceSource(FightOrFlightMod.MODID), experience);
        }
        else {
            int experience = PokeUtils.calculateExperience(attacker.getPokemon(), pokeDefender.getPokemon(), FightOrFlightMod.instance.getAPI().getConfigOptions().worldExpMultiplier);
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

    public static double calculateDamage(double multiplier, boolean isSpecial, ElementalType moveType, Pokemon attacker, Pokemon defender) {
        Move bestMove = getHighestDamagingMoveOfType(attacker, moveType, isSpecial);
        double moveFOFPower = bestMove == null ? 50 : getMoveFoFPower(bestMove);
        double movePower = Math.max(moveFOFPower, 50);

        return multiplier * PokeUtils.calculatePokeAttackDamage(attacker, defender, moveType, movePower, isSpecial, true);
    }

    public static @Nullable Move getHighestDamagingMoveOfType(Pokemon attacker, ElementalType type, boolean isSpecial) {
        Move bestMoveSoFar = null;
        for (Move move : attacker.getMoveSet().getMoves()) {
            boolean isMoveSpecial = move.getDamageCategory() == DamageCategories.INSTANCE.getSPECIAL();
            if (move.getType() == type && isMoveSpecial == isSpecial)
                if (bestMoveSoFar == null || getMoveFoFPower(bestMoveSoFar) < getMoveFoFPower(move)) {
                    bestMoveSoFar = move;
                }
        }
        return bestMoveSoFar;
    }

    public static double getMoveFoFPower(Move move) {
        if (move == null)
            return 50;
        double power = Math.max(50, move.getPower());
        double accuracy = Math.max(50, move.getAccuracy());
        double accuracyPower = Mth.clamp(accuracy / 100.0, 0.0, 1.0);
        if (accuracyPower == 0.0) {
            accuracyPower = 1.0;
        }
        return power * accuracyPower;
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
