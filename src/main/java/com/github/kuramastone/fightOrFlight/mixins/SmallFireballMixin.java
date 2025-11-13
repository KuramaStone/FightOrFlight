package com.github.kuramastone.fightOrFlight.mixins;

import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.github.kuramastone.fightOrFlight.attacks.PokeAttack;
import com.github.kuramastone.fightOrFlight.attacks.types.FireAttack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SmallFireball.class)
public class SmallFireballMixin {


    /**
     * Inject the custom interaction on fireball hit.
     */
    @Inject(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/EntityHitResult;getEntity()Lnet/minecraft/world/entity/Entity;", shift = At.Shift.AFTER), cancellable = true)
    private void onHitEntity(EntityHitResult entityHitResult, CallbackInfo ci) {

        Map<SmallFireball, FireAttack.FireballAttackData> dataMap = FireAttack.fireballsLaunched;
        if (dataMap.containsKey(this)) {
            // override result

            FireAttack.FireballAttackData data = dataMap.get(this);

            Entity defender = entityHitResult.getEntity();
            SmallFireball fireball = (SmallFireball) (Object) this;
            if (defender instanceof LivingEntity) {
                PokeAttack.calculateDamage(1.0 / data.fireballsSent, data.isSpecial, ElementalTypes.FIRE, data.pokemonEntity, data.targetEntity);
            }

            dataMap.remove(this);
            ci.cancel();
        }

    }

    /**
     * Inject the custom interaction on fireball hit.
     */
    @Inject(method = "onHitBlock", at = @At(value = "HEAD"), cancellable = true)
    private void onHitBlock(BlockHitResult hitResult, CallbackInfo ci) {
        SmallFireball fireball = (SmallFireball) (Object) this;
        Map<SmallFireball, FireAttack.FireballAttackData> dataMap = FireAttack.fireballsLaunched;
        if (dataMap.containsKey(this)) {

            FireAttack.FireballAttackData data = dataMap.get(this);
            PokeAttack.calculateDamage(1.0 / data.fireballsSent, data.isSpecial, ElementalTypes.FIRE, data.pokemonEntity, data.targetEntity);
            ci.cancel();
        }
    }

}
