package com.github.kuramastone.fightOrFlight.mixins;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import com.github.kuramastone.fightOrFlight.attacks.types.GhostAttack;
import com.github.kuramastone.fightOrFlight.entity.WrappedPokemon;
import com.github.kuramastone.fightOrFlight.utils.ReflectionUtils;
import com.github.kuramastone.fightOrFlight.utils.TickScheduler;
import com.github.kuramastone.fightOrFlight.utils.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EmptyPokeBallEntity.class)
public class EmptyPokeBallEntityMixin {


    /**
     * Inject the custom interaction on capture fail. makes pokemon target pokeball thrower
     */
    @Inject(method = "breakFree", at = @At(value = "HEAD"), remap = false)
    public void breakFree(CallbackInfo ci) {
        EmptyPokeBallEntity pokeball = (EmptyPokeBallEntity) (Object) this;

        try {
            if (pokeball.getCapturingPokemon() != null) {
                if (pokeball.getOwner() instanceof LivingEntity livingThrower) {
                    TickScheduler.scheduleLater(4, () -> {
                        WrappedPokemon wrapped = FightOrFlightMod.instance.getAPI().getWrappedPokemon(pokeball.getCapturingPokemon());
                        wrapped.setTarget(livingThrower);
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
