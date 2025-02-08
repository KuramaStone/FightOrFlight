package com.github.kuramastone.fightOrFlight.mixins;

import com.github.kuramastone.fightOrFlight.utils.EntityUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {


    /**
     * Inject the custom interaction on fireball hit.
     */
    @Inject(method = "shouldBeSaved", at = @At(value = "HEAD"), cancellable = true)
    public void shouldBeSaved(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;

        if(EntityUtils.entitiesToNotSave.contains(entity.getUUID())) {
            cir.setReturnValue(false);
        }


    }

}
