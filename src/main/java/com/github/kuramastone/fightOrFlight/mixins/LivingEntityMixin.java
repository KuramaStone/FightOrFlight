package com.github.kuramastone.fightOrFlight.mixins;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.utils.PokeUtils;
import com.github.kuramastone.fightOrFlight.utils.ReflectionUtils;
import com.github.kuramastone.fightOrFlight.utils.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Shadow @Final protected static int LIVING_ENTITY_FLAG_IS_USING;

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void actuallyHurt(DamageSource damageSource, float f, CallbackInfo cir) {
        if(FightOrFlightMod.instance.getAPI() == null)
            return;
        LivingEntity defender = (LivingEntity) (Object) this;

        if (defender instanceof PokemonEntity pokeDefender) {
            if(damageSource.getEntity() instanceof LivingEntity livingAttacker) {
                if(livingAttacker instanceof Player || livingAttacker instanceof PokemonEntity) {
                    // let players and pokemon do normal damage.
                    return;
                }

                if (!defender.isInvulnerableTo(damageSource)) {
                    try {

                        // replace damage with the equivalent damage of a 50bp move
                        Pokemon pokeAttacker = FightOrFlightMod.instance.getAPI().getConfigOptions().getPokemonEquivalent(livingAttacker.getType());
                        float pokeDamage = (float) PokeUtils.calculatePokeAttackDamage(pokeAttacker, pokeDefender.getPokemon(), ElementalTypes.INSTANCE.getNORMAL(), 50, false, true);
                        float entityDamage = pokeDamage / pokeDefender.getPokemon().getMaxHealth() * pokeDefender.getMaxHealth();
                        f = entityDamage;

                        f = ReflectionUtils.livingEntityMethod_getDamageAfterArmorAbsorb(defender, damageSource, f);
                        f = ReflectionUtils.livingEntityMethod_getDamageAfterMagicAbsorb(defender, damageSource, f);
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    float var9 = Math.max(f - defender.getAbsorptionAmount(), 0.0F);
                    defender.setAbsorptionAmount(defender.getAbsorptionAmount() - (f - var9));
                    float h = f - var9;
                    if (h > 0.0F && h < 3.4028235E37F && damageSource.getEntity() instanceof ServerPlayer serverPlayer) {
                        serverPlayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(h * 10.0F));
                    }

                    if (var9 != 0.0F) {
                        defender.getCombatTracker().recordDamage(damageSource, var9);
                        defender.setHealth(defender.getHealth() - var9);
                        defender.setAbsorptionAmount(defender.getAbsorptionAmount() - var9);
                        defender.gameEvent(GameEvent.ENTITY_DAMAGE);
                    }
                }

                cir.cancel();
            }

        }

    }

}
