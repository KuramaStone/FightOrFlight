package com.github.kuramastone.fightOrFlight.mixins;

import com.github.kuramastone.fightOrFlight.attacks.types.GhostAttack;
import com.github.kuramastone.fightOrFlight.utils.EntityUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EvokerFangs.class)
public class EvokerFangsMixin {


    /**
     * Inject the custom interaction on fireball hit.
     */
    @Inject(method = "dealDamageTo", at = @At(value = "HEAD"), cancellable = true)
    public void dealDamageTo(LivingEntity livingEntity, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if(GhostAttack.EVOKER_FANGS.contains(entity.getUUID())) {
            // dont do damage if from mod
            ci.cancel();
        }


    }

}
