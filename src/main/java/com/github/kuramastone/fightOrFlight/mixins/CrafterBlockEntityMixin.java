package com.github.kuramastone.fightOrFlight.mixins;

import com.github.kuramastone.fightOrFlight.FightOrFlightMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Specifically prevents PokeWand item from being placed into a crafter via any method
 */
@Mixin(CrafterBlockEntity.class)
public class CrafterBlockEntityMixin {
    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void onSetItem(int slot, ItemStack stack, CallbackInfo ci) {
        if (FightOrFlightMod.instance.getAPI().getWandManager().isWand(stack)) {
            ci.cancel();
        }
    }
}